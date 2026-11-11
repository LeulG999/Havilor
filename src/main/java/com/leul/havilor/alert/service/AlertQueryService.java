package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.response.AlertDetailResponse;
import com.leul.havilor.alert.api.response.AlertSummaryResponse;
import com.leul.havilor.alert.api.response.DashboardResponse;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.enums.AlertStatus;
import com.leul.havilor.alert.domain.enums.Priority;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

// The read side of alerts: one alert as the detail DTO, and the dashboard numbers.
// The transaction keeps the session open while the lazy child lists load.
@Service
public class AlertQueryService {

    static final int CHART_DAYS = 14;

    private final AlertRepository alertRepository;
    private final Clock clock;

    public AlertQueryService(AlertRepository alertRepository, Clock clock) {
        this.alertRepository = alertRepository;
        this.clock = clock;
    }

    // The dashboard: a few simple counts, one count per chart day, and the 5 newest alerts.
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long userId) {
        Instant now = clock.instant();
        return new DashboardResponse(
                alertRepository.countByStatusNot(AlertStatus.CLOSED),
                alertRepository.countByStatusNotAndAssigneeId(AlertStatus.CLOSED, userId),
                alertRepository.countByStatusNotAndPriority(AlertStatus.CLOSED, Priority.CRITICAL),
                alertRepository.countByClosedAtGreaterThanEqual(now.minus(Duration.ofDays(7))),
                alertsPerDay(LocalDate.ofInstant(now, ZoneOffset.UTC)),
                alertRepository.findTop5ByOrderByReceivedAtDescIdDesc().stream().map(AlertSummaryResponse::from).toList());
    }

    // The last 14 UTC days ending today, oldest first: one count query per day, so days without alerts are 0.
    private List<DashboardResponse.DayCount> alertsPerDay(LocalDate today) {
        List<DashboardResponse.DayCount> days = new ArrayList<>();
        for (LocalDate day = today.minusDays(CHART_DAYS - 1); !day.isAfter(today); day = day.plusDays(1)) {
            Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            days.add(new DashboardResponse.DayCount(day,
                    alertRepository.countByReceivedAtGreaterThanEqualAndReceivedAtLessThan(start, end)));
        }
        return days;
    }

    // For other modules that only need "does this alert exist?".
    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return alertRepository.existsById(id);
    }

    // The same, as a 404 when it doesn't.
    @Transactional(readOnly = true)
    public void ensureExists(Long id) {
        if (!alertRepository.existsById(id)) {
            throw NotFoundException.of("Alert", id);
        }
    }

    @Transactional(readOnly = true)
    public AlertDetailResponse getById(Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> NotFoundException.of("Alert", id));
        return AlertDetailResponse.from(alert, alertRepository.findClicksByAlertId(id));
    }
}
