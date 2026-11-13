package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.response.AlertDetailResponse;
import com.leul.havilor.alert.api.response.AlertSummaryResponse;
import com.leul.havilor.alert.api.response.DashboardResponse;
import com.leul.havilor.alert.api.response.QueueCountsResponse;
import com.leul.havilor.alert.api.response.TeamAlertsResponse;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.enums.AlertStatus;
import com.leul.havilor.alert.domain.enums.Priority;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.alert.repository.ClickFlag;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// The read side of alerts: one alert as the detail DTO, and the dashboard numbers.
// The transaction keeps the session open while the lazy child lists load.
@Service
public class AlertQueryService {

    static final int CHART_DAYS = 14;

    private final AlertRepository alertRepository;
    private final UserService userService;
    private final Clock clock;

    public AlertQueryService(AlertRepository alertRepository, UserService userService, Clock clock) {
        this.alertRepository = alertRepository;
        this.userService = userService;
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
                summaries(alertRepository.findTop5ByOrderByReceivedAtDescIdDesc()));
    }

    // Alerts as queue rows, with the assignees' names and the signals. A fixed number of queries for the whole list:
    // one for the names, one for the clicks, one each (@BatchSize) for the URLs and attachments.
    List<AlertSummaryResponse> summaries(List<Alert> alerts) {
        if (alerts.isEmpty()) {
            return List.of();
        }
        Map<Long, String> names = userService.namesByIds(alerts.stream().map(Alert::getAssigneeId).toList());
        Map<Long, List<ClickFlag>> clicks = alertRepository
                .findClickFlagsByAlertIds(alerts.stream().map(Alert::getId).toList()).stream()
                .collect(Collectors.groupingBy(ClickFlag::alertId));
        return alerts.stream()
                .map(alert -> AlertSummaryResponse.from(alert, names.get(alert.getAssigneeId()),
                        Signals.of(alert, clicks.getOrDefault(alert.getId(), List.of()))))
                .toList();
    }

    // The numbers above the queue (Flow D4): three for the whole queue, and one per priority with the queue's
    // status / assignee filter (that's what the priority buttons will show). 7 small count queries.
    @Transactional(readOnly = true)
    public QueueCountsResponse queueCounts(AlertFilter filter) {
        Map<Priority, Long> byPriority = new LinkedHashMap<>();
        for (Priority priority : Priority.values()) {
            byPriority.put(priority, alertRepository.count(filter.withPriority(priority).toSpecification()));
        }
        return new QueueCountsResponse(
                alertRepository.countByStatusNot(AlertStatus.CLOSED),
                alertRepository.countByStatusNotAndPriority(AlertStatus.CLOSED, Priority.CRITICAL),
                alertRepository.countByStatusNotAndAssigneeIdIsNull(AlertStatus.CLOSED),
                byPriority);
    }

    // The manager's view (Flow F26): simple counts, one pair of counts per active analyst, and the oldest open alert.
    @Transactional(readOnly = true)
    public TeamAlertsResponse teamAlerts() {
        Instant now = clock.instant();
        Instant weekAgo = now.minus(Duration.ofDays(7));
        List<TeamAlertsResponse.AnalystLoad> analysts = userService.activeAnalysts().stream()
                .map(analyst -> new TeamAlertsResponse.AnalystLoad(analyst.id(), analyst.name(),
                        alertRepository.countByStatusNotAndAssigneeId(AlertStatus.CLOSED, analyst.id()),
                        alertRepository.countByClosedAtGreaterThanEqualAndAssigneeId(weekAgo, analyst.id())))
                .toList();
        TeamAlertsResponse.OldestOpen oldest = alertRepository.findFirstByStatusNotOrderByReceivedAtAscIdAsc(AlertStatus.CLOSED)
                .map(alert -> new TeamAlertsResponse.OldestOpen(alert.getId(), alert.getTicketNumber(),
                        alert.getReceivedAt(), Duration.between(alert.getReceivedAt(), now).toHours()))
                .orElse(null);
        return new TeamAlertsResponse(
                alertRepository.countByStatusNotAndAssigneeIdIsNull(AlertStatus.CLOSED),
                alertRepository.countByStatusNotAndPriority(AlertStatus.CLOSED, Priority.CRITICAL),
                oldest,
                analysts);
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
        String assigneeName = userService.namesByIds(Collections.singletonList(alert.getAssigneeId()))
                .get(alert.getAssigneeId());
        return AlertDetailResponse.from(alert, alertRepository.findClicksByAlertId(id), assigneeName);
    }
}
