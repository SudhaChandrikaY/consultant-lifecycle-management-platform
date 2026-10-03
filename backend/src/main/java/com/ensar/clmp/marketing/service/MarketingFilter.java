package com.ensar.clmp.marketing.service;

import java.util.List;

import com.ensar.clmp.marketing.domain.MarketingStatus;

public record MarketingFilter(List<MarketingStatus> statuses, Long recruiterId, Long teamId, Boolean overdue) {
}
