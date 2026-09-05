package com.pblurr.app.data.inference

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.detector.PrivateRegionDetector
import com.pblurr.app.domain.model.BoundingBox
import com.pblurr.app.domain.model.DetectionMask
import com.pblurr.app.domain.model.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

/**
 * High-Precision On-Device Intimate Region Detector powered by ONNX Runtime.
 * Performs local tensor inference using official NudeNet ONNX neural network weights.
 */
class OnnxPrivateRegionDetector(private val context: Context) : PrivateRegionDetector {

    private val modelFileName: String = "nudenet.onnx"
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private val preprocessingEngine = PreprocessingEngine()
    private val anatomicalSegmenter = AnatomicalSegmenter()

    // Exposed intimate classes to detect: Breasts, Pelvic/Genitalia, Buttocks, Anus
    private val intimateClassIndices = setOf(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17)

    private val classLabels = mapOf(
        0 to "female_breast_exposed",
        1 to "female_face",
        2 to "female_genitalia_exposed",
        3 to "male_breast_exposed",
        4 to "male_genitalia_exposed",
        5 to "buttocks_exposed",
        6 to "anus_exposed",
        7 to "female_breast_covered",
        8 to "female_genitalia_covered",
        9 to "buttocks_covered",
        10 to "male_breast_covered",
        11 to "male_genitalia_covered",
        12 to "male_face",
        13 to "armpit_exposed",
        14 to "belly_exposed",
        15 to "feet_exposed",
        16 to "feet_covered",
        17 to "belly_covered"
    )

