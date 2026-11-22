package com.tinyfish.jeekalarm

import android.util.Base64
import android.util.Log
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoService {
    private const val aesKey = "7b94fe97-b901-43"
    private const val transformation = "AES/ECB/PKCS7Padding"
    private val secretKey = SecretKeySpec(aesKey.toByteArray(Charsets.UTF_8), "AES")

    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(cipherText, Base64.NO_WRAP)
        } catch (ex: GeneralSecurityException) {
            Log.e(javaClass.name, "encrypt failed", ex)
            ""
        }
    }

    fun decrypt(encryptedText: String): String {
        return try {
            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (ex: GeneralSecurityException) {
            Log.e(javaClass.name, "decrypt failed", ex)
            ""
        } catch (ex: IllegalArgumentException) {
            Log.e(javaClass.name, "decrypt failed: invalid base64", ex)
            ""
        }
    }
}
