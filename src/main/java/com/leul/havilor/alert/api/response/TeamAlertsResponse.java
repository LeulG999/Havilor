package com.leul.havilor.alert.api.response;

import java.time.Instant;
import java.util.List;

// The alert numbers for the manager's team dashboard (the dashboard module adds the standup count).
public record TeamAlertsResponse(
        long unassignedOpen,          // open and nobody has it
        long criticalOpen,            // open and CRITICAL
        OldestOpen oldestOpen,        // the open alert waiting longest; null when nothing is open
        List<AnalystLoad> analysts    // every active analyst, by name, also those with 0
) {

    // ageHours: whole hours since it arrived (receivedAt), at the time of the request.
    public record OldestOpen(Long alertId, String ticketNumber, Instant receivedAt, long ageHours) {
    }

    public record AnalystLoad(Long analystId, String name, long openAlerts, long closedLast7Days) {
    }
}
