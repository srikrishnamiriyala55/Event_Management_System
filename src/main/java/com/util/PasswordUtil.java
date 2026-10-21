package com.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private static final int BCRYPT_COST = 12;

    private PasswordUtil() {
    }

    public static String hash(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }

        return BCrypt.hashpw(
                plainTextPassword,
                BCrypt.gensalt(BCRYPT_COST));
    }

    public static boolean verify(String plainTextPassword, String storedHash) {
        if (plainTextPassword == null
                || storedHash == null
                || !storedHash.startsWith("$2")) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainTextPassword, storedHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
