package com.ensar.clmp.common.web;

import java.time.Instant;

public record NoteResponse(Long id, String body, String author, Instant createdAt) {
}
