# Security Policy

Murmr is designed for high-assurance, end-to-end encrypted messaging. As a security and cryptography research project, we prioritize the integrity of its cryptographic protocols and implementation.

---

## Supported Versions

| Version | Supported |
| :--- | :--- |
| `1.0.x` | :white_check_mark: Supported |
| `< 1.0.0` | :x: End of Life |

---

## Reporting a Vulnerability

**Please do NOT report potential security vulnerabilities through public GitHub issues.**

To report a vulnerability responsibly:
1. Open a private security advisory on GitHub under the repository's **Security** tab -> **Report a vulnerability**.
2. Or email **pablo@purebyte.ai** with the subject `[SECURITY] Murmr App Vulnerability`.

### When reporting, please provide:
- A detailed description of the vulnerability.
- Steps to reproduce or proof-of-concept code.
- Affected platforms (Android, iOS, Desktop JVM) and OS versions.
- Potential impact and threat model vector (e.g., acoustic injection, cryptographic replay, local storage exfiltration).

### Response Timeline
- **Initial Acknowledgement**: Within 48 hours.
- **Triage & Assessment**: Within 5 business days.
- **Remediation & Advisory**: We coordinate coordinated disclosure (typically within 60 to 90 days).

---

## Threat Model & Scope

### In Scope
- **Cryptographic Weaknesses**: Flaws in X25519 ECDH key agreement, HKDF-SHA256 key derivation, AES-256-GCM encryption/authentication, or session key rotation / ratcheting.
- **Acoustic Channel Exploits**: Unauthenticated acoustic signal injection bypassing the SAS (Short Authentication String) emoji verification protocol.
- **Local Storage Leaks**: Insecure persistence of private keys, decrypted message databases, or sensitive session states on Android Keystore, iOS Keychain, or Desktop secure storage.
- **Network Leakage**: Plaintext leakage of messages, identity metadata, or cryptographic parameters over the network transport.

### Out of Scope
- Compromise of the physical hardware device (rooted/jailbroken operating systems where an attacker has arbitrary kernel-level code execution or physical memory dump access).
- Acoustic eavesdropping of public keys where an attacker records ultrasound audio (public keys are intentionally public and unencrypted; confidentiality is guaranteed mathematically by ECDH Diffie-Hellman hardness).
- Ultrasonic signal jamming / physical acoustic denial of service (DoS) caused by loud ultrasonic noise in the room (mitigated by SNR fallback and visual timeouts).
- Relay server downtime or network disconnects.
