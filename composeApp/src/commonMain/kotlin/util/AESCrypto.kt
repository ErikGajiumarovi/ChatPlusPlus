package util
import kotlin.experimental.xor
import kotlin.random.Random

/**
 * AES шифрование для Kotlin Multiplatform
 * Реализует AES-128 в режиме CBC с PKCS7 padding
 */
class AESCrypto {
    companion object {
        private const val BLOCK_SIZE = 16
        private const val KEY_SIZE = 16 // AES-128

        // S-box для AES
        private val SBOX = byteArrayOf(
            0x63.toByte(), 0x7c.toByte(), 0x77.toByte(), 0x7b.toByte(), 0xf2.toByte(), 0x6b.toByte(), 0x6f.toByte(), 0xc5.toByte(),
            0x30.toByte(), 0x01.toByte(), 0x67.toByte(), 0x2b.toByte(), 0xfe.toByte(), 0xd7.toByte(), 0xab.toByte(), 0x76.toByte(),
            0xca.toByte(), 0x82.toByte(), 0xc9.toByte(), 0x7d.toByte(), 0xfa.toByte(), 0x59.toByte(), 0x47.toByte(), 0xf0.toByte(),
            0xad.toByte(), 0xd4.toByte(), 0xa2.toByte(), 0xaf.toByte(), 0x9c.toByte(), 0xa4.toByte(), 0x72.toByte(), 0xc0.toByte(),
            0xb7.toByte(), 0xfd.toByte(), 0x93.toByte(), 0x26.toByte(), 0x36.toByte(), 0x3f.toByte(), 0xf7.toByte(), 0xcc.toByte(),
            0x34.toByte(), 0xa5.toByte(), 0xe5.toByte(), 0xf1.toByte(), 0x71.toByte(), 0xd8.toByte(), 0x31.toByte(), 0x15.toByte(),
            0x04.toByte(), 0xc7.toByte(), 0x23.toByte(), 0xc3.toByte(), 0x18.toByte(), 0x96.toByte(), 0x05.toByte(), 0x9a.toByte(),
            0x07.toByte(), 0x12.toByte(), 0x80.toByte(), 0xe2.toByte(), 0xeb.toByte(), 0x27.toByte(), 0xb2.toByte(), 0x75.toByte(),
            0x09.toByte(), 0x83.toByte(), 0x2c.toByte(), 0x1a.toByte(), 0x1b.toByte(), 0x6e.toByte(), 0x5a.toByte(), 0xa0.toByte(),
            0x52.toByte(), 0x3b.toByte(), 0xd6.toByte(), 0xb3.toByte(), 0x29.toByte(), 0xe3.toByte(), 0x2f.toByte(), 0x84.toByte(),
            0x53.toByte(), 0xd1.toByte(), 0x00.toByte(), 0xed.toByte(), 0x20.toByte(), 0xfc.toByte(), 0xb1.toByte(), 0x5b.toByte(),
            0x6a.toByte(), 0xcb.toByte(), 0xbe.toByte(), 0x39.toByte(), 0x4a.toByte(), 0x4c.toByte(), 0x58.toByte(), 0xcf.toByte(),
            0xd0.toByte(), 0xef.toByte(), 0xaa.toByte(), 0xfb.toByte(), 0x43.toByte(), 0x4d.toByte(), 0x33.toByte(), 0x85.toByte(),
            0x45.toByte(), 0xf9.toByte(), 0x02.toByte(), 0x7f.toByte(), 0x50.toByte(), 0x3c.toByte(), 0x9f.toByte(), 0xa8.toByte(),
            0x51.toByte(), 0xa3.toByte(), 0x40.toByte(), 0x8f.toByte(), 0x92.toByte(), 0x9d.toByte(), 0x38.toByte(), 0xf5.toByte(),
            0xbc.toByte(), 0xb6.toByte(), 0xda.toByte(), 0x21.toByte(), 0x10.toByte(), 0xff.toByte(), 0xf3.toByte(), 0xd2.toByte(),
            0xcd.toByte(), 0x0c.toByte(), 0x13.toByte(), 0xec.toByte(), 0x5f.toByte(), 0x97.toByte(), 0x44.toByte(), 0x17.toByte(),
            0xc4.toByte(), 0xa7.toByte(), 0x7e.toByte(), 0x3d.toByte(), 0x64.toByte(), 0x5d.toByte(), 0x19.toByte(), 0x73.toByte(),
            0x60.toByte(), 0x81.toByte(), 0x4f.toByte(), 0xdc.toByte(), 0x22.toByte(), 0x2a.toByte(), 0x90.toByte(), 0x88.toByte(),
            0x46.toByte(), 0xee.toByte(), 0xb8.toByte(), 0x14.toByte(), 0xde.toByte(), 0x5e.toByte(), 0x0b.toByte(), 0xdb.toByte(),
            0xe0.toByte(), 0x32.toByte(), 0x3a.toByte(), 0x0a.toByte(), 0x49.toByte(), 0x06.toByte(), 0x24.toByte(), 0x5c.toByte(),
            0xc2.toByte(), 0xd3.toByte(), 0xac.toByte(), 0x62.toByte(), 0x91.toByte(), 0x95.toByte(), 0xe4.toByte(), 0x79.toByte(),
            0xe7.toByte(), 0xc8.toByte(), 0x37.toByte(), 0x6d.toByte(), 0x8d.toByte(), 0xd5.toByte(), 0x4e.toByte(), 0xa9.toByte(),
            0x6c.toByte(), 0x56.toByte(), 0xf4.toByte(), 0xea.toByte(), 0x65.toByte(), 0x7a.toByte(), 0xae.toByte(), 0x08.toByte(),
            0xba.toByte(), 0x78.toByte(), 0x25.toByte(), 0x2e.toByte(), 0x1c.toByte(), 0xa6.toByte(), 0xb4.toByte(), 0xc6.toByte(),
            0xe8.toByte(), 0xdd.toByte(), 0x74.toByte(), 0x1f.toByte(), 0x4b.toByte(), 0xbd.toByte(), 0x8b.toByte(), 0x8a.toByte(),
            0x70.toByte(), 0x3e.toByte(), 0xb5.toByte(), 0x66.toByte(), 0x48.toByte(), 0x03.toByte(), 0xf6.toByte(), 0x0e.toByte(),
            0x61.toByte(), 0x35.toByte(), 0x57.toByte(), 0xb9.toByte(), 0x86.toByte(), 0xc1.toByte(), 0x1d.toByte(), 0x9e.toByte(),
            0xe1.toByte(), 0xf8.toByte(), 0x98.toByte(), 0x11.toByte(), 0x69.toByte(), 0xd9.toByte(), 0x8e.toByte(), 0x94.toByte(),
            0x9b.toByte(), 0x1e.toByte(), 0x87.toByte(), 0xe9.toByte(), 0xce.toByte(), 0x55.toByte(), 0x28.toByte(), 0xdf.toByte(),
            0x8c.toByte(), 0xa1.toByte(), 0x89.toByte(), 0x0d.toByte(), 0xbf.toByte(), 0xe6.toByte(), 0x42.toByte(), 0x68.toByte(),
            0x41.toByte(), 0x99.toByte(), 0x2d.toByte(), 0x0f.toByte(), 0xb0.toByte(), 0x54.toByte(), 0xbb.toByte(), 0x16.toByte()
        )

        // Обратный S-box для расшифровки
        private val INV_SBOX = byteArrayOf(
            0x52.toByte(), 0x09.toByte(), 0x6a.toByte(), 0xd5.toByte(), 0x30.toByte(), 0x36.toByte(), 0xa5.toByte(), 0x38.toByte(),
            0xbf.toByte(), 0x40.toByte(), 0xa3.toByte(), 0x9e.toByte(), 0x81.toByte(), 0xf3.toByte(), 0xd7.toByte(), 0xfb.toByte(),
            0x7c.toByte(), 0xe3.toByte(), 0x39.toByte(), 0x82.toByte(), 0x9b.toByte(), 0x2f.toByte(), 0xff.toByte(), 0x87.toByte(),
            0x34.toByte(), 0x8e.toByte(), 0x43.toByte(), 0x44.toByte(), 0xc4.toByte(), 0xde.toByte(), 0xe9.toByte(), 0xcb.toByte(),
            0x54.toByte(), 0x7b.toByte(), 0x94.toByte(), 0x32.toByte(), 0xa6.toByte(), 0xc2.toByte(), 0x23.toByte(), 0x3d.toByte(),
            0xee.toByte(), 0x4c.toByte(), 0x95.toByte(), 0x0b.toByte(), 0x42.toByte(), 0xfa.toByte(), 0xc3.toByte(), 0x4e.toByte(),
            0x08.toByte(), 0x2e.toByte(), 0xa1.toByte(), 0x66.toByte(), 0x28.toByte(), 0xd9.toByte(), 0x24.toByte(), 0xb2.toByte(),
            0x76.toByte(), 0x5b.toByte(), 0xa2.toByte(), 0x49.toByte(), 0x6d.toByte(), 0x8b.toByte(), 0xd1.toByte(), 0x25.toByte(),
            0x72.toByte(), 0xf8.toByte(), 0xf6.toByte(), 0x64.toByte(), 0x86.toByte(), 0x68.toByte(), 0x98.toByte(), 0x16.toByte(),
            0xd4.toByte(), 0xa4.toByte(), 0x5c.toByte(), 0xcc.toByte(), 0x5d.toByte(), 0x65.toByte(), 0xb6.toByte(), 0x92.toByte(),
            0x6c.toByte(), 0x70.toByte(), 0x48.toByte(), 0x50.toByte(), 0xfd.toByte(), 0xed.toByte(), 0xb9.toByte(), 0xda.toByte(),
            0x5e.toByte(), 0x15.toByte(), 0x46.toByte(), 0x57.toByte(), 0xa7.toByte(), 0x8d.toByte(), 0x9d.toByte(), 0x84.toByte(),
            0x90.toByte(), 0xd8.toByte(), 0xab.toByte(), 0x00.toByte(), 0x8c.toByte(), 0xbc.toByte(), 0xd3.toByte(), 0x0a.toByte(),
            0xf7.toByte(), 0xe4.toByte(), 0x58.toByte(), 0x05.toByte(), 0xb8.toByte(), 0xb3.toByte(), 0x45.toByte(), 0x06.toByte(),
            0xd0.toByte(), 0x2c.toByte(), 0x1e.toByte(), 0x8f.toByte(), 0xca.toByte(), 0x3f.toByte(), 0x0f.toByte(), 0x02.toByte(),
            0xc1.toByte(), 0xaf.toByte(), 0xbd.toByte(), 0x03.toByte(), 0x01.toByte(), 0x13.toByte(), 0x8a.toByte(), 0x6b.toByte(),
            0x3a.toByte(), 0x91.toByte(), 0x11.toByte(), 0x41.toByte(), 0x4f.toByte(), 0x67.toByte(), 0xdc.toByte(), 0xea.toByte(),
            0x97.toByte(), 0xf2.toByte(), 0xcf.toByte(), 0xce.toByte(), 0xf0.toByte(), 0xb4.toByte(), 0xe6.toByte(), 0x73.toByte(),
            0x96.toByte(), 0xac.toByte(), 0x74.toByte(), 0x22.toByte(), 0xe7.toByte(), 0xad.toByte(), 0x35.toByte(), 0x85.toByte(),
            0xe2.toByte(), 0xf9.toByte(), 0x37.toByte(), 0xe8.toByte(), 0x1c.toByte(), 0x75.toByte(), 0xdf.toByte(), 0x6e.toByte(),
            0x47.toByte(), 0xf1.toByte(), 0x1a.toByte(), 0x71.toByte(), 0x1d.toByte(), 0x29.toByte(), 0xc5.toByte(), 0x89.toByte(),
            0x6f.toByte(), 0xb7.toByte(), 0x62.toByte(), 0x0e.toByte(), 0xaa.toByte(), 0x18.toByte(), 0xbe.toByte(), 0x1b.toByte(),
            0xfc.toByte(), 0x56.toByte(), 0x3e.toByte(), 0x4b.toByte(), 0xc6.toByte(), 0xd2.toByte(), 0x79.toByte(), 0x20.toByte(),
            0x9a.toByte(), 0xdb.toByte(), 0xc0.toByte(), 0xfe.toByte(), 0x78.toByte(), 0xcd.toByte(), 0x5a.toByte(), 0xf4.toByte(),
            0x1f.toByte(), 0xdd.toByte(), 0xa8.toByte(), 0x33.toByte(), 0x88.toByte(), 0x07.toByte(), 0xc7.toByte(), 0x31.toByte(),
            0xb1.toByte(), 0x12.toByte(), 0x10.toByte(), 0x59.toByte(), 0x27.toByte(), 0x80.toByte(), 0xec.toByte(), 0x5f.toByte(),
            0x60.toByte(), 0x51.toByte(), 0x7f.toByte(), 0xa9.toByte(), 0x19.toByte(), 0xb5.toByte(), 0x4a.toByte(), 0x0d.toByte(),
            0x2d.toByte(), 0xe5.toByte(), 0x7a.toByte(), 0x9f.toByte(), 0x93.toByte(), 0xc9.toByte(), 0x9c.toByte(), 0xef.toByte(),
            0xa0.toByte(), 0xe0.toByte(), 0x3b.toByte(), 0x4d.toByte(), 0xae.toByte(), 0x2a.toByte(), 0xf5.toByte(), 0xb0.toByte(),
            0xc8.toByte(), 0xeb.toByte(), 0xbb.toByte(), 0x3c.toByte(), 0x83.toByte(), 0x53.toByte(), 0x99.toByte(), 0x61.toByte(),
            0x17.toByte(), 0x2b.toByte(), 0x04.toByte(), 0x7e.toByte(), 0xba.toByte(), 0x77.toByte(), 0xd6.toByte(), 0x26.toByte(),
            0xe1.toByte(), 0x69.toByte(), 0x14.toByte(), 0x63.toByte(), 0x55.toByte(), 0x21.toByte(), 0x0c.toByte(), 0x7d.toByte()
        )

        // Rcon для генерации ключей
        private val RCON = byteArrayOf(
            0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80.toByte(), 0x1b, 0x36
        )
    }

