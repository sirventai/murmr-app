# Cryptographic Architecture & Pipeline

Murmr guarantees end-to-end security through a multi-stage cryptographic pipeline combining physical out-of-band key agreement, visual human verification, and high-performance authenticated symmetric encryption.

---

## 1. Cryptographic Primitive Stack

| Purpose | Primitive / Algorithm | Standard / Reference |
|---|---|---|
| **Asymmetric Key Agreement** | Curve25519 (X25519) | RFC 7748 |
| **Key Derivation (KDF)** | HKDF-SHA256 | RFC 5869 |
| **Symmetric Payload Encryption** | AES-256-GCM (128-bit auth tag) | NIST SP 800-38D |
| **Short Authentication String (SAS)** | 5 Visual Emojis over 32-symbol alphabet | RFC 4086 / Custom SAS |
| **Digest & Fingerprinting** | SHA-256 / Blake2b | FIPS 180-4 |

---

## 2. Complete Key Exchange & Derivation Flow

```
                      Device A                                 Device B
                  (Alice's Phone)                          (Bob's Phone)
                         |                                       |
    Generate key pair:   |                                       | Generate key pair:
    (sk_A, pk_A)         |                                       | (sk_B, pk_B)
                         |                                       |
                         |--- [OOB Acoustic Frame: pk_A] ------->|
                         |<-- [OOB Acoustic Frame: pk_B] --------|
                         |                                       |
    Compute ECDH Secret: |                                       | Compute ECDH Secret:
    SS = X25519(sk_A,pk_B)|                                      | SS = X25519(sk_B,pk_A)
                         |                                       |
                         +-------------------+-------------------+
                                             |
                                    Shared Secret (SS)
                                             |
                                             v
                                  HKDF-Extract(salt, SS)
                                             |
                                             v
                                        Master PRK
                                             |
                   +-------------------------+-------------------------+
                   |                                                   |
                   v                                                   v
         HKDF-Expand("sas", 5)                              HKDF-Expand("session", 32)
                   |                                                   |
                   v                                                   v
          5 SAS Emoji Indices                                  Root Key (AES-256)
        (Visual Anti-MITM Check)                                       |
                                                                       v
                                                           Symmetric Ratchet / GCM Key
```

---

## 3. Acoustic Anti-MITM: Visual SAS Emoji Verification

Although acoustic sound travels through physical space, an attacker equipped with directional ultrasonic microphones and ultrasonic parametric speakers could theoretically execute an active Man-in-the-Middle (MITM) attack if they were positioned nearby.

To eliminate this vulnerability without requiring cryptographic certificates or online authorities, Murmr implements a **Short Authentication String (SAS)** verification:

1. Both devices derive 25 bits of entropy from the shared secret via HKDF:
   $$\text{SAS\_Bytes} = \text{HKDF-Expand}(PRK, \text{"murmr-sas-v1"}, 4)$$
2. The 25 bits are partitioned into $5 \times 5\text{-bit}$ chunks ($2^5 = 32$ values each).
3. Each 5-bit index maps to an easily distinguishable emoji from a curated 32-emoji set:
   ```
   [0: 🦊] [1: 🐻] [2: 🐼] [3: 🐨] [4: 🦁] [5: 🐯] [6: 🐵] [7: 🦄] ...
   ```
4. **Asymmetric UX**:
   - Device A displays the 5 target emojis.
   - Device B presents a shuffled $4 \times 4$ or $4 \times 8$ selection grid.
   - User A reads out or shows their screen; User B selects the 5 emojis.
   - If a MITM attacker altered either public key, the calculated secrets diverge and the emojis will not match with probability $1 - \frac{1}{2^{25}} = 99.999997\%$.

---

## 4. Message Encryption (AES-256-GCM)

Once paired, messages sent over the network transport are protected with AES-256 in Galois/Counter Mode (GCM):

```
Envelope:
{
  "senderDeviceId": "4f9c...-uuid",
  "recipientDeviceId": "a18b...-uuid",
  "sessionId": "s-928174",
  "iv": "<12-byte random IV, Base64>",
  "ciphertext": "<Encrypted plaintext, Base64>",
  "tag": "<16-byte GCM authentication tag, Base64>",
  "timestamp": 1715124000000
}
```

- **Confidentiality**: The plaintext message is encrypted with a 256-bit session key.
- **Authenticity & Integrity**: The 128-bit authentication tag guarantees that any modification in transit by the relay server or network intermediaries is detected immediately, resulting in envelope rejection.
- **Replay Protection**: Envelopes track increasing sequence numbers and unique cryptographic IVs per message.

---

## 5. Forward Secrecy & Session Key Ratcheting

Murmr prevents historical compromise through **cryptographic key rotation**:
- Each session derives directional ratchet keys.
- Sending or receiving messages advances the state of the key derivation chain:
  $$K_{n+1} = \text{HMAC-SHA256}(K_n, \text{"ratchet-advance"})$$
- Once a key is used to encrypt or decrypt, it is immediately scrubbed from memory. If a device is seized at time $T$, past messages ($< T$) cannot be decrypted because the prior keys no longer exist.
- Sessions also enforce a time-to-live (`SessionExpiry`). When expired, session keys are erased from the secure vault.
