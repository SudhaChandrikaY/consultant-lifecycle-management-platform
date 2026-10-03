package com.ensar.clmp.common.web;

/** {id, name} reference to a team, region, vendor, or client in a response. */
public record NamedRef(Long id, String name) {
}
