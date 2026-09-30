package uoc.edu.psirvent.murmr.crypto

object PureAesGcm {

    fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray, aad: ByteArray): AeadCiphertext {
        require(key.size == AES_KEY_BYTES) { "key debe ser $AES_KEY_BYTES bytes" }
        require(iv.size == AES_GCM_IV_BYTES) { "iv debe ser $AES_GCM_IV_BYTES bytes" }
        val (ct, tag) = gcm(key, iv, plaintext, aad, encrypting = true)
        return AeadCiphertext(ct, tag)
    }

    fun decrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray, authTag: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == AES_KEY_BYTES) { "key debe ser $AES_KEY_BYTES bytes" }
        require(iv.size == AES_GCM_IV_BYTES) { "iv debe ser $AES_GCM_IV_BYTES bytes" }
        require(authTag.size == AES_GCM_TAG_BYTES) { "tag debe ser $AES_GCM_TAG_BYTES bytes" }
        val (pt, computedTag) = gcm(key, iv, ciphertext, aad, encrypting = false)
        if (!constantTimeEq(computedTag, authTag)) throw AeadDecryptionFailed("tag inválido")
        return pt
    }

    private fun gcm(
        key: ByteArray,
        iv: ByteArray,
        input: ByteArray,
        aad: ByteArray,
        encrypting: Boolean,
    ): Pair<ByteArray, ByteArray> {
        val expanded = expandKey(key)

        val j0 = ByteArray(16)
        iv.copyInto(j0, 0, 0, 12)
        j0[15] = 1

        val out = ByteArray(input.size)
        var counter = j0.copyOf()
        var pos = 0
        while (pos < input.size) {
            incrementCounter(counter)
            val keystream = aesEncrypt(counter, expanded)
            val n = minOf(16, input.size - pos)
            for (i in 0 until n) out[pos + i] = (input[pos + i].toInt() xor keystream[i].toInt()).toByte()
            pos += n
        }

        val h = aesEncrypt(ByteArray(16), expanded)
        val ctForGhash = if (encrypting) out else input
        val ghashInput = buildGhashInput(aad, ctForGhash)
        val s = ghash(h, ghashInput)

        val ek = aesEncrypt(j0, expanded)
        val tag = ByteArray(16) { i -> (ek[i].toInt() xor s[i].toInt()).toByte() }

        return out to tag
    }

    private fun buildGhashInput(aad: ByteArray, ciphertext: ByteArray): ByteArray {
        val aadPad = (16 - aad.size % 16) % 16
        val ctPad = (16 - ciphertext.size % 16) % 16
        val len = aad.size + aadPad + ciphertext.size + ctPad + 16
        val out = ByteArray(len)
        aad.copyInto(out, 0)
        ciphertext.copyInto(out, aad.size + aadPad)

        val aadBits = aad.size.toLong() * 8
        val ctBits = ciphertext.size.toLong() * 8
        val lenOff = aad.size + aadPad + ciphertext.size + ctPad
        writeLongBE(out, lenOff, aadBits)
        writeLongBE(out, lenOff + 8, ctBits)
        return out
    }

    private fun ghash(h: ByteArray, data: ByteArray): ByteArray {
        var y = ByteArray(16)
        var i = 0
        while (i < data.size) {
            for (j in 0 until 16) y[j] = (y[j].toInt() xor data[i + j].toInt()).toByte()
            y = gfMul(y, h)
            i += 16
        }
        return y
    }

    private fun gfMul(x: ByteArray, y: ByteArray): ByteArray {
        var z = ByteArray(16)
        val v = y.copyOf()
        for (i in 0 until 128) {
            val byte = i ushr 3
            val bit = 7 - (i and 7)
            val xi = (x[byte].toInt() ushr bit) and 1
            if (xi == 1) for (j in 0 until 16) z[j] = (z[j].toInt() xor v[j].toInt()).toByte()
            val lsb = v[15].toInt() and 1
            shiftRight1(v)
            if (lsb == 1) v[0] = (v[0].toInt() xor 0xE1).toByte()
        }
        return z
    }

    private fun shiftRight1(v: ByteArray) {
        var carry = 0
        for (i in 0 until 16) {
            val cur = v[i].toInt() and 0xFF
            v[i] = ((cur ushr 1) or (carry shl 7)).toByte()
            carry = cur and 1
        }
    }

    private fun incrementCounter(c: ByteArray) {
        for (i in 15 downTo 12) {
            val v = ((c[i].toInt() and 0xFF) + 1) and 0xFF
            c[i] = v.toByte()
            if (v != 0) return
        }
    }

    private fun writeLongBE(out: ByteArray, offset: Int, v: Long) {
        for (i in 0 until 8) out[offset + i] = (v ushr (56 - i * 8)).toByte()
    }

    private fun constantTimeEq(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private val SBOX = intArrayOf(
        0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
        0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
        0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
        0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
        0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
        0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
        0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
        0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
        0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
        0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
        0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
        0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
        0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
        0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
        0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
        0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16,
    )

    private val RCON = intArrayOf(
        0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8, 0xab, 0x4d,
    )

    private fun expandKey(key: ByteArray): IntArray {

        val nk = 8
        val nr = 14
        val w = IntArray(4 * (nr + 1))
        for (i in 0 until nk) {
            w[i] = ((key[4 * i].toInt() and 0xFF) shl 24) or
                ((key[4 * i + 1].toInt() and 0xFF) shl 16) or
                ((key[4 * i + 2].toInt() and 0xFF) shl 8) or
                (key[4 * i + 3].toInt() and 0xFF)
        }
        for (i in nk until 4 * (nr + 1)) {
            var t = w[i - 1]
            if (i % nk == 0) {
                t = subWord(rotWord(t)) xor (RCON[i / nk] shl 24)
            } else if (nk > 6 && i % nk == 4) {
                t = subWord(t)
            }
            w[i] = w[i - nk] xor t
        }
        return w
    }

    private fun rotWord(w: Int): Int = (w shl 8) or (w ushr 24)
    private fun subWord(w: Int): Int =
        (SBOX[(w ushr 24) and 0xFF] shl 24) or
            (SBOX[(w ushr 16) and 0xFF] shl 16) or
            (SBOX[(w ushr 8) and 0xFF] shl 8) or
            SBOX[w and 0xFF]

    private fun aesEncrypt(input: ByteArray, w: IntArray): ByteArray {
        val nr = 14

        val s = IntArray(4)
        for (c in 0 until 4) {
            s[c] = ((input[4 * c].toInt() and 0xFF) shl 24) or
                ((input[4 * c + 1].toInt() and 0xFF) shl 16) or
                ((input[4 * c + 2].toInt() and 0xFF) shl 8) or
                (input[4 * c + 3].toInt() and 0xFF)
        }

        for (c in 0 until 4) s[c] = s[c] xor w[c]
        for (round in 1 until nr) {

            val ns = IntArray(4)
            for (c in 0 until 4) {
                val a0 = SBOX[(s[c] ushr 24) and 0xFF]
                val a1 = SBOX[(s[(c + 1) % 4] ushr 16) and 0xFF]
                val a2 = SBOX[(s[(c + 2) % 4] ushr 8) and 0xFF]
                val a3 = SBOX[s[(c + 3) % 4] and 0xFF]
                ns[c] = (mix2(a0) xor mix3(a1) xor a2 xor a3) shl 24 or
                    (((a0 xor mix2(a1) xor mix3(a2) xor a3) and 0xFF) shl 16) or
                    (((a0 xor a1 xor mix2(a2) xor mix3(a3)) and 0xFF) shl 8) or
                    ((mix3(a0) xor a1 xor a2 xor mix2(a3)) and 0xFF)
            }
            for (c in 0 until 4) s[c] = ns[c] xor w[round * 4 + c]
        }

        val ns = IntArray(4)
        for (c in 0 until 4) {
            val a0 = SBOX[(s[c] ushr 24) and 0xFF]
            val a1 = SBOX[(s[(c + 1) % 4] ushr 16) and 0xFF]
            val a2 = SBOX[(s[(c + 2) % 4] ushr 8) and 0xFF]
            val a3 = SBOX[s[(c + 3) % 4] and 0xFF]
            ns[c] = (a0 shl 24) or (a1 shl 16) or (a2 shl 8) or a3
        }
        for (c in 0 until 4) s[c] = ns[c] xor w[nr * 4 + c]

        val out = ByteArray(16)
        for (c in 0 until 4) {
            out[4 * c] = (s[c] ushr 24).toByte()
            out[4 * c + 1] = (s[c] ushr 16).toByte()
            out[4 * c + 2] = (s[c] ushr 8).toByte()
            out[4 * c + 3] = s[c].toByte()
        }
        return out
    }

    private fun mix2(b: Int): Int {
        val x = b and 0xFF
        return ((x shl 1) xor (if ((x and 0x80) != 0) 0x1B else 0)) and 0xFF
    }
    private fun mix3(b: Int): Int = mix2(b) xor (b and 0xFF)
}
