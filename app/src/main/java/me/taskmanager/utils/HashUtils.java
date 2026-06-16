package me.taskmanager.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility class for password hashing using SHA-256 + Salt (Decision 003)
 */
public class HashUtils {

    private HashUtils() {}

    /**
     * Generates a cryptographically secure random salt (16 bytes, Base64-encoded)
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP);
    }

    /**
     * Hashes a password with the given salt using SHA-256.
     *
     * @param password plain-text password
     * @param salt     Base64-encoded salt string
     * @return Base64-encoded hash, or null on error
     */
    public static String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.decode(salt, Base64.NO_WRAP);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltBytes);
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Verifies a plain-text password against a stored hash and salt.
     *
     * @param password   plain-text password to verify
     * @param storedHash Base64-encoded hash stored in database
     * @param salt       Base64-encoded salt stored in database
     * @return true if the password matches
     */
    public static boolean verifyPassword(String password, String storedHash, String salt) {
        String computedHash = hashPassword(password, salt);
        return computedHash != null && computedHash.equals(storedHash);
    }
}
