package com.ensar.clmp.history.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.history.domain.ChangeType;
import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.domain.HistoryRecord;
import com.ensar.clmp.history.domain.HistoryRecordRepository;

/**
 * The only writer of history. Every method joins the caller's transaction (MANDATORY), so a
 * change and its history row commit or roll back together (FR-100).
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class HistoryService {

    private final HistoryRecordRepository records;
    private final Clock clock;

    public HistoryService(HistoryRecordRepository records, Clock clock) {
        this.records = records;
        this.clock = clock;
    }

    public void recordCreated(HistoryEntityType entityType, Long entityId, Long consultantId, Long ownerRecruiterId,
            String initialStatus, CurrentUser actor) {
        save(HistoryRecord.builder(entityType, entityId, ChangeType.CREATED)
                .consultant(consultantId).ownerRecruiter(ownerRecruiterId)
                .values(null, initialStatus), actor);
    }

    public void recordStatusChange(HistoryEntityType entityType, Long entityId, Long consultantId,
            Long ownerRecruiterId, String oldStatus, String newStatus, String reason, String note,
            CurrentUser actor) {
        save(HistoryRecord.builder(entityType, entityId, ChangeType.STATUS)
                .consultant(consultantId).ownerRecruiter(ownerRecruiterId)
                .values(oldStatus, newStatus).reason(blankToNull(reason)).note(blankToNull(note)), actor);
    }

    public void recordRecruiterAssignment(Long consultantId, String oldRecruiterName, String newRecruiterName,
            CurrentUser actor) {
        save(HistoryRecord.builder(HistoryEntityType.CONSULTANT, consultantId, ChangeType.RECRUITER_ASSIGNMENT)
                .consultant(consultantId).field("recruiter").values(oldRecruiterName, newRecruiterName), actor);
    }

    public void recordOwnerTransfer(HistoryEntityType entityType, Long entityId, Long consultantId,
            Long newOwnerRecruiterId, String oldOwnerName, String newOwnerName, CurrentUser actor) {
        save(HistoryRecord.builder(entityType, entityId, ChangeType.OWNER_TRANSFER)
                .consultant(consultantId).ownerRecruiter(newOwnerRecruiterId).field("ownerRecruiter")
                .values(oldOwnerName, newOwnerName), actor);
    }

    /** Records which profile fields changed: names only, never values (FR-103). */
    public void recordProfileUpdated(Long consultantId, List<String> changedFieldNames, CurrentUser actor) {
        save(HistoryRecord.builder(HistoryEntityType.CONSULTANT, consultantId, ChangeType.PROFILE_UPDATED)
                .consultant(consultantId).field(String.join(", ", changedFieldNames)), actor);
    }

    public void recordFieldEdit(HistoryEntityType entityType, Long entityId, Long consultantId, Long ownerRecruiterId,
            String fieldName, String oldValue, String newValue, CurrentUser actor) {
        save(HistoryRecord.builder(entityType, entityId, ChangeType.FIELD_EDIT)
                .consultant(consultantId).ownerRecruiter(ownerRecruiterId).field(fieldName)
                .values(oldValue, newValue), actor);
    }

    public void recordNoteAdded(HistoryEntityType entityType, Long entityId, Long consultantId, Long ownerRecruiterId,
            CurrentUser actor) {
        save(HistoryRecord.builder(entityType, entityId, ChangeType.NOTE_ADDED)
                .consultant(consultantId).ownerRecruiter(ownerRecruiterId), actor);
    }

    private void save(HistoryRecord.Builder builder, CurrentUser actor) {
        records.save(builder.actor(actor.userId(), actor.displayName()).at(clock.instant()).build());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
