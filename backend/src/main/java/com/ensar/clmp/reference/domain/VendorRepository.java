package com.ensar.clmp.reference.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByNormalizedName(String normalizedName);

    List<Vendor> findTop20ByNormalizedNameContainingOrderByName(String normalizedFragment);
}
