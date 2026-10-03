package com.ensar.clmp.history.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * An immutable audit row (FR-100, FR-101). There are no setters and the repository exposes no
 * update or delete. Values never include passwords or consultant contact details.
 */
@Entity
@Table(name = "history_record", indexes = {
        @Index(name = "ix_history_entity", columnList = "entity_type, entity_id, occurred_at"),
        @Index(name = "ix_history_occurred", columnList = "occurred_at"),
        @Index(name = "ix_history_consultant", columnList = "consultant_id") })
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, updatable = false, length = 30)
    private HistoryEntityType entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private Long entityId;

    @Column(name = "consultant_id", updatable = false)
    private Long consultantId;

    @Column(name = "owner_recruiter_id", updatable = false)
    private Long ownerRecruiterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, updatable = false, length = 30)
    private ChangeType changeType;

    /** One field (FIELD_EDIT) or the comma-separated changed field names (PROFILE_UPDATED). */
    @Column(name = "field_name", updatable = false, length = 255)
    private String fieldName;

    @Column(name = "old_value", updatable = false, length = 255)
    private String oldValue;

    @Column(name = "new_value", updatable = false, length = 255)
    private String newValue;

    @Column(updatable = false, length = 500)
    private String reason;

    @Column(updatable = false, length = 2000)
    private String note;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private Long actorUserId;

    @Column(name = "actor_display_name", nullable = false, updatable = false, length = 120)
    private String actorDisplayName;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "system_triggered", nullable = false, updatable = false)
    private boolean systemTriggered;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", updatable = false, length = 40)
    private TriggerEvent triggerEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_entity_type", updatable = false, length = 30)
    private HistoryEntityType triggerEntityType;

    @Column(name = "trigger_entity_id", updatable = false)
    private Long triggerEntityId;

    protected HistoryRecord() {
    }

    HistoryRecord(Builder b) {
        this.entityType = b.entityType;
        this.entityId = b.entityId;
        this.consultantId = b.consultantId;
        this.ownerRecruiterId = b.ownerRecruiterId;
        this.changeType = b.changeType;
        this.fieldName = b.fieldName;
        this.oldValue = truncate(b.oldValue, 255);
        this.newValue = truncate(b.newValue, 255);
        this.reason = b.reason;
        this.note = b.note;
        this.actorUserId = b.actorUserId;
        this.actorDisplayName = b.actorDisplayName;
        this.occurredAt = b.occurredAt;
        this.systemTriggered = b.triggerEvent != null;
        this.triggerEvent = b.triggerEvent;
        this.triggerEntityType = b.triggerEntityType;
        this.triggerEntityId = b.triggerEntityId;
    }

    public static Builder builder(HistoryEntityType entityType, Long entityId, ChangeType changeType) {
        return new Builder(entityType, entityId, changeType);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public Long getId() {
        return id;
    }

    public HistoryEntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Long getConsultantId() {
        return consultantId;
    }

    public Long getOwnerRecruiterId() {
        return ownerRecruiterId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getReason() {
        return reason;
    }

    public String getNote() {
        return note;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public boolean isSystemTriggered() {
        return systemTriggered;
    }

    public TriggerEvent getTriggerEvent() {
        return triggerEvent;
    }

    public HistoryEntityType getTriggerEntityType() {
        return triggerEntityType;
    }

    public Long getTriggerEntityId() {
        return triggerEntityId;
    }

    /** Collects the fields of one new row; only HistoryService builds rows. */
    public static final class Builder {
        private final HistoryEntityType entityType;
        private final Long entityId;
        private final ChangeType changeType;
        private Long consultantId;
        private Long ownerRecruiterId;
        private String fieldName;
        private String oldValue;
        private String newValue;
        private String reason;
        private String note;
        private Long actorUserId;
        private String actorDisplayName;
        private Instant occurredAt;
        private TriggerEvent triggerEvent;
        private HistoryEntityType triggerEntityType;
        private Long triggerEntityId;

        private Builder(HistoryEntityType entityType, Long entityId, ChangeType changeType) {
            this.entityType = entityType;
            this.entityId = entityId;
            this.changeType = changeType;
        }

        public Builder consultant(Long id) {
            this.consultantId = id;
            return this;
        }

        public Builder ownerRecruiter(Long id) {
            this.ownerRecruiterId = id;
            return this;
        }

        public Builder field(String name) {
            this.fieldName = name;
            return this;
        }

        public Builder values(String oldValue, String newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder note(String note) {
            this.note = note;
            return this;
        }

        public Builder actor(Long userId, String displayName) {
            this.actorUserId = userId;
            this.actorDisplayName = displayName;
            return this;
        }

        public Builder at(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        /** Marks the row system-triggered by {@code event} on the given record (FR-036). */
        public Builder trigger(TriggerEvent event, HistoryEntityType entityType, Long entityId) {
            this.triggerEvent = event;
            this.triggerEntityType = entityType;
            this.triggerEntityId = entityId;
            return this;
        }

        public HistoryRecord build() {
            return new HistoryRecord(this);
        }
    }
}
