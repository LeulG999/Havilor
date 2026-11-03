package com.leul.havilor.playbook;

import com.leul.havilor.common.CaseType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// Package-private on purpose: other modules can't even see it. They go through PlaybookService (border rule).
interface PlaybookRepository extends JpaRepository<Playbook, Long> {

    List<Playbook> findAll(Sort sort);

    // One query: the playbook and its steps (ordered by @OrderBy on the collection).
    @Query("select p from Playbook p left join fetch p.steps where p.id = :id")
    Optional<Playbook> findWithStepsById(Long id);

    @Query("select p from Playbook p left join fetch p.steps where p.caseType = :caseType and p.active = true")
    Optional<Playbook> findActiveWithStepsByCaseType(CaseType caseType);
}