    private var key: ByteArray
    private val expandedKey: ByteArray

    constructor(keyString: String) {
        this.key = keyString.encodeToByteArray().take(KEY_SIZE).toByteArray()
        if (this.key.size < KEY_SIZE) {
            // Дополняем ключ нулями если он короче 16 байт
            val paddedKey = ByteArray(KEY_SIZE)
            this.key.copyInto(paddedKey)
            this.key = paddedKey
        }
        this.expandedKey = expandKey(this.key)
    }

    constructor(keyBytes: ByteArray) {
        this.key = keyBytes.take(KEY_SIZE).toByteArray()
        if (this.key.size < KEY_SIZE) {
            val paddedKey = ByteArray(KEY_SIZE)
            this.key.copyInto(paddedKey)
            this.key = paddedKey
        }
        this.expandedKey = expandKey(this.key)
    }

    /**
     * Шифрует строку и возвращает результат в Base64
     */
    fun encrypt(plaintext: String): String {
        val data = plaintext.encodeToByteArray()
        val encrypted = encryptBytes(data)
        return encodeBase64(encrypted)
    }

    /**
     * Расшифровывает строку из Base64
     */
    fun decrypt(encryptedBase64: String): String {
        val encrypted = decodeBase64(encryptedBase64)
        val decrypted = decryptBytes(encrypted)
        return decrypted.decodeToString()
    }

