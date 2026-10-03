package com.ensar.clmp.common.domain;

/** An aggregate with an optimistic-lock version (research R10). */
public interface Versioned {

    Long getVersion();
}
