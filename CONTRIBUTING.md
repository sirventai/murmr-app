# Contributing to Murmr

Thank you for your interest in contributing to Murmr!

Murmr is an open-source, research-grade, privacy-first messaging application developed as a **Final Degree Project (Trabajo de Fin de Grado - TFG)** in Computer Engineering at the **Universitat Oberta de Catalunya (UOC)**.

We welcome bug reports, design feedback, security audits, and code contributions.

---

## Code of Conduct

All contributors and maintainers are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please report any unacceptable behavior to **pablo@purebyte.ai**.

---

## How to Contribute

### 1. Reporting Bugs
- Check the [issue tracker](https://github.com/sirventai/murmr-app/issues) first to ensure the bug hasn't already been reported.
- If you find a new bug, please open an issue describing:
  - Your environment (OS, device model, Android/iOS version, JVM version).
  - Clear steps to reproduce the issue.
  - Expected behavior vs. observed behavior.
  - Logs or error traces if applicable.
- **Do NOT disclose potential security vulnerabilities in public issues**. See our [Security Policy](SECURITY.md).

### 2. Feature Requests & Proposals
- Before implementing major changes, especially to the acoustic PHY/MAC protocol, cryptography layer, or networking primitives, open a Discussion or RFC Issue to align with the core architectural tenets.

### 3. Pull Requests
- Fork the repository and create your feature branch:
  ```sh
  git checkout -b feature/acoustic-preamble-optimization
  ```
- Keep changes concise, well-documented, and covered by unit or integration tests.
- Ensure all automated checks pass locally:
  ```sh
  ./gradlew test
  ./gradlew :composeApp:testDebugUnitTest
  ```
- Write clear, conventional commit messages:
  - `feat(...)`: new feature
  - `fix(...)`: bug fix
  - `docs(...)`: documentation improvements
  - `refactor(...)`: code refactoring without behavior change
  - `test(...)`: adding or updating tests

---

## Development Guidelines

### Architectural Tenets
1. **Zero-Knowledge by Design**: The relay server must never see plaintext data, session keys, or identity metadata.
2. **Acoustic Out-of-Band Integrity**: The physical sound channel (15–19 kHz) is used exclusively for key exchange; never send messages over sound.
3. **No Personal Data**: Never introduce identifiers linked to real-world individuals (no phone numbers, emails, or central account registries).
4. **Offline-First Storage**: Messages, contacts, and key history are strictly managed in local encrypted storage (SQLDelight + system secure enclaves / Keystore / Keychain).

### Code Style
- Kotlin coding conventions adhere strictly to official [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html) and ktlint rules.
- Maintain immutability and functional patterns where possible.
- Use coroutines and reactive flows for async operations (`StateFlow`, `SharedFlow`).
- Preserve docstrings and cryptographic invariants.

---

## Questions & Community

If you have questions about the codebase, research background, or acoustic demodulation implementation, feel free to open a GitHub Discussion or reach out to the project author:
- **Author**: Pablo Sirvent Jiménez
- **Email**: pablo@purebyte.ai
