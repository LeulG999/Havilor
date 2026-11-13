package com.leul.havilor.dashboard.service;

import com.leul.havilor.alert.api.response.TeamAlertsResponse;
import com.leul.havilor.alert.service.AlertQueryService;
import com.leul.havilor.dashboard.api.response.TeamDashboardResponse;
import com.leul.havilor.standup.service.StandupService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

// The manager's dashboard combines two modules: alert numbers (alert module) and today's shared standups (standup
// module). It lives in its own small module because standup already depends on alert: asking standup from inside
// the alert module would make a cycle (ModuleBorderTest forbids that).
@Service
public class TeamDashboardService {

    private final AlertQueryService alertQueryService;
    private final StandupService standupService;
    private final Clock clock;

    public TeamDashboardService(AlertQueryService alertQueryService, StandupService standupService, Clock clock) {
        this.alertQueryService = alertQueryService;
        this.standupService = standupService;
        this.clock = clock;
    }

    public TeamDashboardResponse teamDashboard() {
        TeamAlertsResponse alerts = alertQueryService.teamAlerts();
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        return new TeamDashboardResponse(alerts.unassignedOpen(), alerts.criticalOpen(), alerts.oldestOpen(),
                alerts.analysts(), standupService.countSharedOn(today));
    }
}
