package com.ensar.clmp.recruiter.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long>, JpaSpecificationExecutor<Recruiter> {

    Optional<Recruiter> findByUserId(Long userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByUserId(Long userId);
}
