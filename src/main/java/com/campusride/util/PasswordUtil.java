package com.campusride.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// Password hashing with PBKDF2 (it is built into Java, no extra library).
// We never store the real password, only the hash and the salt.
public class PasswordUtil {

    // running the hash 65536 times makes guessing passwords very slow for an attacker
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;   // bits

    private static SecureRandom random = new SecureRandom();

    // A new random salt for every user, so two people with the same
    // password still get different hashes.
    public static String newSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(char[] password, String salt) {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(password, saltBytes, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hashBytes = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // every normal Java 17 has PBKDF2, so this should never happen
            throw new RuntimeException("PBKDF2 is not available", e);
        }
    }

    // At login: hash the typed password with the user's saved salt and compare.
    public static boolean verify(char[] password, String salt, String savedHash) {
        String newHash = hash(password, salt);
        // MessageDigest.isEqual takes the same time even when the first characters differ,
        // so nobody can guess the hash from how long the check takes
        return MessageDigest.isEqual(newHash.getBytes(), savedHash.getBytes());
    }

    // We used this to make the password hashes in sql/02_sample_data.sql
    // Run:  java com.campusride.util.PasswordUtil Admin@2026
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java com.campusride.util.PasswordUtil <password>");
            return;
        }
        for (String password : args) {
            String salt = newSalt();
            System.out.println(password + "  salt=" + salt + "  hash=" + hash(password.toCharArray(), salt));
        }
    }
}
