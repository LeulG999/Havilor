package com.leul.havilor.alert.service;

import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.enums.AlertStatus;
import com.leul.havilor.alert.domain.enums.Priority;
import com.leul.havilor.common.error.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// The queue's filters (Flow D4), all optional: a priority, a status ("OPEN" = everything not CLOSED, or one
// AlertStatus) and an assignee. toSpecification() turns them into one WHERE clause: a JPA Specification adds a
// condition only for the filters that are set, so one repository method covers every combination.
public record AlertFilter(Priority priority, String status, Long assigneeId) {

    public static final String OPEN = "OPEN";

    public static AlertFilter none() {
        return new AlertFilter(null, null, null);
    }

    // An unknown status → 400 with the allowed values (before any query runs).
    public AlertFilter {
        status = status == null || status.isBlank() ? null : status;
        if (status != null && !OPEN.equals(status) && !isAlertStatus(status)) {
            throw new BadRequestException("status", "must be OPEN or one of " + Arrays.toString(AlertStatus.values()));
        }
    }

    private static boolean isAlertStatus(String name) {
        return Arrays.stream(AlertStatus.values()).anyMatch(s -> s.name().equals(name));
    }

    // The same filter with another priority (for the per-priority counts).
    AlertFilter withPriority(Priority newPriority) {
        return new AlertFilter(newPriority, status, assigneeId);
    }

    Specification<Alert> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (priority != null) {
                conditions.add(cb.equal(root.get("priority"), priority));
            }
            if (OPEN.equals(status)) {
                conditions.add(cb.notEqual(root.get("status"), AlertStatus.CLOSED));
            } else if (status != null) {
                conditions.add(cb.equal(root.get("status"), AlertStatus.valueOf(status)));
            }
            if (assigneeId != null) {
                conditions.add(cb.equal(root.get("assigneeId"), assigneeId));
            }
            return cb.and(conditions.toArray(new Predicate[0])); // no conditions = every alert
        };
    }
}
