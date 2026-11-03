package com.leul.havilor.playbook;

import java.util.List;

// One step, as other modules and the API see it. Public: the alert module uses it for GET /alerts/{id}/steps.
public record PlaybookStepResponse(
        Long id,
        int orderNo,
        String title,
        String whyItMatters,
        String question,
        List<String> answerOptions,
        String appliesWhen,
        String toolGuide
) {

    static PlaybookStepResponse from(PlaybookStep step) {
        return new PlaybookStepResponse(step.getId(), step.getOrderNo(), step.getTitle(), step.getWhyItMatters(),
                step.getQuestion(), step.getAnswerOptions(), step.getAppliesWhen(), step.getToolGuide());
    }
}
