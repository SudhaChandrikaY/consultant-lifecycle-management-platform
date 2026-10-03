package com.ensar.clmp.submission.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.TriggerLabelResolver;
import com.ensar.clmp.submission.domain.Submission;
import com.ensar.clmp.submission.domain.SubmissionRepository;

/** "Submission for {client} / {jobTitle}" in one batch query. */
@Component
public class SubmissionTriggerLabelResolver implements TriggerLabelResolver {

    private final SubmissionRepository submissions;

    public SubmissionTriggerLabelResolver(SubmissionRepository submissions) {
        this.submissions = submissions;
    }

    @Override
    public boolean supports(HistoryEntityType entityType) {
        return entityType == HistoryEntityType.SUBMISSION;
    }

    @Override
    public Map<Long, String> labels(Set<Long> ids) {
        return submissions.findLabelSourcesByIdIn(ids).stream().collect(Collectors.toMap(Submission::getId,
                s -> "Submission for " + s.getClient().getName() + " / " + s.getJobTitle()));
    }
}
