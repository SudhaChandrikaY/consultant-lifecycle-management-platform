package com.ensar.clmp.recruiter.service;

import com.ensar.clmp.recruiter.domain.RecruiterStatus;

public record RecruiterFilter(String q, Long teamId, Long regionId, RecruiterStatus status) {
}
