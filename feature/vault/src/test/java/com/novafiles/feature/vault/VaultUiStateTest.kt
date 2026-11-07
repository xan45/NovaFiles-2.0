package com.novafiles.feature.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VaultUiStateTest {

    @Test
    fun testVaultItemProperties() {
        val tempFile = File.createTempFile("confidential_doc", ".nova")
        tempFile.writeBytes(ByteArray(2048))

        val item = VaultItem(file = tempFile)
        assertTrue(item.originalName.startsWith("confidential_doc"))
        assertEquals(tempFile.name.removeSuffix(".nova"), item.originalName)
        assertEquals(2048L, item.sizeBytes)
        assertEquals(tempFile.absolutePath, item.id)

        tempFile.delete()
    }

    @Test
    fun testVaultStateTransitions() {
        val initial = VaultUiState()
        assertEquals(VaultState.LOCKED, initial.state)
        assertEquals("", initial.enteredPin)
        assertTrue(initial.vaultItems.isEmpty())
        assertFalse(initial.isBiometricEnabled)

        val unlocked = initial.copy(state = VaultState.UNLOCKED)
        assertEquals(VaultState.UNLOCKED, unlocked.state)

        val decoy = initial.copy(state = VaultState.DECOY_UNLOCKED)
        assertEquals(VaultState.DECOY_UNLOCKED, decoy.state)

        val lockedOut = initial.copy(state = VaultState.LOCKED_OUT, lockoutSeconds = 30)
        assertEquals(VaultState.LOCKED_OUT, lockedOut.state)
        assertEquals(30L, lockedOut.lockoutSeconds)
    }
}
