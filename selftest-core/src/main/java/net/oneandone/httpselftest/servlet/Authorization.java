package net.oneandone.httpselftest.servlet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

public final class Authorization {

    private Authorization() {
    }

    public static boolean isOk(HttpServletRequest request, Optional<String> authConfiguration) {
        if (!authConfiguration.isPresent()) {
            return true;
        }

        String headerValue = request.getHeader("Authorization");
        if (headerValue == null || !headerValue.startsWith("Basic ")) {
            return false;
        }

        return isHeaderOk(headerValue, authConfiguration.get());
    }

    private static boolean isHeaderOk(String headerValue, String configured) {
        try {
            String userCredentialsB64 = headerValue.substring("Basic ".length()).trim();
            byte[] bytes = Base64.getMimeDecoder().decode(userCredentialsB64);
            String userCredentials = new String(bytes, StandardCharsets.UTF_8);

            return isSecretOk(userCredentials, configured);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean isSecretOk(String userCredentials, String configured) {
        if (configured.startsWith("plain|")) {
            return plainAuth(userCredentials, configured.substring("plain|".length()));
        } else if (configured.startsWith("sha256|")) {
            return sha256Auth(userCredentials, configured.substring("sha256|".length()));
        } else {
            return plainAuth(userCredentials, configured);
        }
    }

    private static boolean plainAuth(String userCredentials, String configured) {
        return userCredentials.equals(configured);
    }

    private static boolean sha256Auth(String userCredentials, String configured) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] unhashed = userCredentials.getBytes(StandardCharsets.UTF_8);
            byte[] hashed = sha256.digest(unhashed);
            String hex = bytesToHex(hashed);
            return hex.equalsIgnoreCase(configured);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
