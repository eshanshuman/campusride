package com.campusride.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with PBKDF2 (built into Java, no extra library).
 *
 * How it works:
 *  1. When a user registers we create a random 16-byte "salt".
 *  2. PBKDF2 mixes password + salt 65,536 times with HMAC-SHA256
 *     and gives a 256-bit hash. Doing it many times makes guessing slow.
 *  3. We store hash and salt (both as Base64 text) - never the password.
 *  4. At login we hash the typed password with the SAME salt and compare.
 *
 * The salt means two users with the same password get different hashes.
 */
public final class PasswordUtil {

    private static final String ALGORITHM  = "PBKDF2WithHmacSHA256";
    private static final int    ITERATIONS = 65536;
    private static final int    KEY_BITS   = 256;
    private static final int    SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }   // utility class: no objects needed

    /** Creates a new random salt, returned as Base64 text (24 characters). */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Hashes a password with the given Base64 salt; returns Base64 text (44 characters). */
    public static String hash(char[] password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Cannot happen on a normal Java 17 install, so we stop loudly.
            throw new IllegalStateException("PBKDF2 is not available", e);
        }
    }

    /** True if the typed password produces the stored hash. */
    public static boolean verify(char[] password, String saltBase64, String expectedHash) {
        String actual = hash(password, saltBase64);
        // MessageDigest.isEqual compares in constant time (does not leak timing)
        return java.security.MessageDigest.isEqual(actual.getBytes(), expectedHash.getBytes());
    }

    /**
     * Small helper used to create the hashes in sql/02_sample_data.sql.
     * Usage:  java com.campusride.util.PasswordUtil  MyPassword1
     * Prints a salt and hash you can paste into an INSERT statement.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java com.campusride.util.PasswordUtil <password> [more passwords]");
            return;
        }
        for (String pwd : args) {
            String salt = newSalt();
            System.out.println(pwd + "  salt=" + salt + "  hash=" + hash(pwd.toCharArray(), salt));
        }
    }
}
