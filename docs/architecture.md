# Architecture Documentation

Murmr is engineered around **Clean Architecture**, **Model-View-Intent (MVI)** reactive state handling, and **Kotlin Multiplatform (KMP)** principles to ensure 100% cryptographic and business logic parity across Android, Desktop JVM, and iOS.

---

## 1. High-Level System Overview

Murmr bifurcates communication into two decoupled channels:
1. **Out-of-Band (OOB) Physical Acoustic Channel**: Used exclusively during device-to-device pairing to negotiate identities and establish authenticated cryptographic keys.
2. **Online Blind Transport Channel**: Used for transmitting end-to-end encrypted message envelopes via an asynchronous zero-knowledge relay server.

```
       +-----------------------------------------------------------+
       |                  Physical Acoustic Link                   |
       |               (15 - 19 kHz Near-Ultrasound)               |
       |  - Slotted ALOHA Role Negotiation                         |
       |  - X25519 Public Key Exchange                             |
       |  - Visual SAS (Short Authentication String) Emoji Check   |
       +-----------------------------+-----------------------------+
                                     ^
                                     |
               +---------------------+---------------------+
               |                                           |
      +--------+--------+                         +--------+--------+
      |  Device A (App) |                         |  Device B (App) |
      |  - UUID A       |                         |  - UUID B       |
      |  - Local DB     |                         |  - Local DB     |
      |  - Secure Vault |                         |  - Secure Vault |
      +--------+--------+                         +--------+--------+
               |                                           |
               |       +---------------------------+       |
               +------>| Zero-Knowledge Relay      |<------+
                       | - Blind Envelope Routing  |
                       | - In-Memory Ephemeral Box |
                       | - Persistent WebSockets   |
                       +---------------------------+
```

---

## 2. Kotlin Multiplatform Code Organization

The client codebase is structured within `composeApp/src`:

```
composeApp/src/
├── commonMain/              # 100% Shared Business Logic & UI
│   ├── kotlin/uoc/edu/psirvent/murmr/
│   │   ├── audio/           # DSP interfaces, frame decoders, acoustic state machine
│   │   ├── crypto/          # X25519, HKDF, AES-GCM abstractions & SAS generator
│   │   ├── data/            # SQLDelight database drivers, repositories, network clients
│   │   ├── domain/          # Entities (Contact, Message, Session), Use Cases, Enums
│   │   └── presentation/    # Compose Multiplatform UI, MVI ViewModels, theme, screens
│   └── sqldelight/          # SQLDelight schema (.sq files) for local SQLite persistence
│
├── androidMain/             # Android Platform Implementations
│   ├── kotlin/              # AudioRecord / AudioTrack, Android Keystore, Activity
│   └── res/                 # Android manifest, adaptive launcher icons, resources
│
├── jvmMain/                 # Desktop JVM Platform Implementations
│   └── kotlin/              # Java Sound API (TargetDataLine/SourceDataLine), Desktop vaults
│
└── iosMain/                 # iOS Platform Implementations
    └── kotlin/              # CoreAudio / AVFoundation wrappers, Keychain integration
```

---

## 3. Layered Design

```
+-----------------------------------------------------------------+
|                      Presentation Layer                         |
|   - Jetpack Compose / Compose Multiplatform                     |
|   - Unidirectional Data Flow (MVI): StateFlow & UI Events       |
|   - Screens: Onboarding, Pairing, Chat, Contacts, Settings      |
+-----------------------------------------------------------------+
                                |
                                v
+-----------------------------------------------------------------+
|                         Domain Layer                            |
|   - Use Cases: PairDeviceUseCase, SendMessageUseCase,           |
|                RotateKeyUseCase, ExpireMessagesUseCase          |
|   - Pure business models: Contact, Message, Session, Identity   |
+-----------------------------------------------------------------+
                                |
                                v
+-----------------------------------------------------------------+
|                          Data Layer                             |
|   - Repository implementations (ContactRepo, MessageRepo)       |
|   - SQLDelight SQLite storage with automatic migrations         |
|   - Ktor HTTP & WebSocket network client                        |
+-----------------------------------------------------------------+
        |                                       |
        v                                       v
+-----------------------+              +--------------------------+
|  Crypto Engine        |              |  Audio Physical Layer    |
|  - X25519 ECDH        |              |  - FFT Spectrogram       |
|  - HKDF-SHA256        |              |  - Modulator / Demod     |
|  - AES-256-GCM        |              |  - Slotted ALOHA State   |
+-----------------------+              +--------------------------+
```

---

## 4. Local Storage & Security Architecture

1. **SQLDelight SQLite Database**:
   - Stores contacts, verified public keys, active chat threads, and encrypted message history.
   - Database operations are completely offline-first.
2. **Hardware-Backed Key Vault**:
   - Device identity private keys and active shared secrets are managed via platform hardware-backed enclaves:
     - **Android**: `AndroidKeyStore` with StrongBox / TEE.
     - **iOS**: Apple Secure Enclave & Keychain Services.
     - **Desktop**: Platform-appropriate encrypted file storage.
3. **Session Auto-Expiry**:
   - Background coroutine job periodically evicts messages exceeding the negotiated `SessionExpiry` threshold, overwriting records in SQLite and updating the UI reactively.
