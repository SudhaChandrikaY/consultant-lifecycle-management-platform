package com.ensar.clmp.submission.domain;

import java.util.List;

import org.springframework.data.repository.Repository;

/** Append-only (FR-060): save and read only. */
public interface SubmissionNoteRepository extends Repository<SubmissionNote, Long> {

    SubmissionNote save(SubmissionNote note);

    List<SubmissionNote> findBySubmissionIdOrderByCreatedAtAscIdAsc(Long submissionId);
}
