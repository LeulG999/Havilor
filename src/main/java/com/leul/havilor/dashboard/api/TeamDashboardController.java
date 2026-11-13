package com.leul.havilor.dashboard.api;

import com.leul.havilor.dashboard.api.response.TeamDashboardResponse;
import com.leul.havilor.dashboard.service.TeamDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamDashboardController {

    private final TeamDashboardService teamDashboardService;

    public TeamDashboardController(TeamDashboardService teamDashboardService) {
        this.teamDashboardService = teamDashboardService;
    }

    // MANAGER only (rule in SecurityConfiguration). A fixed path wins over /api/v1/alerts/{id}.
    @GetMapping("/api/v1/alerts/team-dashboard")
    public TeamDashboardResponse teamDashboard() {
        return teamDashboardService.teamDashboard();
    }
}
