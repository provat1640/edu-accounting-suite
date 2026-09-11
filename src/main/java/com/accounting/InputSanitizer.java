package com.accounting;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public final class InputSanitizer {
    private static final Pattern SAFE_ACCOUNT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 .&'_-]{0,79}");
    private InputSanitizer() { }

    public static String accountName(String value) {
        if (value == null || !SAFE_ACCOUNT.matcher(value.trim()).matches())
            throw new IllegalArgumentException("Account names may contain letters, numbers, spaces, and . & ' _ - only");
        return value.trim();
    }

    public static String description(String value) {
        if (value == null) return "";
        return value.trim().replace("\u0000", "").substring(0, Math.min(500, value.trim().length()));
    }

    public static Path inputFile(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("A file path is required");
        Path path = Paths.get(value).toAbsolutePath().normalize();
        if (!path.toFile().isFile()) throw new IllegalArgumentException("File does not exist: " + path);
        return path;
    }

    public static String spreadsheetText(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")
                ? "'" + trimmed : trimmed;
    }
}
