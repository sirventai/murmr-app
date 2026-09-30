# Changelog

All notable changes to **Murmr** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-05-08

### Added
- **Acoustic Out-of-Band (OOB) Pairing**:
  - Near-ultrasonic physical layer (15 kHz – 19 kHz) for direct device-to-device key exchange.
  - Slotted ALOHA collision-avoidance protocol for half-duplex audio role negotiation.
  - Live FFT spectrogram visualizer in pairing screen.
- **End-to-End Cryptography Engine**:
  - X25519 Elliptic-Curve Diffie-Hellman (ECDH) key agreement.
  - HKDF-SHA256 key derivation pipeline.
  - Short Authentication String (SAS) visual verification using a 32-emoji visual alphabet (5 emojis, entropy $2^{25}$).
  - AES-256-GCM authenticated symmetric encryption for all message payloads.
  - Automated session key ratcheting and rotation.
- **Cross-Platform Compose Multiplatform UI**:
  - Unified Material 3 design system with custom brand theming (`MurmrTheme`).
  - Android native client (API 26+).
  - Desktop JVM client (macOS, Windows, Linux).
  - iOS client wrapper via SwiftUI and Compose Framework.
- **Privacy & Security Architecture**:
  - Zero PII: no phone numbers, no emails, anonymous local UUIDs.
  - Platform secure storage integration: Android Keystore, iOS Keychain, encrypted desktop vaults.
  - Ephemeral chat sessions with configurable auto-expiry (30s, 5m, 1h, 1d, 7d, 30d, or custom).
  - Offline-first local message and contact persistence powered by SQLDelight SQLite.
- **Network Transport**:
  - Seamless dual-mode connectivity: persistent WebSockets for live push and HTTP REST fallback for offline inbox emptying.
  - Asynchronous background message delivery queues with delivery and read receipt indicators (`·`, `✓`, `✓✓`).

### Academic Context
- Developed and defended as a Final Degree Project (**Trabajo de Fin de Grado - TFG**) in Computer Engineering at **Universitat Oberta de Catalunya (UOC)**.
