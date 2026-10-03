package com.ensar.clmp.reference.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByNormalizedName(String normalizedName);

    List<Client> findTop20ByNormalizedNameContainingOrderByName(String normalizedFragment);
}
