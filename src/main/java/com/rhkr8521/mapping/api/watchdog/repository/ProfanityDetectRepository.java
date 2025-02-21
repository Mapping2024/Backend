package com.rhkr8521.mapping.api.watchdog.repository;

import com.rhkr8521.mapping.api.watchdog.entity.ProfanityDetect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfanityDetectRepository extends JpaRepository<ProfanityDetect, Long> {
}
