package com.ksmconnect.participant;

import java.util.UUID;

public record ParticipantContact(UUID participantId, UUID eventId, String firstName, String lastName,
                                 String email, String phoneNumber) {

    public String fullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "ParticipantContact[participantId=" + participantId + "]";
    }
}
