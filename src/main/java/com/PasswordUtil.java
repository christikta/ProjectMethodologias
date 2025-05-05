package com;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Hash a password
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean checkPassword(String password, String storedHash) {
        if (storedHash == null || storedHash.isEmpty() || !storedHash.startsWith("$2")) {
            System.err.println("Invalid bcrypt hash format.");
            return false;
        }

        return BCrypt.checkpw(password, storedHash);
    }
}
