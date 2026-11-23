package com.ksmconnect.draw;

import com.ksmconnect.user.AccountDeletionPolicy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class AssignmentDeletionPolicy implements AccountDeletionPolicy {

    private final AssignmentRepository assignmentRepository;

    AssignmentDeletionPolicy(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete(UUID userId) {
        return !assignmentRepository.existsInvolvingUser(userId);
    }
}
