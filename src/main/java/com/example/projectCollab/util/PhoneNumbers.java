package com.example.projectCollab.util;

public final class PhoneNumbers {

    private static final String PATTERN = "^\\+?[0-9]{8,15}$";

    private PhoneNumbers() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[\\s().-]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    public static String requireValidOrEmpty(String value) {
        String phone = normalize(value);
        if (phone != null && !phone.matches(PATTERN)) {
            throw new IllegalArgumentException(
                    "Enter a valid phone number with 8 to 15 digits. You can start with + for a country code."
            );
        }
        return phone;
    }
}
