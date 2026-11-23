package com.ksmconnect.draw;

public record Pairing<T>(T giver, T recipient) {

    @Override
    public String toString() {
        return "Pairing[redacted]";
    }
}
