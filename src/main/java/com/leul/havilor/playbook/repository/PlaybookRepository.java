package com.leul.havilor.playbook.repository;

import com.leul.havilor.common.CaseType;
import com.leul.havilor.playbook.domain.Playbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// Other modules must not use this; they go through PlaybookService (border rule, enforced by ModuleBorderTest).
public interface PlaybookRepository extends JpaRepository<Playbook, Long> {

    List<Playbook> findAll(Sort sort);

    // One query: the playbook and its steps (ordered by @OrderBy on the collection).
    @Query("select p from Playbook p left join fetch p.steps where p.id = :id")
    Optional<Playbook> findWithStepsById(Long id);

    @Query("select p from Playbook p left join fetch p.steps where p.caseType = :caseType and p.active = true")
    Optional<Playbook> findActiveWithStepsByCaseType(CaseType caseType);
}
