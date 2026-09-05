# Security & Privacy Policy

## 🔒 Security Guarantees & Privacy First

**P.blurr** is engineered with a strict **Privacy-First Architecture**. The application is designed to function entirely offline, guaranteeing zero external exposure of user media or personal data.

### 1. 100% On-Device Processing
- All image decoding, intimate region detection via ONNX Runtime (`nudenet.onnx`), mask generation, and visual censor rendering occur strictly on your local device hardware.
- The application contains **zero network upload endpoints**, **zero analytics tracking**, and **zero telemetry SDKs**.

### 2. Process Memory & Data Retention
- Imported photos and intermediate memory bitmaps exist only in volatile process RAM during active processing sessions.
- Bitmaps are recycled immediately when navigating away or closing the editor.
- Censored photos are saved to your device's local gallery only when explicitly commanded via the **Save** action.

---

## 🛡️ Supported Versions

We provide security updates and patches for the following versions of P.blurr:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## 📩 Reporting a Vulnerability or Security Concern

We take security and privacy concerns very seriously. If you discover a security vulnerability, privacy flaw, or unexpected network artifact:

1. **Do NOT open a public GitHub issue** for undisclosed security vulnerabilities.
2. Please report the issue directly to the developer **[APPROX](https://github.com/APPROX4)**:
   - **GitHub Security Advisory**: Open a Private Security Advisory at [APPROX4/P.blurr/security/advisories](https://github.com/APPROX4/P.blurr/security/advisories)
   - **Developer Profile**: Contact via [github.com/APPROX4](https://github.com/APPROX4)

### What to include in your report:
- A detailed description of the vulnerability or flaw.
- Steps or proof-of-concept to reproduce the issue.
- Impact assessment regarding local device data or process isolation.

### Response Timeline
- **Acknowledgement**: Within 24-48 hours.
- **Triage & Patch**: High-priority patches will be released via a new GitHub release tag within 7 days.