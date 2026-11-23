package com.ksmconnect.auth;

record AuthEmailRequested(AuthEmailKind kind, String recipientEmail, String firstName, String rawToken) {

    @Override
    public String toString() {
        return "AuthEmailRequested[kind=" + kind + "]";
    }
}
