package com.ensar.clmp.dashboard.web;

import com.ensar.clmp.consultant.domain.ConsultantStatus;

public record StatusCount(ConsultantStatus status, long count, CountTile.Link link) {
}
