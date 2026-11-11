package com.leul.havilor.alert.api.response;

import java.time.LocalDate;
import java.util.List;

// GET /api/v1/alerts/dashboard: the numbers on the analyst's start page.
public record DashboardResponse(
        long openAlerts,       // not CLOSED
        long assignedToMe,     // open and assigned to the logged-in user
        long criticalOpen,     // open and CRITICAL
        long closedLast7Days,  // closed in the last 7 days (closedAt)
        List<DayCount> alertsPerDay,       // the last 14 UTC days, oldest first, days without alerts included (0)
        List<AlertSummaryResponse> recent  // the 5 newest alerts
) {

    // Alerts received on one UTC day.
    public record DayCount(LocalDate date, long count) {
    }
}
