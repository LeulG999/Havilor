package com.leul.havilor.playbook.domain;

import com.leul.havilor.common.CaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// A versioned checklist for one case type. Seeded by Flyway and never changed by the app:
// @Immutable makes Hibernate refuse to write it, and there is no constructor for our code to call.
@Entity
@Table(name = "playbook")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playbook {

    @Id
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 30)
    private CaseType caseType;

    // The playbook's content version (1, 2, ...), not an optimistic-locking @Version.
    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "playbook")
    @OrderBy("orderNo")
    private List<PlaybookStep> steps = new ArrayList<>();

    public List<PlaybookStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
