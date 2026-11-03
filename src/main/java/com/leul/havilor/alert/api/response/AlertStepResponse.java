package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.StepProgress;
import com.leul.havilor.alert.domain.enums.StepStatus;
import com.leul.havilor.playbook.api.response.PlaybookStepResponse;
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

    public static AlertStepResponse of(PlaybookStepResponse step, StepProgress progress) {
        return new AlertStepResponse(step.id(), step.orderNo(), step.title(), step.whyItMatters(), step.question(),
                step.answerOptions(), step.appliesWhen(), step.toolGuide(),
                progress.getStatus(), progress.getAnswer(), progress.getCompletedAt());
    }
}
