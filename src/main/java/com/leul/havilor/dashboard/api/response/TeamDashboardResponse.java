package com.leul.havilor.dashboard.api.response;

import com.leul.havilor.alert.api.response.TeamAlertsResponse;

import java.util.List;

// GET /api/v1/alerts/team-dashboard: the manager's start page.
public record TeamDashboardResponse(
        long unassignedOpen,                              // open and nobody has it
        long criticalOpen,                                // open and CRITICAL
        TeamAlertsResponse.OldestOpen oldestOpen,         // ticket + age of the open alert waiting longest (or null)
        List<TeamAlertsResponse.AnalystLoad> analysts,    // per active analyst: open now, closed in the last 7 days
        long standupsSharedToday                          // SHARED standup notes for today (UTC)
) {
}
