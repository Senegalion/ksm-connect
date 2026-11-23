package com.ksmconnect.user;

import com.ksmconnect.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phoneNumber;

    @Column(nullable = false)
    private String passwordHash;

    private Instant emailVerifiedAt;

    @Column(nullable = false)
    private int failedLoginAttempts;

    private Instant lockedUntil;

    @Column(nullable = false)
    private String privacyNoticeVersion;

    @Column(nullable = false)
    private Instant privacyAcceptedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    protected User() {
    }

    public User(String email, PersonName name, String phoneNumber, String passwordHash,
                String privacyNoticeVersion, Instant privacyAcceptedAt) {
        this.email = email;
        this.firstName = name.firstName();
        this.lastName = name.lastName();
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.privacyNoticeVersion = privacyNoticeVersion;
        this.privacyAcceptedAt = privacyAcceptedAt;
        this.roles.add(Role.PARTICIPANT);
    }

    public void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(Instant now, int maxAttempts, Duration lockout) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockout);
            failedLoginAttempts = 0;
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        recordSuccessfulLogin();
    }

    public void updateProfile(PersonName name, String phoneNumber) {
        this.firstName = name.firstName();
        this.lastName = name.lastName();
        this.phoneNumber = phoneNumber;
    }

    public void grantRole(Role role) {
        roles.add(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<Role> getRoles() {
        return roles.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(roles));
    }
}