    init {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open(modelFileName).readBytes()
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
            }
            ortSession = ortEnv?.createSession(modelBytes, options)
            AppLogger.i(LogCategory.MODEL, "Official NudeNet ONNX model '$modelFileName' loaded successfully into ONNX Runtime!")
        } catch (e: Exception) {
            AppLogger.e(LogCategory.MODEL, "Failed to load ONNX model '$modelFileName' from assets: ${e.message}")
        }
    }

    companion object {
        private const val HIGH_SENSITIVITY_THRESHOLD = 0.20f
    }

    override suspend fun detect(bitmap: Bitmap): List<DetectionMask> =
        withContext(Dispatchers.Default) {
            val session = ortSession
            val env = ortEnv

            if (session == null || env == null) {
                AppLogger.w(LogCategory.MODEL, "ONNX Session unavailable. Using anatomical segmenter fallback.")
                return@withContext anatomicalSegmenter.segment(bitmap, HIGH_SENSITIVITY_THRESHOLD)
            }

            try {
                val startTime = System.currentTimeMillis()

                // Fixed ONNX input tensor resolution required by NudeNet ONNX graph
                val tensorWidth = 320
                val tensorHeight = 320

                // Direct 1-step letterboxing from original photo to tensor resolution (320x320)
                // This ensures 100% accurate matrix mapping back to original photo space.
                val letterbox = preprocessingEngine.letterbox(bitmap, tensorWidth, tensorHeight)
                val letterboxedBmp = letterbox.letterboxedBitmap

                // 2. Convert Bitmap to CHW FloatBuffer [1, 3, 320, 320]
                val floatBuffer = FloatBuffer.allocate(1 * 3 * tensorWidth * tensorHeight)
                val pixels = IntArray(tensorWidth * tensorHeight)
                letterboxedBmp.getPixels(pixels, 0, tensorWidth, 0, 0, tensorWidth, tensorHeight)

                val channelStride = tensorWidth * tensorHeight
                for (i in 0 until channelStride) {
                    val color = pixels[i]
                    val r = ((color shr 16) and 0xFF) / 255f
                    val g = ((color shr 8) and 0xFF) / 255f
                    val b = (color and 0xFF) / 255f

                    floatBuffer.put(i, r)
                    floatBuffer.put(channelStride + i, g)
                    floatBuffer.put(2 * channelStride + i, b)
                }
                floatBuffer.rewind()

                val inputShape = longArrayOf(1, 3, tensorWidth.toLong(), tensorHeight.toLong())
                val inputTensor = OnnxTensor.createTensor(env, floatBuffer, inputShape)

                // 3. Run ONNX Session inference
                val inputName = session.inputNames.iterator().next()
                val results = session.run(mapOf(inputName to inputTensor))
                inputTensor.close()

                @Suppress("UNCHECKED_CAST")
                val outputTensor = results.get(0).value as Array<Array<FloatArray>> // Shape: [1, 22, 2100]
                val candidateBoxes = mutableListOf<BoundingBox>()

                val numBoxes = outputTensor[0][0].size // 2100

                for (i in 0 until numBoxes) {
                    val cx = outputTensor[0][0][i]
                    val cy = outputTensor[0][1][i]
                    val w = outputTensor[0][2][i]
                    val h = outputTensor[0][3][i]

                    // Find highest scoring class among 18 classes (channels 4..21)
                    var maxScore = 0f
                    var maxClass = -1

                    for (c in 0 until 18) {
                        val score = outputTensor[0][4 + c][i]
                        if (score > maxScore) {
                            maxScore = score
                            maxClass = c
                        }
                    }

                    // Strictly filter for INTIMATE EXPOSED private classes at high sensitivity
                    if (maxScore >= HIGH_SENSITIVITY_THRESHOLD && maxClass in intimateClassIndices) {
                        val left = cx - w / 2f
                        val top = cy - h / 2f
                        val right = cx + w / 2f
                        val bottom = cy + h / 2f

                        candidateBoxes.add(
                            BoundingBox(
                                left = left,
                                top = top,
                                right = right,
                                bottom = bottom,
                                score = maxScore,
                                classIndex = maxClass,
                                label = classLabels[maxClass] ?: "private_region"
                            )
                        )
                    }
                }

                results.close()

                // 4. Apply Class-Aware Non-Maximum Suppression (NMS)
                val nmsBoxes = applyClassAwareNMS(candidateBoxes, iouThreshold = 0.30f)

                // 5. Map coordinates from 320x320 tensor directly back to full-resolution photo space
                val mappedMasks = nmsBoxes.map { box ->
                    val mappedBox = preprocessingEngine.mapBoxToOriginal(box, letterbox)
                    DetectionMask(
                        maskBitmap = null,
                        confidence = mappedBox.score,
                        label = mappedBox.label,
                        boundingBox = mappedBox,
                        transformMatrix = letterbox.tensorToOriginalMatrix
                    )
                }

                val duration = System.currentTimeMillis() - startTime
                AppLogger.i(
                    LogCategory.DETECTION,
                    "Official NudeNet ONNX inference completed in ${duration}ms: ${candidateBoxes.size} raw -> ${mappedMasks.size} intimate regions post-NMS."
                )

                return@withContext mappedMasks
            } catch (e: Throwable) {
                AppLogger.e(LogCategory.MODEL, "ONNX inference exception: ${e.message}. Using fallback segmenter.", e.stackTraceToString())
                return@withContext anatomicalSegmenter.segment(bitmap, HIGH_SENSITIVITY_THRESHOLD)
            }
        }

    /**
     * Class-aware NMS: only suppresses detections of the same class index against each other.
     */
    private fun applyClassAwareNMS(boxes: List<BoundingBox>, iouThreshold: Float): List<BoundingBox> {
        return boxes
            .groupBy { it.classIndex }
            .flatMap { (_, classBoxes) ->
                val sorted = classBoxes.sortedByDescending { it.score }.toMutableList()
                val selected = mutableListOf<BoundingBox>()
                while (sorted.isNotEmpty()) {
                    val current = sorted.removeAt(0)
                    selected.add(current)
                    sorted.removeAll { box -> calculateIoU(current, box) > iouThreshold }
                }
                selected
            }
    }

    private fun calculateIoU(a: BoundingBox, b: BoundingBox): Float {
        val interLeft = Math.max(a.left, b.left)
        val interTop = Math.max(a.top, b.top)
        val interRight = Math.min(a.right, b.right)
        val interBottom = Math.min(a.bottom, b.bottom)

        if (interLeft >= interRight || interTop >= interBottom) return 0f

        val interArea = (interRight - interLeft) * (interBottom - interTop)
        val unionArea = a.area + b.area - interArea

        return if (unionArea > 0f) interArea / unionArea else 0f
    }

    override fun release() {
        try {
            ortSession?.close()
            ortEnv?.close()
            ortSession = null
            ortEnv = null
        } catch (e: Exception) {
            AppLogger.e(LogCategory.MODEL, "Error releasing ONNX resources: ${e.message}")
        }
    }
}
