package com.ensar.clmp.common.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for adding an append-only note (marketing, submissions). */
public record NoteRequest(@NotBlank(message = "Note text is required.") @Size(max = 2000, message = "Note is too long.") String body) {
}
