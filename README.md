<div align="center">

<img src="p.blurr logo.jpg" alt="P.blurr Logo" width="130" style="border-radius: 28px;" />

# P.blurr

### Premium On-Device Intimate Region Censoring for Android

[![Platform](https://img.shields.io/badge/Platform-Android-000000?style=for-the-badge&logo=android&logoColor=white)](https://github.com/APPROX4/P.blurr)
[![AI Engine](https://img.shields.io/badge/AI%20Engine-ONNX%20Runtime-000000?style=for-the-badge&logo=onnx&logoColor=white)](https://github.com/APPROX4/P.blurr)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-000000?style=for-the-badge&logo=shield&logoColor=white)](https://github.com/APPROX4/P.blurr)
[![License](https://img.shields.io/badge/License-MIT-000000?style=for-the-badge)](LICENSE)

**P.blurr** is an automated, high-precision Android application designed to detect and censor intimate exposed regions in photos locally on-device. Powered by local **ONNX Runtime** neural network inference, P.blurr ensures complete offline data privacy while providing pixel-perfect mask controls and manual drawing shape tools.

Developed with a modern pitch-black white bloom aesthetic by **[APPROX](https://github.com/APPROX4)**.

---

</div>

## ✨ Key Features

- 🔒 **100% On-Device & Offline Privacy**: All neural network detection and image rendering happen strictly on local hardware. Zero cloud processing, zero analytics, zero image uploads.
- ⚡ **ONNX Runtime AI Engine**: Direct local execution of official NudeNet ONNX neural network weights (`nudenet.onnx`), running high-precision bounding box extraction in milliseconds.
- 🎯 **Targeted Intimate Masking**: Smart class-aware filtering targets exposed intimate regions (breasts, genitalia, buttocks, anus) while strictly leaving faces, body skin, and backgrounds pristine.
- 🎨 **Censor Visual Modes**: Choose between **Pixelate** (adjustable block size) and **Gaussian Blur** (adjustable intensity).
- ✏️ **Manual Mask Editing Tools**: Live interactive drawing overlay with **Rectangle (Box)**, **Circle (Round)**, **Freehand Brush**, and **Eraser** tools with full Undo/Redo history.
- 💎 **Pitch-Black White Bloom UI**: Built with Jetpack Compose featuring smooth 60 FPS transitions, minimal glassmorphism cards, and text glyph shadow animations.
- 🚀 **In-App Update Recommended Checker**: Automatically checks GitHub API for new releases and prompts with one-tap updates.

---

## 📱 Supported Devices & System Requirements

Based on the codebase build configuration and runtime specifications:

| Requirement Category | Specification Details |
| -------------------- | --------------------- |
| **Minimum OS**       | Android 8.0 Oreo (`API Level 26`) or higher |
| **Target OS**        | Android 14 (`API Level 34`) & Android 15 compatible |
| **CPU Architectures** | `arm64-v8a` (64-bit ARM), `armeabi-v7a` (32-bit ARM), `x86_64`, `x86` |
| **Recommended RAM**  | 2 GB+ (for high-resolution bitmap buffer processing) |
| **Hardware Threads** | Utilizes 4 intra-op CPU cores for ONNX NPU/CPU neural inference |
| **Supported Image Formats** | JPEG, PNG, WEBP, HEIC, HEIF, BMP |
| **Network Requirements** | None (Fully offline operation) |

---

## 🛠️ Architecture & Tech Stack

P.blurr is built strictly following modern Android Clean Architecture and Jetpack Compose best practices.

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose with Material 3 Design
- **Inference Engine**: ONNX Runtime Android SDK (`ai.onnxruntime:onnxruntime-android`)
- **Neural Network Weights**: Official NudeNet ONNX Model
- **Image Processing**: Android Canvas & Hardware Bitmap Rendering Matrix Transforms
- **State Management**: Kotlin Coroutines & `StateFlow`
- **Navigation**: Jetpack Compose Navigation

```
app/src/main/java/com/pblurr/app/
├── data/
│   ├── censor/            # Pixelate & Gaussian Blur Bitmap Engine
│   ├── inference/         # ONNX Session, Letterboxing & NMS Processing
│   ├── logging/           # Diagnostic System Logger
│   ├── settings/          # SharedPreferences Repository
│   └── update/            # GitHub Release API Update Checker
├── domain/
│   ├── detector/          # Interface Contracts for Detection Engine
│   ├── model/             # BoundingBox, DetectionMask & CensorOptions
│   └── usecase/           # Region Detection, Refinement & Export Use Cases
└── presentation/
    ├── ui/
    │   ├── editor/        # Visual Censor & Shape Drawing Overlay
    │   ├── home/          # Gallery Picker & Primary Controls
    │   ├── settings/      # Developer Info & System Controls
    │   └── theme/         # Pitch-Black Pitch Palette & Typography
```

---

## 🚀 Building & Running from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK 34

### Compilation
1. Clone the repository:
   ```bash
   git clone https://github.com/APPROX4/P.blurr.git
   ```
2. Open the project in Android Studio.
3. Build the Release APK:
   ```bash
   ./gradlew assembleRelease
   ```
4. The generated APK will be available in `app/build/outputs/apk/release/`.

---

## 👤 Developer & Owner

Developed and maintained by **[APPROX](https://github.com/APPROX4)**.

- **GitHub Repository**: [APPROX4/P.blurr](https://github.com/APPROX4/P.blurr)
- **Profile**: [@APPROX4](https://github.com/APPROX4)

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
