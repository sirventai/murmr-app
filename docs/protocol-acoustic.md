# Acoustic Out-of-Band (OOB) Pairing Protocol

Murmr exchanges identity public keys physically between co-located devices using near-ultrasound acoustic waves. This document details the physical layer (PHY), media access control (MAC), and frame structure.

---

## 1. Why Acoustic Sound?

Traditional secure messaging applications rely on **TOFU (Trust On First Use)** backed by a centralized server (e.g., Signal Key Distribution Server). While convenient, this architecture requires trusting the central server not to serve malicious public keys during initial key agreement.

Murmr shifts initial trust to the **physical realm**:
- Two devices in the same room (~10–50 cm apart) transmit key material directly over air.
- Remote network attackers cannot intercept, manipulate, or inject physical audio in the room without physical proximity.
- Requires no cameras (unlike QR codes), works in complete darkness, and needs no Bluetooth pairing popups or device pairing permissions.

---

## 2. Physical Layer (PHY)

### Frequency Band
- **Range**: **15.0 kHz – 19.0 kHz**.
- **Human Audibility**: Inaudible or virtually imperceptible to most adults, avoiding annoyance during pairing.
- **Hardware Compatibility**: Supported by standard smartphone microphones, speakers, and audio DACs running at standard sample rates (44.1 kHz or 48.0 kHz).

### Modulation Scheme
- **M-ary Frequency-Shift Keying (MFSK)** / Chirp Spread Spectrum modulation.
- Multiple discrete frequency bins represent individual nibbles/symbols.
- Preamble sequence for symbol synchronization and start-of-frame detection.
- Fast Fourier Transform (FFT) analysis is executed in real-time over sliding audio windows to compute frequency power spectra.

```
Frequency (kHz)
  19.0 |   - - - - - - - - - - - - - - - - - - - - - - - - - - - -
       |       [Symbol 3]           [Symbol 7]
  17.0 |                     [Symbol 1]
       |   [Preamble Chirp]
  15.0 |   -------------------------------------------------------
       0ms     50ms     100ms    150ms    200ms    250ms    Time (ms)
```

---

## 3. Media Access Control (MAC): Slotted ALOHA Half-Duplex Arbitration

Smartphones cannot transmit ultrasound through speakers while simultaneously listening cleanly through microphones on the same audio hardware due to **acoustic feedback and speaker-mic cross-talk** (half-duplex channel).

To avoid both phones transmitting at the same time and jamming each other, Murmr employs a **Slotted ALOHA collision-avoidance protocol**:

```
Device A (Initiator)                               Device B (Responder)
       |                                                    |
       |--------------- [Probe Tone / Beacon] ------------->|
       |                                                    |
   (Random wait)                                     (Listens & Locks)
       |                                                    |
       |<------------- [Role ACK + Beacon] -----------------|
       |                                                    |
[Assigned: SENDER]                                  [Assigned: RECEIVER]
       |                                                    |
       |============ [X25519 Public Key Frame] ============>|
       |                                                    |
[Switches to RECEIVER]                              [Switches to SENDER]
       |                                                    |
       |<=========== [X25519 Public Key Frame] =============|
       |                                                    |
[Computes ECDH Secret]                              [Computes ECDH Secret]
       |                                                    |
       +---------------- [Display 5 SAS Emojis] ------------+
```

1. **Beacon Phase**: When "Pair" is initiated, devices emit short chirp beacons at randomized slot intervals ($T_{slot} \approx 120\text{ ms} + \text{random jitter}$).
2. **Role Locking**: The first device to cleanly decode the counterpart's beacon adopts the primary transmitter role; the counterpart acknowledges and enters receive mode.
3. **Turnaround**: Once Device A transmits its payload, it immediately turns off its transmitter and opens its microphone to receive Device B's payload.

---

## 4. Acoustic Frame Format

```
+---------------+----------------+----------------+----------------+---------------+
| Preamble      | Sync Word      | Payload Length | Payload Data   | CRC-16 / Par  |
| (Chirp tones) | (16 bits)      | (8 bits)       | (32-48 bytes)  | (16 bits)     |
+---------------+----------------+----------------+----------------+---------------+
```

- **Preamble**: Linear frequency sweep across 15–18.5 kHz for receiver gain settling and clock synchronization.
- **Sync Word**: Fixed Barker sequence providing sharp autocorrelation peak for sample-exact frame alignment.
- **Payload Length**: Dynamic size indicator.
- **Payload Data**: X25519 ephemeral identity public key (32 bytes) + ephemeral session nonce.
- **CRC-16 / Parity**: Ensures frame integrity; malformed frames are rejected immediately without triggering cryptographic failure.

---

## 5. Live FFT Visualizer

During acoustic pairing, the application displays a real-time spectrogram on screen:
- Audio samples from `AudioRecord` / `TargetDataLine` / `AVAudioEngine` pass through a windowed Hann FFT (size 2048).
- The power in the 15–20 kHz band is mapped into color gradients representing signal presence, background noise floor, and active transmission chirps.
