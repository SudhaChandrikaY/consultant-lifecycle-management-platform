package com.ensar.clmp.reference.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByCode(String code);

    List<Team> findAllByOrderByName();
}
