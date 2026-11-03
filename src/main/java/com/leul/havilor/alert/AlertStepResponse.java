package com.leul.havilor.alert;

import com.leul.havilor.playbook.PlaybookStepResponse;

import java.time.Instant;
import java.util.List;

// One step of an alert's playbook: what the playbook says (from the playbook module) + where this alert is with it.
public record AlertStepResponse(
        Long stepId,
        int orderNo,
        String title,
        String whyItMatters,
        String question,
        List<String> answerOptions,
        String appliesWhen,
        String toolGuide,
        StepStatus status,
        String answer,
        Instant completedAt
) {

    static AlertStepResponse of(PlaybookStepResponse step, StepProgress progress) {
        return new AlertStepResponse(step.id(), step.orderNo(), step.title(), step.whyItMatters(), step.question(),
                step.answerOptions(), step.appliesWhen(), step.toolGuide(),
                progress.getStatus(), progress.getAnswer(), progress.getCompletedAt());
    }
}
