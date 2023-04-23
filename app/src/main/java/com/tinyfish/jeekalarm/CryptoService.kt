package com.tinyfish.jeekalarm

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CryptoService {
    companion object {
        private const val alias = "7b94fe97-b901-4341-9dc3-beb0f900d8d9"
        private var keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")

        init {
            keyStore.load(null)
            generateKey()
        }

        private fun generateKey() {
            if (keyStore.containsAlias(alias))
                return

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(false).build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }

        fun encrypt(plainText: String): String {
            val secretKey = keyStore.getKey(alias, null) as SecretKey
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val ivBytes = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charset.forName("UTF-8")))

            val encryptedData = ByteArray(ivBytes.size + encryptedBytes.size)
            System.arraycopy(ivBytes, 0, encryptedData, 0, ivBytes.size)
            System.arraycopy(encryptedBytes, 0, encryptedData, ivBytes.size, encryptedBytes.size)

            return Base64.encodeToString(encryptedData, Base64.DEFAULT)
        }

        fun decrypt(encryptedText: String): String {
            try {
                val secretKey = keyStore.getKey(alias, null) as SecretKey
                val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

                val encryptedData = Base64.decode(encryptedText, Base64.DEFAULT)

                val ivBytes = encryptedData.copyOfRange(0, 16)
                val encryptedBytes = encryptedData.copyOfRange(16, encryptedData.size)

                val ivParameterSpec = IvParameterSpec(ivBytes)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

                val decryptedBytes = cipher.doFinal(encryptedBytes)
                return String(decryptedBytes, Charset.forName("UTF-8"))
            } catch (ex: Exception) {
                return ""
            }
        }
    }
}