    /**
     * Шифрует массив байт
     */
    private fun encryptBytes(data: ByteArray): ByteArray {
        val iv = generateRandomIV()
        val paddedData = addPKCS7Padding(data)
        val encrypted = encryptCBC(paddedData, iv)

        // Возвращаем IV + зашифрованные данные
        return iv + encrypted
    }

    /**
     * Расшифровывает массив байт
     */
    private fun decryptBytes(data: ByteArray): ByteArray {
        if (data.size < BLOCK_SIZE) {
            throw IllegalArgumentException("Зашифрованные данные слишком короткие")
        }

        val iv = data.sliceArray(0 until BLOCK_SIZE)
        val encrypted = data.sliceArray(BLOCK_SIZE until data.size)

        val decrypted = decryptCBC(encrypted, iv)
        return removePKCS7Padding(decrypted)
    }

    /**
     * Шифрование в режиме CBC
     */
    private fun encryptCBC(data: ByteArray, iv: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        var previousBlock = iv

        for (i in data.indices step BLOCK_SIZE) {
            val block = data.sliceArray(i until i + BLOCK_SIZE)
            val xoredBlock = xorBlocks(block, previousBlock)
            val encryptedBlock = encryptBlock(xoredBlock)
            encryptedBlock.copyInto(result, i)
            previousBlock = encryptedBlock
        }

        return result
    }

