package com.pblurr.app.presentation.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pblurr.app.data.censor.CensorRenderer
import com.pblurr.app.data.inference.MaskRefiner
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.CensorEffect
import com.pblurr.app.domain.model.CensorOptions
import com.pblurr.app.domain.model.DetectionMask
import com.pblurr.app.domain.model.DetectionPipelineResult
import com.pblurr.app.domain.model.LogCategory
import com.pblurr.app.domain.usecase.ApplyCensorUseCase
import com.pblurr.app.domain.usecase.DetectPrivateRegionsUseCase
import com.pblurr.app.domain.usecase.ExportImageUseCase
import com.pblurr.app.domain.usecase.RefineMaskUseCase
import com.pblurr.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ProcessingStage(val title: String, val progress: Float) {
    object LoadingImage : ProcessingStage("Loading Image & Meta...", 0.15f)
    object InitializingModel : ProcessingStage("Preparing AI Detector...", 0.35f)
    object DetectingRegions : ProcessingStage("Detecting Private Regions...", 0.60f)
    object RefiningMask : ProcessingStage("Generating Pixel Mask...", 0.80f)
    object ApplyingCensor : ProcessingStage("Applying Censor Effect...", 0.95f)
    object Complete : ProcessingStage("Complete!", 1.0f)
}

sealed class UIState {
    object Idle : UIState()
    data class Processing(val stage: ProcessingStage) : UIState()
    data class Success(val result: DetectionPipelineResult) : UIState()
    data class Error(val message: String) : UIState()
}

enum class VersionStatus {
    NOT_CHECKED,
    LATEST,
    OUTDATED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val currentInstalledVersion: String = "1.0.1"

    private val detector = com.pblurr.app.data.inference.OnnxPrivateRegionDetector(context)
    private val detectPrivateRegionsUseCase = DetectPrivateRegionsUseCase(detector)
    private val refineMaskUseCase = RefineMaskUseCase(MaskRefiner())
    private val applyCensorUseCase = ApplyCensorUseCase(CensorRenderer())
    private val exportImageUseCase = ExportImageUseCase(context)
    private val settingsRepository = SettingsRepository(context)

    private val _uiState = MutableStateFlow<UIState>(UIState.Idle)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    private val _censorOptions = MutableStateFlow(settingsRepository.load())
    val censorOptions: StateFlow<CensorOptions> = _censorOptions.asStateFlow()

    private val updateChecker = com.pblurr.app.data.update.UpdateChecker()

    private val _updateInfo = MutableStateFlow<com.pblurr.app.data.update.AppUpdateInfo?>(null)
    val updateInfo: StateFlow<com.pblurr.app.data.update.AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _versionStatus = MutableStateFlow(VersionStatus.NOT_CHECKED)
    val versionStatus: StateFlow<VersionStatus> = _versionStatus.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    private val _manualUpdateResult = MutableStateFlow<String?>(null)
    val manualUpdateResult: StateFlow<String?> = _manualUpdateResult.asStateFlow()

    var currentSourceUri: Uri? = null
        private set

    init {
        // Apply persisted logging setting on startup
        AppLogger.isEnabled = _censorOptions.value.loggingEnabled

        if (_censorOptions.value.autoUpdateCheckEnabled) {
            viewModelScope.launch {
                val update = updateChecker.checkForUpdates()
                if (update != null) {
                    _updateInfo.value = update
                    _versionStatus.value = VersionStatus.OUTDATED
                } else {
                    _versionStatus.value = VersionStatus.LATEST
                }
            }
        }
    }

