package com.leul.havilor.playbook.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerOptionsConverterTest {

    private final AnswerOptionsConverter converter = new AnswerOptionsConverter();

    @Test
    void roundTripsAList() {
        String column = converter.convertToDatabaseColumn(List.of("Malicious", "Suspicious", "Clean"));

        assertThat(column).isEqualTo("Malicious|Suspicious|Clean");
        assertThat(converter.convertToEntityAttribute(column)).containsExactly("Malicious", "Suspicious", "Clean");
    }

    @Test
    void emptyOrMissingMeansNoOptions() {
        assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute(" ")).isEmpty();
    }
}
