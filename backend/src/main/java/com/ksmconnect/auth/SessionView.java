package com.ksmconnect.auth;

import com.ksmconnect.user.Role;
import com.ksmconnect.user.User;

import java.util.Set;
import java.util.UUID;

public record SessionView(boolean authenticated, UserView user) {

    public record UserView(UUID id, String email, String firstName, String lastName, String phoneNumber,
                           Set<Role> roles) {
    }

    static SessionView anonymous() {
        return new SessionView(false, null);
    }

    static SessionView of(User user) {
        return new SessionView(true, new UserView(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getPhoneNumber(), user.getRoles()));
    }
}
