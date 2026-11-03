package com.leul.havilor.playbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.List;

// One step of a playbook: what to check, why it matters (the teaching part), and how.
@Entity
@Table(name = "playbook_step")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaybookStep {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playbook_id", nullable = false)
    private Playbook playbook;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "why_it_matters", nullable = false, columnDefinition = "TEXT")
    private String whyItMatters;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Convert(converter = AnswerOptionsConverter.class)
    @Column(name = "answer_options", columnDefinition = "TEXT")
    private List<String> answerOptions;

    // null = the step always applies; otherwise the condition in words ("Someone clicked a URL").
    @Column(name = "applies_when", length = 200)
    private String appliesWhen;

    @Column(name = "tool_guide", columnDefinition = "TEXT")
    private String toolGuide;
}
