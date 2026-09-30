# Frequently Asked Questions (FAQ)

Questions and design rationale regarding **Murmr**, its acoustic protocol, and its security model.

---

### 1. How does Murmr differ from Signal or WhatsApp?

| Dimension | Signal / WhatsApp | Murmr |
|---|---|---|
| **Identity** | Bound to telephone numbers | **Zero PII**: Pure ephemeral local UUIDs |
| **Key Exchange** | Trust On First Use (TOFU) via central server | **Out-of-Band (OOB)** physical acoustic channel |
| **Trust Model** | Must trust directory server not to MITM initial keys | **Zero-Trust**: Key exchange never touches any server |
| **Metadata Storage** | Server stores social graph / contact registration | **Zero Knowledge**: Server only sees blind envelopes |
| **Session Expiry** | Manually set per chat or persistent | **Cryptographic auto-expiration** with key wiping |

---

### 2. Can humans hear the acoustic pairing sound?

The acoustic pairing carrier operates in the **15 kHz – 19 kHz** frequency spectrum.
- For most adults, sounds above 15–16 kHz fall outside the typical perceptual hearing range due to natural presbycusis.
- The transmission duration is short (typically < 3 seconds).
- The volume level is auto-calibrated to minimize audibility while maintaining sufficient signal-to-noise ratio (SNR) over a 20–50 cm distance.

---

### 3. Does background ambient noise disrupt the pairing?

Standard human conversation, street traffic, music, and household noise concentrate their acoustic energy below **8 kHz**.
Murmr's DSP receiver applies:
1. A digital **high-pass bandpass filter** centered at 15–19 kHz, rejecting almost all human voice and ambient noise.
2. An FFT-based energy peak detector that calculates SNR relative to background noise.
3. Slotted ALOHA retransmission if a packet collision or unexpected high-frequency spike occurs.

---

### 4. Why use sound instead of Bluetooth (BLE) or QR codes?

- **Versus QR Codes**: QR codes require clean camera lenses, adequate ambient lighting (they fail in the dark), line of sight, and steady hands. Sound works in total darkness, inside pockets or bags placed next to each other, and requires zero camera permissions.
- **Versus Bluetooth (BLE)**: BLE pairing triggers OS-level system modals, reveals persistent Bluetooth MAC addresses that facilitate hardware tracking, and suffers from long discovery timeouts and permission friction. Sound requires only standard mic/speaker audio permissions.

---

### 5. Why is audio half-duplex and how does Slotted ALOHA solve it?

A smartphone cannot cleanly record ultrasonic audio through its microphone while its speaker is blasting ultrasonic tones; the speaker output saturates the microphone diaphragm and creates severe acoustic echo.

Murmr solves this by treating the room as a **half-duplex shared medium**:
- Devices emit randomized beacon chirps.
- The first clean handshake locks one phone as Transmitter and the other as Receiver.
- Once the initial payload is transferred, the devices swap roles deterministically.

---

### 6. What happens if an attacker records the ultrasonic sound in the room?

Public keys are, by definition, **public**. An attacker capturing the acoustic audio only obtains $pk_A$ and $pk_B$ (the ephemeral X25519 public keys).
Under the Diffie-Hellman problem on Curve25519:
$$\text{Given } pk_A = sk_A \cdot G \text{ and } pk_B = sk_B \cdot G \text{, computing } SS = sk_A \cdot sk_B \cdot G \text{ is computationally infeasible.}$$
Confidentiality is guaranteed mathematically. Active injection is prevented by the 5-emoji SAS verification step.

---

### 7. What academic work does this project represent?

Murmr was researched, designed, implemented, and defended by **Pablo Sirvent Jiménez** as a **Final Degree Project (Trabajo de Fin de Grado - TFG)** in Computer Engineering at the **Universitat Oberta de Catalunya (UOC)**.
