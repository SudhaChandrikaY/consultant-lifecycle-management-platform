package com.ensar.clmp.dashboard.web;

/** A dashboard figure and the list query that reproduces it (FR-083). */
public record CountTile(String key, String label, long value, Link link) {

    public record Link(String list, String query) {
    }
}
