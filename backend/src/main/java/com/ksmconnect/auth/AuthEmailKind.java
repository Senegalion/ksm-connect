package com.ksmconnect.auth;

enum AuthEmailKind {
    EMAIL_VERIFICATION("verify-email", "Potwierdź swój adres e-mail – KSM Connect", "/verify-email"),
    PASSWORD_RESET("password-reset", "Ustaw nowe hasło – KSM Connect", "/reset-password"),
    ACCOUNT_ALREADY_EXISTS("account-exists", "Masz już konto w KSM Connect", "/login");

    private final String template;
    private final String subject;
    private final String frontendPath;

    AuthEmailKind(String template, String subject, String frontendPath) {
        this.template = template;
        this.subject = subject;
        this.frontendPath = frontendPath;
    }

    String template() {
        return template;
    }

    String subject() {
        return subject;
    }

    String frontendPath() {
        return frontendPath;
    }
}
