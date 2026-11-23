package com.ksmconnect.user;

import java.util.UUID;

public interface AccountDeletionPolicy {

    boolean canDelete(UUID userId);
}
