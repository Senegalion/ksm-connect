package com.ksmconnect.user;

public record PersonName(String firstName, String lastName) {

    public PersonName {
        firstName = normalize(firstName);
        lastName = normalize(lastName);
    }

    private static String normalize(String value) {
        return value == null ? null : value.strip().replaceAll("\\s+", " ");
    }
}
