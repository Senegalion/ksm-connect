package com.ksmconnect.wishlist;

import com.ksmconnect.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "gift_suggestions")
public class GiftSuggestion extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID eventParticipantId;

    @Column(nullable = false, updatable = false)
    private short position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionType type;

    private String url;

    private String description;

    protected GiftSuggestion() {
    }

    GiftSuggestion(UUID eventParticipantId, int position, SuggestionContent content) {
        this.eventParticipantId = eventParticipantId;
        this.position = (short) position;
        replaceContent(content);
    }

    void replaceContent(SuggestionContent content) {
        this.type = content.type();
        this.url = content.url();
        this.description = content.description();
    }

    public int getPosition() {
        return position;
    }

    public SuggestionType getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }
}
