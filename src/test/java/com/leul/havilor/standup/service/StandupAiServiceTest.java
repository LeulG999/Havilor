package com.leul.havilor.standup.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Flow F23: the short subject in the AI facts.
class StandupAiServiceTest {

    @Test
    void aShortSubjectStaysAsItIs() {
        assertThat(StandupAiService.shortSubject("Fake invoice overdue")).isEqualTo("Fake invoice overdue");
    }

    @Test
    void aLongSubjectIsCutWithAnEllipsis() {
        String subject = "Action required: your mailbox storage is almost full, verify your account within 24 hours";

        String shortSubject = StandupAiService.shortSubject(subject);

        assertThat(shortSubject).hasSizeLessThanOrEqualTo(StandupAiService.SHORT_SUBJECT).endsWith("…")
                .startsWith("Action required: your mailbox storage is almost full");
    }
}