    fun checkUpdatesManually() {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            _manualUpdateResult.value = null
            AppLogger.i(LogCategory.APP, "Manual update check initiated by user")
            val update = updateChecker.checkForUpdates()
            _isCheckingUpdates.value = false
            if (update != null) {
                _updateInfo.value = update
                _versionStatus.value = VersionStatus.OUTDATED
                _manualUpdateResult.value = "Update v${update.latestVersion} is available on GitHub!"
            } else {
                _versionStatus.value = VersionStatus.LATEST
                _manualUpdateResult.value = "You are running the latest version of P.blurr."
            }
        }
    }

    fun completeOnboarding(autoUpdateEnabled: Boolean) {
        val current = _censorOptions.value
        val updated = current.copy(
            hasCompletedOnboarding = true,
            autoUpdateCheckEnabled = autoUpdateEnabled,
            hasAcceptedInternetNotice = current.hasAcceptedInternetNotice || autoUpdateEnabled
        )
        updateCensorOptions(updated)
        AppLogger.i(LogCategory.APP, "Onboarding setup completed. Auto-update: $autoUpdateEnabled")
    }

    fun clearManualUpdateResult() {
        _manualUpdateResult.value = null
    }

    fun restartApp() {
        AppLogger.i(LogCategory.APP, "App restart requested due to settings change.")
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

    // Active job for censor rendering — cancelled before each new render to prevent OOM from concurrent ops
    private var activeCensorJob: Job? = null

    // Layer display toggles for editor debug overlay (Default: OFF)
    private val _showMaskOverlay = MutableStateFlow(false)
    val showMaskOverlay: StateFlow<Boolean> = _showMaskOverlay.asStateFlow()

    private val _showRawBoxes = MutableStateFlow(false)
    val showRawBoxes: StateFlow<Boolean> = _showRawBoxes.asStateFlow()

    // Current working bitmap artifacts
    var currentOriginalBitmap: Bitmap? = null
        private set
    var currentRawDetections: List<DetectionMask> = emptyList()
        private set
    var currentMaskBitmap: Bitmap? = null
        private set
    var currentCensoredBitmap: Bitmap? = null
        private set

    // Undo / Redo stacks for Manual Mask Editing
    private val maskUndoStack = mutableListOf<Bitmap>()
    private val maskRedoStack = mutableListOf<Bitmap>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _exportResult = MutableStateFlow<Result<Uri>?>(null)
    val exportResult: StateFlow<Result<Uri>?> = _exportResult.asStateFlow()

    fun processImageUri(uri: Uri) {
        currentSourceUri = uri
        viewModelScope.launch {
            _uiState.value = UIState.Processing(ProcessingStage.LoadingImage)
            AppLogger.i(LogCategory.APP, "Selected image URI: $uri")

            val original = loadBitmapFromUri(uri)
            if (original == null) {
                _uiState.value = UIState.Error("Failed to decode image from gallery.")
                return@launch
            }

            currentOriginalBitmap = original
            val origW = original.width
            val origH = original.height
            AppLogger.i(LogCategory.IMAGE, "Loaded image bitmap: ${origW}x${origH}")

            runDetectionPipeline(original)
        }
    }

    private suspend fun runDetectionPipeline(original: Bitmap) = withContext(Dispatchers.Default) {
        val options = _censorOptions.value

        // Stage 1: Detector prep
        _uiState.value = UIState.Processing(ProcessingStage.InitializingModel)
        val detectStart = System.currentTimeMillis()

        // Stage 2: Detection
        _uiState.value = UIState.Processing(ProcessingStage.DetectingRegions)
        val detections = detectPrivateRegionsUseCase(original)
        currentRawDetections = detections
        val detectTime = System.currentTimeMillis() - detectStart

        // Stage 3: Refine mask
        _uiState.value = UIState.Processing(ProcessingStage.RefiningMask)
        val refineStart = System.currentTimeMillis()
        val refinedMask = refineMaskUseCase(
            targetWidth = original.width,
            targetHeight = original.height,
            detections = detections,
            dilationPx = options.maskDilationPx,
            featherEdges = options.featherEdges
        )
        currentMaskBitmap = refinedMask
        val refineTime = System.currentTimeMillis() - refineStart

        // Clear undo/redo history on fresh AI detection run
        maskUndoStack.clear()
        maskRedoStack.clear()
        updateUndoRedoStates()

        // Stage 4: Apply Censor Effect
        _uiState.value = UIState.Processing(ProcessingStage.ApplyingCensor)
        val renderStart = System.currentTimeMillis()
        val censored = applyCensorUseCase(original, refinedMask, options.effect)
        currentCensoredBitmap = censored
        val renderTime = System.currentTimeMillis() - renderStart

        // Calculate mask pixel coverage
        val coverage = calculateMaskCoverage(refinedMask)

        val pipelineResult = DetectionPipelineResult(
            originalBitmap = original,
            rawDetections = detections,
            refinedMask = refinedMask,
            censoredBitmap = censored,
            inferenceTimeMs = detectTime,
            refinementTimeMs = refineTime,
            renderingTimeMs = renderTime,
            maskCoveragePercentage = coverage,
            originalWidth = original.width,
            originalHeight = original.height
        )

        _uiState.value = UIState.Success(pipelineResult)
        AppLogger.i(LogCategory.APP, "Pipeline completed successfully. Coverage: ${"%.2f".format(coverage)}%")
    }

    fun updateCensorOptions(newOptions: CensorOptions) {
        val oldOptions = _censorOptions.value
        _censorOptions.value = newOptions
        AppLogger.isEnabled = newOptions.loggingEnabled
        settingsRepository.save(newOptions)  // persist immediately
        val original = currentOriginalBitmap ?: return

        // Cancel any in-flight render before starting a new one to prevent OOM
        activeCensorJob?.cancel()
        activeCensorJob = viewModelScope.launch(Dispatchers.Default) {
            val detectionParamsChanged = oldOptions.maskDilationPx != newOptions.maskDilationPx

            if (detectionParamsChanged) {
                // Re-run mask refinement at the new dilation
                val detections = detectPrivateRegionsUseCase(original)
                currentRawDetections = detections
                val refinedMask = refineMaskUseCase(
                    targetWidth = original.width,
                    targetHeight = original.height,
                    detections = detections,
                    dilationPx = newOptions.maskDilationPx,
                    featherEdges = newOptions.featherEdges
                )
                currentMaskBitmap = refinedMask
                val newCensored = applyCensorUseCase(original, refinedMask, newOptions.effect)
                if (!isActive) return@launch
                currentCensoredBitmap = newCensored

                val currentSuccess = _uiState.value as? UIState.Success
                if (currentSuccess != null) {
                    _uiState.value = currentSuccess.copy(
                        result = currentSuccess.result.copy(
                            rawDetections = detections,
                            refinedMask = refinedMask,
                            censoredBitmap = newCensored
                        )
                    )
                }
            } else {
                // Only effect/style changed: re-composite existing mask
                val mask = currentMaskBitmap ?: return@launch
                val newCensored = applyCensorUseCase(original, mask, newOptions.effect)
                if (!isActive) return@launch  // job was cancelled while rendering
                currentCensoredBitmap = newCensored

                val currentSuccess = _uiState.value as? UIState.Success
                if (currentSuccess != null) {
                    _uiState.value = currentSuccess.copy(
                        result = currentSuccess.result.copy(censoredBitmap = newCensored)
                    )
                }
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun resetSettings() {
        settingsRepository.resetToDefaults()
        updateCensorOptions(SettingsRepository.DEFAULTS)
    }

    fun toggleMaskOverlay() {
        _showMaskOverlay.value = !_showMaskOverlay.value
    }

    fun toggleRawBoxes() {
        _showRawBoxes.value = !_showRawBoxes.value
    }

    /**
     * Updates manual mask bitmap after user drawing/erasing.
     */
    fun updateManualMask(editedMask: Bitmap) {
        val currentMask = currentMaskBitmap
        if (currentMask != null) {
            maskUndoStack.add(currentMask.copy(currentMask.config ?: Bitmap.Config.ARGB_8888, true))
            maskRedoStack.clear()
            updateUndoRedoStates()
        }

        currentMaskBitmap = editedMask
        recompositeCurrentCensor()
    }

    fun undoMaskEdit() {
        if (maskUndoStack.isNotEmpty()) {
            val current = currentMaskBitmap
            if (current != null) {
                maskRedoStack.add(current)
            }
            val previous = maskUndoStack.removeAt(maskUndoStack.size - 1)
            currentMaskBitmap = previous
            updateUndoRedoStates()
            recompositeCurrentCensor()
        }
    }

    fun redoMaskEdit() {
        if (maskRedoStack.isNotEmpty()) {
            val current = currentMaskBitmap
            if (current != null) {
                maskUndoStack.add(current)
            }
            val next = maskRedoStack.removeAt(maskRedoStack.size - 1)
            currentMaskBitmap = next
            updateUndoRedoStates()
            recompositeCurrentCensor()
        }
    }

    fun clearMask() {
        val original = currentOriginalBitmap ?: return
        val emptyMask = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        updateManualMask(emptyMask)
    }

    fun restoreAiMask() {
        val original = currentOriginalBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val restoredMask = refineMaskUseCase(
                targetWidth = original.width,
                targetHeight = original.height,
                detections = currentRawDetections,
                dilationPx = _censorOptions.value.maskDilationPx,
                featherEdges = _censorOptions.value.featherEdges
            )
            updateManualMask(restoredMask)
        }
    }

    private fun recompositeCurrentCensor() {
        val original = currentOriginalBitmap ?: return
        val mask = currentMaskBitmap ?: return
        // Cancel any in-flight render before starting a new one to prevent OOM
        activeCensorJob?.cancel()
        activeCensorJob = viewModelScope.launch(Dispatchers.Default) {
            val censored = applyCensorUseCase(original, mask, _censorOptions.value.effect)
            if (!isActive) return@launch  // job was cancelled while rendering
            currentCensoredBitmap = censored

            val currentSuccess = _uiState.value as? UIState.Success
            if (currentSuccess != null) {
                _uiState.value = currentSuccess.copy(
                    result = currentSuccess.result.copy(
                        refinedMask = mask,
                        censoredBitmap = censored,
                        maskCoveragePercentage = calculateMaskCoverage(mask)
                    )
                )
            }
        }
    }

    fun exportCensoredImage(stripExif: Boolean) {
        val bitmapToSave = currentCensoredBitmap ?: return
        viewModelScope.launch {
            _exportResult.value = null
            val result = exportImageUseCase(
                bitmap = bitmapToSave,
                stripExif = stripExif,
                sourceUri = currentSourceUri
            )
            _exportResult.value = result
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = maskUndoStack.isNotEmpty()
        _canRedo.value = maskRedoStack.isNotEmpty()
    }

    private fun calculateMaskCoverage(mask: Bitmap): Float {
        val width = mask.width
        val height = mask.height
        val totalPixels = width * height
        val pixels = IntArray(width * height)
        mask.getPixels(pixels, 0, width, 0, 0, width, height)

        var nonZeroCount = 0
        for (pixel in pixels) {
            val alpha = (pixel shr 24) and 0xFF
            if (alpha > 10) nonZeroCount++
        }
        return (nonZeroCount.toFloat() / totalPixels.toFloat()) * 100f
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: Exception) {
            AppLogger.e(LogCategory.IMAGE, "Error decoding bitmap from Uri: $uri", e.stackTraceToString())
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.release()
    }
}
