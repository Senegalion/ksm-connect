package com.ksmconnect.security;

import com.ksmconnect.user.Role;
import com.ksmconnect.user.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public final class AuthenticatedUser implements UserDetails, CredentialsContainer {

    private final UUID id;
    private final Set<Role> roles;
    private String passwordHash;

    public AuthenticatedUser(UUID id, Set<Role> roles, String passwordHash) {
        this.id = id;
        this.roles = Set.copyOf(roles);
        this.passwordHash = passwordHash;
    }

    static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getRoles(), user.getPasswordHash());
    }

    public UUID id() {
        return id;
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())).toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return id.toString();
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
