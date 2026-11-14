package com.novafiles.core.security

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinSecurityManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var securityManager: PinSecurityManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        securityManager = PinSecurityManager(prefs = fakePrefs)
    }

    @Test
    fun testInitialStateHasNoPin() {
        assertFalse(securityManager.hasPin)
        assertFalse(securityManager.hasDecoyPin)
        assertEquals(0L, securityManager.getRemainingLockoutSeconds())
    }

    @Test
    fun testSetAndVerifyPinSuccess() {
        securityManager.setPin("1234")
        assertTrue(securityManager.hasPin)

        val result = securityManager.verifyPin("1234")
        assertTrue(result is PinVerificationResult.Success)
    }

    @Test
    fun testVerifyInvalidPin() {
        securityManager.setPin("1234")

        val result = securityManager.verifyPin("9999")
        assertTrue(result is PinVerificationResult.Invalid)
        assertEquals(4, (result as PinVerificationResult.Invalid).attemptsRemaining)
    }

    @Test
    fun testDecoyPinSuccess() {
        securityManager.setPin("1234")
        securityManager.setDecoyPin("5678")

        assertTrue(securityManager.hasPin)
        assertTrue(securityManager.hasDecoyPin)

        val result = securityManager.verifyPin("5678")
        assertTrue(result is PinVerificationResult.DecoySuccess)
    }

    @Test
    fun testLockoutAfterMaxAttempts() {
        securityManager.setPin("1234")

        repeat(4) {
            val res = securityManager.verifyPin("0000")
            assertTrue(res is PinVerificationResult.Invalid)
        }

        // 5th failed attempt triggers lockout
        val lockoutResult = securityManager.verifyPin("0000")
        assertTrue(lockoutResult is PinVerificationResult.LockedOut)
        val lockedOut = lockoutResult as PinVerificationResult.LockedOut
        assertTrue(lockedOut.secondsRemaining > 0)
        assertTrue(securityManager.getRemainingLockoutSeconds() > 0)
    }

    @Test
    fun testRemovePin() {
        securityManager.setPin("1234")
        securityManager.setDecoyPin("5678")
        assertTrue(securityManager.hasPin)

        securityManager.removePin()
        assertFalse(securityManager.hasPin)
        assertFalse(securityManager.hasDecoyPin)
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String> ?: defValues)
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(data)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val data: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val removes = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { key?.let { temp[it] = value } }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { key?.let { temp[it] = values } }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { key?.let { temp[it] = value } }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { key?.let { temp[it] = value } }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { key?.let { temp[it] = value } }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { key?.let { temp[it] = value } }
            override fun remove(key: String?): SharedPreferences.Editor = apply { key?.let { removes.add(it) } }
            override fun clear(): SharedPreferences.Editor = apply { clearAll = true }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearAll) data.clear()
                removes.forEach { data.remove(it) }
                data.putAll(temp)
            }
        }
    }
}
