package com.novafiles.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class FakeKeyProvider(private val secretKey: SecretKey) : VaultKeyProvider {
    override fun hasMasterKey(): Boolean = true
    override fun getMasterKey(): SecretKey = secretKey
    override fun initMasterKey(): SecretKey = secretKey
    override fun getHardwareSecurityLevel(): HardwareSecurityLevel = HardwareSecurityLevel.SOFTWARE
}

class VaultCryptoEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testKey: SecretKey

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        testKey = keyGen.generateKey()
        VaultCryptoEngine.keyProvider = FakeKeyProvider(testKey)
    }

    @Test
    fun testEncryptionAndDecryptionRoundtrip() {
        val originalFile = tempFolder.newFile("confidential.txt")
        val content = "Highly confidential data: NovaFiles Vault 2.0"
        originalFile.writeText(content)

        val vaultFile = File(tempFolder.root, "confidential.txt.nova")
        VaultCryptoEngine.encryptFile(originalFile, vaultFile)

        assertTrue(vaultFile.exists())
        assertFalse(originalFile.exists()) // Original must be deleted/wiped

        // Verify magic header
        val headerBytes = ByteArray(VaultCryptoEngine.MAGIC_HEADER.size)
        vaultFile.inputStream().use { it.read(headerBytes) }
        assertArrayEquals(VaultCryptoEngine.MAGIC_HEADER, headerBytes)

        // Decrypt
        val decryptedFile = File(tempFolder.root, "restored.txt")
        VaultCryptoEngine.decryptFile(vaultFile, decryptedFile)

        assertTrue(decryptedFile.exists())
        assertEquals(content, decryptedFile.readText())
    }

    @Test(expected = VaultSecurityException::class)
    fun testCorruptedFileThrowsException() {
        val corruptFile = tempFolder.newFile("corrupt.nova")
        corruptFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04))

        val dest = File(tempFolder.root, "failed.txt")
        VaultCryptoEngine.decryptFile(corruptFile, dest)
    }

    @Test
    fun testWipeAndSecureDelete() {
        val file = tempFolder.newFile("to_wipe.txt")
        file.writeText("Sensitive text to overwrite")
        assertTrue(file.exists())

        VaultCryptoEngine.wipeAndSecureDelete(file)
        assertFalse(file.exists())
    }
}
