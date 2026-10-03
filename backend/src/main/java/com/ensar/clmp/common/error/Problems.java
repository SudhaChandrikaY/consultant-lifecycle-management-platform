package com.ensar.clmp.common.error;

import java.net.URI;

import org.springframework.http.ProblemDetail;

/** Builds RFC 9457 problem bodies carrying the stable {@code code} property. */
public final class Problems {

    private Problems() {
    }

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(code.title());
        problem.setProperty("code", code.name());
        return problem;
    }

    /** Minimal JSON for filters that run outside Spring MVC (entry point, access-denied handler). */
    public static String json(ErrorCode code, String detail) {
        return "{\"type\":\"about:blank\",\"title\":\"" + code.title() + "\",\"status\":"
                + code.status().value() + ",\"detail\":\"" + detail + "\",\"code\":\"" + code.name()
                + "\"}";
    }
}
