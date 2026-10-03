package com.ensar.clmp.common.web;

/** {id, fullName} reference to a consultant or recruiter in a response. */
public record PersonRef(Long id, String fullName) {
}
