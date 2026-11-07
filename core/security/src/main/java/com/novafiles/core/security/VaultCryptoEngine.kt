package com.novafiles.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.withLock

enum class HardwareSecurityLevel {
    STRONGBOX,
    TEE,
    SOFTWARE,
    UNAVAILABLE
}

open class VaultSecurityException(message: String, cause: Throwable? = null) : SecurityException(message, cause)
class VaultKeyNotFoundException(message: String) : VaultSecurityException(message)
class VaultAuthenticationException(message: String, cause: Throwable? = null) : VaultSecurityException(message, cause)
class VaultCorruptFileException(message: String, cause: Throwable? = null) : VaultSecurityException(message, cause)

interface VaultKeyProvider {
    fun hasMasterKey(): Boolean
    fun getMasterKey(): SecretKey
    fun initMasterKey(): SecretKey
    fun getHardwareSecurityLevel(): HardwareSecurityLevel
}

class AndroidKeyStoreProvider : VaultKeyProvider {
    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "NovaFiles_Vault_Master_Key_v2"
    }

    override fun hasMasterKey(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.containsAlias(KEY_ALIAS)
        } catch (_: Exception) {
            false
        }
    }

    override fun getMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            throw VaultKeyNotFoundException(
                "Vault master key '$KEY_ALIAS' was not found in Android Keystore."
            )
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw VaultKeyNotFoundException("Key entry '$KEY_ALIAS' is invalid.")
        return entry.secretKey
    }

    override fun initMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun getHardwareSecurityLevel(): HardwareSecurityLevel {
        return try {
            val masterKey = getMasterKey()
            val factory = SecretKeyFactory.getInstance(masterKey.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(masterKey, KeyInfo::class.java) as KeyInfo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (keyInfo.securityLevel) {
                    KeyProperties.SECURITY_LEVEL_STRONGBOX -> HardwareSecurityLevel.STRONGBOX
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> HardwareSecurityLevel.TEE
                    else -> HardwareSecurityLevel.SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                if (keyInfo.isInsideSecureHardware) HardwareSecurityLevel.TEE else HardwareSecurityLevel.SOFTWARE
            }
        } catch (_: Exception) {
            HardwareSecurityLevel.SOFTWARE
        }
    }
}

object VaultCryptoEngine {

    private const val GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    // Versioned Header: ASCII 'NOVAV1'
    val MAGIC_HEADER = byteArrayOf(0x4E, 0x4F, 0x56, 0x41, 0x56, 0x31)

    private val secureRandom = SecureRandom()

    var keyProvider: VaultKeyProvider = AndroidKeyStoreProvider()
        @VisibleForTesting set

    private val fileLocks = ConcurrentHashMap<String, ReentrantLock>()

    private fun getLockFor(file: File): ReentrantLock =
        fileLocks.computeIfAbsent(file.canonicalPath) { ReentrantLock() }

    fun hasMasterKey(): Boolean = keyProvider.hasMasterKey()
    fun initMasterKey(): SecretKey = keyProvider.initMasterKey()
    fun getHardwareSecurityLevel(): HardwareSecurityLevel = keyProvider.getHardwareSecurityLevel()

    fun wipeAndSecureDelete(file: File) {
        if (!file.exists()) return
        try {
            if (file.isFile && file.length() > 0) {
                val len = file.length().coerceAtMost(10 * 1024 * 1024)
                FileOutputStream(file).use { fos ->
                    val zeros = ByteArray(8192)
                    var written = 0L
                    while (written < len) {
                        val toWrite = minOf(zeros.size.toLong(), len - written).toInt()
                        fos.write(zeros, 0, toWrite)
                        written += toWrite
                    }
                    fos.flush()
                    fos.fd.sync()
                }
            }
        } catch (_: Exception) {
        } finally {
            file.delete()
        }
    }

