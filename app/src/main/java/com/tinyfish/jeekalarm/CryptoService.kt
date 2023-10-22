package com.tinyfish.jeekalarm

import android.util.Base64
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.ShortBufferException
import javax.crypto.spec.SecretKeySpec

class CryptoService {
    companion object {
        private const val aesKey = "7b94fe97-b901-43"
        private var secretKey: SecretKeySpec? = null
        private val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding")

        init {
            try {
                val keyBytes = aesKey.toByteArray(charset("UTF8"))
                secretKey = SecretKeySpec(keyBytes, "AES")
            } catch (uee: UnsupportedEncodingException) {
                uee.printStackTrace()
            }
        }

        fun encrypt(plainText: String): String {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)

                val input = plainText.toByteArray(Charsets.UTF_8)
                val cipherText = ByteArray(cipher.getOutputSize(input.size))
                var ctLength = cipher.update(
                    input, 0, input.size,
                    cipherText, 0
                )
                ctLength += cipher.doFinal(cipherText, ctLength)

                return Base64.encodeToString(cipherText, Base64.DEFAULT)
            } catch (ex: UnsupportedEncodingException) {
                ex.printStackTrace()
            } catch (ex: IllegalBlockSizeException) {
                ex.printStackTrace()
            } catch (ex: BadPaddingException) {
                ex.printStackTrace()
            } catch (ex: InvalidKeyException) {
                ex.printStackTrace()
            } catch (ex: NoSuchPaddingException) {
                ex.printStackTrace()
            } catch (ex: NoSuchAlgorithmException) {
                ex.printStackTrace()
            } catch (ex: ShortBufferException) {
                ex.printStackTrace()
            }

            return ""
        }

        fun decrypt(encryptedText: String): String {
            try {
                cipher.init(Cipher.DECRYPT_MODE, secretKey)

                val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
                val decryptedBytes = ByteArray(cipher.getOutputSize(encryptedBytes.size))
                var ptLength = cipher.update(encryptedBytes, 0, encryptedBytes.size, decryptedBytes, 0)
                ptLength += cipher.doFinal(decryptedBytes, ptLength)

                return String(decryptedBytes, 0, ptLength)
            } catch (ex: UnsupportedEncodingException) {
                ex.printStackTrace()
            } catch (ex: IllegalBlockSizeException) {
                ex.printStackTrace()
            } catch (ex: BadPaddingException) {
                ex.printStackTrace()
            } catch (ex: InvalidKeyException) {
                ex.printStackTrace()
            } catch (ex: NoSuchPaddingException) {
                ex.printStackTrace()
            } catch (ex: NoSuchAlgorithmException) {
                ex.printStackTrace()
            } catch (e: ShortBufferException) {
                e.printStackTrace()
            }

            return ""
        }
    }
}