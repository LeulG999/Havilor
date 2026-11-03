package com.leul.havilor.playbook;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

// Stores a step's answer options as one text column, 'Malicious|Suspicious|Clean', and gives Java a List.
// Simpler than a child table or a PostgreSQL array for a short, read-only list.
@Converter
class AnswerOptionsConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARATOR = "|";

    @Override
    public String convertToDatabaseColumn(List<String> options) {
        return options == null || options.isEmpty() ? null : String.join(SEPARATOR, options);
    }

    @Override
    public List<String> convertToEntityAttribute(String column) {
        if (column == null || column.isBlank()) {
            return List.of();
        }
        return Arrays.stream(column.split("\\|")).map(String::trim).toList();
    }
}
