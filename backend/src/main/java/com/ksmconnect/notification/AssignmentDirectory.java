package com.ksmconnect.notification;

import java.util.Optional;
import java.util.UUID;

public interface AssignmentDirectory {

    Optional<AssignmentNotice> noticeForGiver(UUID giverParticipantId);
}
