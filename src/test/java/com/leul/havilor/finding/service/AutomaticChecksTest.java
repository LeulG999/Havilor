package com.leul.havilor.finding.service;

import com.leul.havilor.finding.domain.Finding;
import com.leul.havilor.finding.domain.enums.FindingSource;
import com.leul.havilor.finding.domain.enums.FindingType;
import com.leul.havilor.finding.domain.enums.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.leul.havilor.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

// The rules alone: facts in, findings out. No Spring, no database.
class AutomaticChecksTest {

    private static final CheckFacts CLEAN = new CheckFacts(false, false, false, List.of(), List.of(), List.of(), false);

    @Test
    void aCleanAlertHasNoFindings() {
        assertThat(AutomaticChecks.run(1L, CLEAN, NOW)).isEmpty();
    }

    @Test
    void everyRuleThatHitsGivesOneFinding() {
        CheckFacts facts = new CheckFacts(true, true, true,
                List.of("https://login.fake-bank.example.net/verify", "https://evil.example.org/x"),
                List.of("invoice.html"), List.of("alex@corp.example.com"), true);

        List<Finding> findings = AutomaticChecks.run(7L, facts, NOW);

        assertThat(findings).extracting(Finding::getType, Finding::getSeverity, Finding::getTitle).containsExactly(
                tuple(FindingType.SPF_FAIL, Severity.MEDIUM, "SPF failed"),
                tuple(FindingType.DKIM_FAIL, Severity.MEDIUM, "DKIM failed"),
                tuple(FindingType.DMARC_FAIL, Severity.HIGH, "DMARC failed"),
                tuple(FindingType.BAD_LIST_URL, Severity.HIGH,
                        "URL on a bad list: https://login.fake-bank.example.net/verify (+1 more)"),
                tuple(FindingType.MALICIOUS_ATTACHMENT, Severity.HIGH, "Malicious attachment: invoice.html"),
                tuple(FindingType.RECIPIENT_CLICKED, Severity.HIGH,
                        "Recipient clicked a link on a bad list: alex@corp.example.com"));
        assertThat(findings).allSatisfy(f -> {
            assertThat(f.getAlertId()).isEqualTo(7L);
            assertThat(f.getSource()).isEqualTo(FindingSource.AUTOMATIC);
            assertThat(f.getTeachingText()).isNotBlank();
            assertThat(f.getCreatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void aClickOnAnOrdinaryLinkIsStillAFinding() {
        CheckFacts facts = new CheckFacts(false, false, false, List.of(), List.of(), List.of("a@corp.example.com"), false);

        assertThat(AutomaticChecks.run(1L, facts, NOW)).singleElement()
                .extracting(Finding::getTitle).isEqualTo("Recipient clicked a link: a@corp.example.com");
    }

    @Test
    void longTitlesAreCutToFitTheColumn() {
        CheckFacts facts = new CheckFacts(false, false, false, List.of("https://example.net/" + "a".repeat(400)),
                List.of(), List.of(), false);

        assertThat(AutomaticChecks.run(1L, facts, NOW).getFirst().getTitle()).hasSize(300).endsWith("...");
    }
}