    @Throws(IOException::class, VaultSecurityException::class)
    fun encryptFile(sourceFile: File, destinationVaultFile: File) {
        if (!sourceFile.exists()) {
            throw IOException("Source file does not exist: ${sourceFile.absolutePath}")
        }

        val masterKey = if (hasMasterKey()) keyProvider.getMasterKey() else initMasterKey()
        val tempEncrypted = File(destinationVaultFile.parentFile, "${destinationVaultFile.name}.tmp")
        tempEncrypted.parentFile?.mkdirs()

        getLockFor(sourceFile).withLock {
            getLockFor(destinationVaultFile).withLock {
                try {
                    val iv = ByteArray(GCM_IV_LENGTH_BYTES)
                    secureRandom.nextBytes(iv)

                    val cipher = Cipher.getInstance(GCM_TRANSFORMATION)
                    cipher.init(Cipher.ENCRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

                    FileOutputStream(tempEncrypted).use { fos ->
                        fos.write(MAGIC_HEADER)
                        fos.write(iv)

                        FileInputStream(sourceFile).use { fis ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (fis.read(buffer).also { bytesRead = it } != -1) {
                                val output = cipher.update(buffer, 0, bytesRead)
                                if (output != null && output.isNotEmpty()) {
                                    fos.write(output)
                                }
                            }
                            val finalBytes = cipher.doFinal()
                            if (finalBytes != null && finalBytes.isNotEmpty()) {
                                fos.write(finalBytes)
                            }
                        }
                        fos.flush()
                        fos.fd.sync()
                    }

                    // Move temp file to final destination
                    if (destinationVaultFile.exists()) destinationVaultFile.delete()
                    if (!tempEncrypted.renameTo(destinationVaultFile)) {
                        tempEncrypted.copyTo(destinationVaultFile, overwrite = true)
                        tempEncrypted.delete()
                    }

                    // Securely wipe original file
                    wipeAndSecureDelete(sourceFile)
                } catch (e: Exception) {
                    wipeAndSecureDelete(tempEncrypted)
                    throw VaultSecurityException("Failed to encrypt file: ${sourceFile.name}", e)
                }
            }
        }
    }

    @Throws(IOException::class, VaultSecurityException::class)
    fun decryptFile(vaultFile: File, destinationPlaintextFile: File) {
        if (!vaultFile.exists()) {
            throw IOException("Vault file does not exist: ${vaultFile.absolutePath}")
        }

        val masterKey = keyProvider.getMasterKey()
        val tempDecrypted = File(destinationPlaintextFile.parentFile, "${destinationPlaintextFile.name}.tmp")
        tempDecrypted.parentFile?.mkdirs()

        getLockFor(vaultFile).withLock {
            getLockFor(destinationPlaintextFile).withLock {
                try {
                    FileInputStream(vaultFile).use { fis ->
                        // 1. Read and verify Magic Header
                        val header = ByteArray(MAGIC_HEADER.size)
                        val headerRead = fis.read(header)
                        if (headerRead != MAGIC_HEADER.size || !header.contentEquals(MAGIC_HEADER)) {
                            throw VaultCorruptFileException("File is not a valid NovaFiles encrypted file: ${vaultFile.name}")
                        }

                        // 2. Read IV
                        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
                        val ivRead = fis.read(iv)
                        if (ivRead != GCM_IV_LENGTH_BYTES) {
                            throw VaultCorruptFileException("Corrupt IV in encrypted file: ${vaultFile.name}")
                        }

                        val cipher = Cipher.getInstance(GCM_TRANSFORMATION)
                        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

                        FileOutputStream(tempDecrypted).use { fos ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (fis.read(buffer).also { bytesRead = it } != -1) {
                                val output = cipher.update(buffer, 0, bytesRead)
                                if (output != null && output.isNotEmpty()) {
                                    fos.write(output)
                                }
                            }
                            val finalBytes = cipher.doFinal()
                            if (finalBytes != null && finalBytes.isNotEmpty()) {
                                fos.write(finalBytes)
                            }
                            fos.flush()
                            fos.fd.sync()
                        }
                    }

                    // Move temp file to final destination
                    if (destinationPlaintextFile.exists()) destinationPlaintextFile.delete()
                    if (!tempDecrypted.renameTo(destinationPlaintextFile)) {
                        tempDecrypted.copyTo(destinationPlaintextFile, overwrite = true)
                        tempDecrypted.delete()
                    }
                } catch (e: Exception) {
                    wipeAndSecureDelete(tempDecrypted)
                    throw VaultAuthenticationException("Failed to decrypt or authenticate file: ${vaultFile.name}", e)
                }
            }
        }
    }
}
