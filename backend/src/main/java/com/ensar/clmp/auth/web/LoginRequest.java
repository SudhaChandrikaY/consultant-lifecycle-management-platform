package com.ensar.clmp.auth.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {

    /** Never print the password. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + "]";
    }
}