    /**
     * Расшифровка в режиме CBC
     */
    private fun decryptCBC(data: ByteArray, iv: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        var previousBlock = iv

        for (i in data.indices step BLOCK_SIZE) {
            val block = data.sliceArray(i until i + BLOCK_SIZE)
            val decryptedBlock = decryptBlock(block)
            val xoredBlock = xorBlocks(decryptedBlock, previousBlock)
            xoredBlock.copyInto(result, i)
            previousBlock = block
        }

        return result
    }

    /**
     * Шифрование одного блока AES
     */
    private fun encryptBlock(block: ByteArray): ByteArray {
        val state = Array(4) { ByteArray(4) }

        // Копируем блок в состояние
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                state[j][i] = block[i * 4 + j]
            }
        }

        // Начальное добавление ключа
        addRoundKey(state, 0)

        // 9 раундов для AES-128
        for (round in 1 until 10) {
            subBytes(state)
            shiftRows(state)
            mixColumns(state)
            addRoundKey(state, round)
        }

        // Финальный раунд
        subBytes(state)
        shiftRows(state)
        addRoundKey(state, 10)

        // Конвертируем обратно в массив байт
        val result = ByteArray(BLOCK_SIZE)
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                result[i * 4 + j] = state[j][i]
            }
        }

        return result
    }

    /**
     * Расшифровка одного блока AES
     */
    private fun decryptBlock(block: ByteArray): ByteArray {
        val state = Array(4) { ByteArray(4) }

        // Копируем блок в состояние
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                state[j][i] = block[i * 4 + j]
            }
        }

        // Начальное добавление ключа
        addRoundKey(state, 10)

        // 9 раундов для AES-128 (в обратном порядке)
        for (round in 9 downTo 1) {
            invShiftRows(state)
            invSubBytes(state)
            addRoundKey(state, round)
            invMixColumns(state)
        }

        // Финальный раунд
        invShiftRows(state)
        invSubBytes(state)
        addRoundKey(state, 0)

        // Конвертируем обратно в массив байт
        val result = ByteArray(BLOCK_SIZE)
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                result[i * 4 + j] = state[j][i]
            }
        }

        return result
    }

    // AES операции
    private fun subBytes(state: Array<ByteArray>) {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                state[i][j] = SBOX[state[i][j].toInt() and 0xFF]
            }
        }
    }

    private fun invSubBytes(state: Array<ByteArray>) {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                state[i][j] = INV_SBOX[state[i][j].toInt() and 0xFF]
            }
        }
    }

    private fun shiftRows(state: Array<ByteArray>) {
        // Строка 1: сдвиг на 1 позицию влево
        val temp1 = state[1][0]
        state[1][0] = state[1][1]
        state[1][1] = state[1][2]
        state[1][2] = state[1][3]
        state[1][3] = temp1

        // Строка 2: сдвиг на 2 позиции влево
        val temp2a = state[2][0]
        val temp2b = state[2][1]
        state[2][0] = state[2][2]
        state[2][1] = state[2][3]
        state[2][2] = temp2a
        state[2][3] = temp2b

        // Строка 3: сдвиг на 3 позиции влево (или 1 вправо)
        val temp3 = state[3][3]
        state[3][3] = state[3][2]
        state[3][2] = state[3][1]
        state[3][1] = state[3][0]
        state[3][0] = temp3
    }

    private fun invShiftRows(state: Array<ByteArray>) {
        // Строка 1: сдвиг на 1 позицию вправо
        val temp1 = state[1][3]
        state[1][3] = state[1][2]
        state[1][2] = state[1][1]
        state[1][1] = state[1][0]
        state[1][0] = temp1

        // Строка 2: сдвиг на 2 позиции вправо
        val temp2a = state[2][2]
        val temp2b = state[2][3]
        state[2][2] = state[2][0]
        state[2][3] = state[2][1]
        state[2][0] = temp2a
        state[2][1] = temp2b

        // Строка 3: сдвиг на 3 позиции вправо (или 1 влево)
        val temp3 = state[3][0]
        state[3][0] = state[3][1]
        state[3][1] = state[3][2]
        state[3][2] = state[3][3]
        state[3][3] = temp3
    }

    private fun mixColumns(state: Array<ByteArray>) {
        for (c in 0 until 4) {
            val s0 = state[0][c]
            val s1 = state[1][c]
            val s2 = state[2][c]
            val s3 = state[3][c]

            // Convert Byte to Int when needed for XOR operations
            val s0Int = s0.toInt() and 0xFF
            val s1Int = s1.toInt() and 0xFF
            val s2Int = s2.toInt() and 0xFF
            val s3Int = s3.toInt() and 0xFF

            state[0][c] = (gmul(0x02, s0) xor gmul(0x03, s1) xor s2Int xor s3Int).toByte()
            state[1][c] = (s0Int xor gmul(0x02, s1) xor gmul(0x03, s2) xor s3Int).toByte()
            state[2][c] = (s0Int xor s1Int xor gmul(0x02, s2) xor gmul(0x03, s3)).toByte()
            state[3][c] = (gmul(0x03, s0) xor s1Int xor s2Int xor gmul(0x02, s3)).toByte()
        }
    }

    private fun invMixColumns(state: Array<ByteArray>) {
        for (c in 0 until 4) {
            val s0 = state[0][c]
            val s1 = state[1][c]
            val s2 = state[2][c]
            val s3 = state[3][c]

            state[0][c] = (gmul(0x0e, s0) xor gmul(0x0b, s1) xor gmul(0x0d, s2) xor gmul(0x09, s3)).toByte()
            state[1][c] = (gmul(0x09, s0) xor gmul(0x0e, s1) xor gmul(0x0b, s2) xor gmul(0x0d, s3)).toByte()
            state[2][c] = (gmul(0x0d, s0) xor gmul(0x09, s1) xor gmul(0x0e, s2) xor gmul(0x0b, s3)).toByte()
            state[3][c] = (gmul(0x0b, s0) xor gmul(0x0d, s1) xor gmul(0x09, s2) xor gmul(0x0e, s3)).toByte()
        }
    }

    private fun addRoundKey(state: Array<ByteArray>, round: Int) {
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                state[r][c] = (state[r][c] xor expandedKey[round * 16 + r * 4 + c]).toByte()
            }
        }
    }

    // Умножение в поле Галуа GF(2^8)
    private fun gmul(a: Int, b: Byte): Int {
        var p = 0
        var aByte = a
        var bByte = b.toInt() and 0xFF

        for (counter in 0 until 8) {
            if ((bByte and 1) != 0) {
                p = p xor aByte
            }
            val hiBitSet = (aByte and 0x80) != 0
            aByte = aByte shl 1
            if (hiBitSet) {
                aByte = aByte xor 0x1b
            }
            bByte = bByte shr 1
        }
        return p and 0xFF
    }

    /**
     * Расширение ключа для AES-128
     */
    private fun expandKey(key: ByteArray): ByteArray {
        val expandedKey = ByteArray(176) // 11 раундов * 16 байт

        // Первые 16 байт - это исходный ключ
        key.copyInto(expandedKey, 0, 0, 16)

        var i = 16
        while (i < 176) {
            val temp = expandedKey.sliceArray(i - 4 until i)

            if (i % 16 == 0) {
                // RotWord
                val rotated = byteArrayOf(temp[1], temp[2], temp[3], temp[0])

                // SubWord
                for (j in rotated.indices) {
                    rotated[j] = SBOX[rotated[j].toInt() and 0xFF]
                }

                // XOR с Rcon
                rotated[0] = (rotated[0] xor RCON[i / 16 - 1]).toByte()

                for (j in 0 until 4) {
                    expandedKey[i + j] = (expandedKey[i + j - 16] xor rotated[j]).toByte()
                }
            } else {
                for (j in 0 until 4) {
                    expandedKey[i + j] = (expandedKey[i + j - 16] xor temp[j]).toByte()
                }
            }
            i += 4
        }

        return expandedKey
    }

    /**
     * Добавляет PKCS7 padding
     */
    private fun addPKCS7Padding(data: ByteArray): ByteArray {
        val paddingLength = BLOCK_SIZE - (data.size % BLOCK_SIZE)
        val paddedData = ByteArray(data.size + paddingLength)
        data.copyInto(paddedData)

        for (i in data.size until paddedData.size) {
            paddedData[i] = paddingLength.toByte()
        }

        return paddedData
    }

    /**
     * Удаляет PKCS7 padding
     */
    private fun removePKCS7Padding(data: ByteArray): ByteArray {
        if (data.isEmpty()) {
            throw IllegalArgumentException("Данные не могут быть пустыми")
        }

        val paddingLength = data.last().toInt() and 0xFF
        if (paddingLength > BLOCK_SIZE || paddingLength > data.size) {
            throw IllegalArgumentException("Неверный padding")
        }

        // Проверяем корректность padding
        for (i in data.size - paddingLength until data.size) {
            if ((data[i].toInt() and 0xFF) != paddingLength) {
                throw IllegalArgumentException("Неверный padding")
            }
        }

        return data.sliceArray(0 until data.size - paddingLength)
    }

    /**
     * XOR двух блоков
     */
    private fun xorBlocks(a: ByteArray, b: ByteArray): ByteArray {
        val result = ByteArray(a.size)
        for (i in a.indices) {
            result[i] = (a[i] xor b[i]).toByte()
        }
        return result
    }

    /**
     * Генерирует случайный IV
     */
    private fun generateRandomIV(): ByteArray {
        return Random.nextBytes(BLOCK_SIZE)
    }

    /**
     * Кодирует в Base64 (простая реализация)
     */
    private fun encodeBase64(data: ByteArray): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val result = StringBuilder()

        var i = 0
        while (i < data.size) {
            val b1 = data[i].toInt() and 0xFF
            val b2 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            val b3 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0

            val bitmap = (b1 shl 16) or (b2 shl 8) or b3

            result.append(chars[(bitmap shr 18) and 63])
            result.append(chars[(bitmap shr 12) and 63])
            result.append(if (i + 1 < data.size) chars[(bitmap shr 6) and 63] else '=')
            result.append(if (i + 2 < data.size) chars[bitmap and 63] else '=')

            i += 3
        }

        return result.toString()
    }

    /**
     * Декодирует из Base64 (простая реализация)
     */
    private fun decodeBase64(data: String): ByteArray {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val cleanData = data.replace("=", "")
        val result = mutableListOf<Byte>()

        var i = 0
        while (i < cleanData.length) {
            val c1 = chars.indexOf(cleanData[i])
            val c2 = if (i + 1 < cleanData.length) chars.indexOf(cleanData[i + 1]) else 0
            val c3 = if (i + 2 < cleanData.length) chars.indexOf(cleanData[i + 2]) else 0
            val c4 = if (i + 3 < cleanData.length) chars.indexOf(cleanData[i + 3]) else 0

            val bitmap = (c1 shl 18) or (c2 shl 12) or (c3 shl 6) or c4

            result.add(((bitmap shr 16) and 0xFF).toByte())
            if (i + 2 < cleanData.length) {
                result.add(((bitmap shr 8) and 0xFF).toByte())
            }
            if (i + 3 < cleanData.length) {
                result.add((bitmap and 0xFF).toByte())
            }

            i += 4
        }

        return result.toByteArray()
    }
}