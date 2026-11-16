package com.findit.campus.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 12_000
    private const val KEY_BITS = 256

    fun salt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(encoded)
    }

    fun verify(password: String, saltBase64: String, expectedHash: String): Boolean {
        val actual = hash(password, Base64.getDecoder().decode(saltBase64))
        return MessageDigest.isEqual(actual.toByteArray(Charsets.UTF_8), expectedHash.toByteArray(Charsets.UTF_8))
    }
}

fun sealSecret(secret: String): String {
    val salt = PasswordHasher.salt()
    val hash = PasswordHasher.hash(secret.trim().lowercase(), salt)
    return Base64.getEncoder().encodeToString(salt) + ":" + hash
}

fun secretMatches(secret: String, sealed: String?): Boolean {
    if (sealed.isNullOrBlank()) return false
    val parts = sealed.split(":", limit = 2)
    if (parts.size != 2) return false
    return PasswordHasher.verify(secret.trim().lowercase(), parts[0], parts[1])
}

fun validateNewAccount(
    name: String,
    email: String,
    password: String,
    confirm: String,
    campus: String,
): String? {
    if (name.trim().length < 2) return "Enter your name."
    if (!isValidEmail(email)) return "Enter a valid email address."
    if (password.length < 8) return "Use at least 8 characters."
    if (password != confirm) return "Passwords do not match."
    if (campus.trim().length < 2) return "Enter your campus name."
    return null
}

fun isValidEmail(email: String): Boolean =
    email.trim().matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
