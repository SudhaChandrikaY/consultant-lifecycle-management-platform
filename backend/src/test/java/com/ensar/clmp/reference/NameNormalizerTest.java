package com.ensar.clmp.reference;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ensar.clmp.reference.service.NameNormalizer;

/** FR-055: case- and whitespace-insensitive matching of vendors, clients, and job titles. */
class NameNormalizerTest {

    @Test
    void vendorNamesNormalizeEqual() {
        assertThat(NameNormalizer.normalize("  Acme   Staffing ")).isEqualTo(NameNormalizer.normalize("acme staffing"))
                .isEqualTo("acme staffing");
    }

    @Test
    void jobTitlesNormalizeEqual() {
        assertThat(NameNormalizer.normalize("Java\tDeveloper")).isEqualTo(NameNormalizer.normalize(" JAVA developer "));
    }

    @Test
    void differentNamesStayDifferent() {
        assertThat(NameNormalizer.normalize("Acme Staffing")).isNotEqualTo(NameNormalizer.normalize("Acme Staff"));
    }

    @Test
    void nullAndBlank() {
        assertThat(NameNormalizer.normalize(null)).isNull();
        assertThat(NameNormalizer.normalize("   ")).isEmpty();
    }
}
