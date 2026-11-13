package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.enums.Priority;

import java.util.Map;

// GET /api/v1/alerts/counts: the numbers above the alert queue (Flow D4).
public record QueueCountsResponse(
        long open,                        // not CLOSED (whole queue)
        long criticalOpen,                // open and CRITICAL (whole queue)
        long unassignedOpen,              // open and nobody has it (whole queue)
        Map<Priority, Long> byPriority    // per priority, with the current status / "assigned to me" filter applied
) {
}
