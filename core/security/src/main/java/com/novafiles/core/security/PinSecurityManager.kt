package com.novafiles.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data object DecoySuccess : PinVerificationResult
    data class Invalid(val attemptsRemaining: Int) : PinVerificationResult
    data class LockedOut(val secondsRemaining: Long) : PinVerificationResult
}

class PinSecurityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("nova_security_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_PBKDF2_HASH = "vault_pbkdf2_hash"
        const val KEY_SALT = "vault_salt"
        const val KEY_DECOY_HASH = "vault_decoy_hash"
        const val KEY_DECOY_SALT = "vault_decoy_salt"
        const val KEY_FAILED_ATTEMPTS = "vault_failed_attempts"
        const val KEY_LOCKOUT_UNTIL = "vault_lockout_until"
        const val KEY_LAST_CHECK_TIME = "vault_last_check_time"
        const val KEY_BIOMETRIC_ENABLED = "vault_biometric_enabled"

        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
        private const val MAX_ATTEMPTS = 5
        private const val BASE_LOCKOUT_MS = 30_000L
    }

    private val secureRandom = SecureRandom()

    val hasPin: Boolean
        get() = prefs.getString(KEY_PBKDF2_HASH, null) != null

    val hasDecoyPin: Boolean
        get() = prefs.getString(KEY_DECOY_HASH, null) != null

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    fun getRemainingLockoutSeconds(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)

        // Clock rollback detection
        if (lockoutUntil > 0L && lastCheck > 0L && now < (lastCheck - 5_000L)) {
            val updatedLockout = now + BASE_LOCKOUT_MS
            prefs.edit()
                .putLong(KEY_LOCKOUT_UNTIL, updatedLockout)
                .putLong(KEY_LAST_CHECK_TIME, now)
                .apply()
            return (BASE_LOCKOUT_MS + 999) / 1000
        }

        prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()

        return if (lockoutUntil > now) {
            (lockoutUntil - now + 999) / 1000
        } else {
            0L
        }
    }

    fun setPin(pin: CharArray) {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val hash = hashPbkdf2(pin, salt, ITERATIONS, KEY_LENGTH)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PBKDF2_HASH, hashBase64)
            .putString(KEY_SALT, saltBase64)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            .apply()

        pin.fill('0')
    }

    fun setDecoyPin(pin: CharArray) {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val hash = hashPbkdf2(pin, salt, ITERATIONS, KEY_LENGTH)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_DECOY_HASH, hashBase64)
            .putString(KEY_DECOY_SALT, saltBase64)
            .apply()

        pin.fill('0')
    }

    fun verifyPin(pin: CharArray): PinVerificationResult {
        val lockoutSeconds = getRemainingLockoutSeconds()
        if (lockoutSeconds > 0) {
            pin.fill('0')
            return PinVerificationResult.LockedOut(lockoutSeconds)
        }

        val storedHashBase64 = prefs.getString(KEY_PBKDF2_HASH, null)
        val storedSaltBase64 = prefs.getString(KEY_SALT, null)

        if (storedHashBase64 == null || storedSaltBase64 == null) {
            pin.fill('0')
            return PinVerificationResult.Invalid(MAX_ATTEMPTS)
        }

        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP)
        val computedHash = hashPbkdf2(pin, salt, ITERATIONS, KEY_LENGTH)

        // 1. Check Primary PIN
        val isPrimaryMatch = MessageDigest.isEqual(expectedHash, computedHash)

        // 2. Check Decoy PIN
        var isDecoyMatch = false
        val storedDecoyHashBase64 = prefs.getString(KEY_DECOY_HASH, null)
        val storedDecoySaltBase64 = prefs.getString(KEY_DECOY_SALT, null)
        if (storedDecoyHashBase64 != null && storedDecoySaltBase64 != null) {
            val decoySalt = Base64.decode(storedDecoySaltBase64, Base64.NO_WRAP)
            val expectedDecoyHash = Base64.decode(storedDecoyHashBase64, Base64.NO_WRAP)
            val computedDecoyHash = hashPbkdf2(pin, decoySalt, ITERATIONS, KEY_LENGTH)
            isDecoyMatch = MessageDigest.isEqual(expectedDecoyHash, computedDecoyHash)
        }

        pin.fill('0')

        if (isPrimaryMatch) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            return PinVerificationResult.Success
        }

        if (isDecoyMatch) {
            // Decoy success acts like a normal unlock to fool observers
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            return PinVerificationResult.DecoySuccess
        }

        // Failed attempt handling
        val failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val attemptsRemaining = (MAX_ATTEMPTS - failedAttempts).coerceAtLeast(0)

        if (failedAttempts >= MAX_ATTEMPTS) {
            val multiplier = (failedAttempts - MAX_ATTEMPTS + 1).coerceAtMost(5)
            val lockoutMs = BASE_LOCKOUT_MS * multiplier
            val lockoutUntil = System.currentTimeMillis() + lockoutMs

            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, failedAttempts)
                .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                .apply()

            return PinVerificationResult.LockedOut(lockoutMs / 1000)
        } else {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, failedAttempts)
                .apply()

            return PinVerificationResult.Invalid(attemptsRemaining)
        }
    }

    fun removePin() {
        prefs.edit()
            .remove(KEY_PBKDF2_HASH)
            .remove(KEY_SALT)
            .remove(KEY_DECOY_HASH)
            .remove(KEY_DECOY_SALT)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .remove(KEY_BIOMETRIC_ENABLED)
            .apply()
    }

    private fun hashPbkdf2(pin: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
