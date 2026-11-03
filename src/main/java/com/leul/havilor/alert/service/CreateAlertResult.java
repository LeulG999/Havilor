package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.response.AlertDetailResponse;

// The service's answer to "create": the alert, and whether this call created it
// (true → 201 Created) or found it already stored (false → 200 OK).
public record CreateAlertResult(AlertDetailResponse alert, boolean wasCreated) {

    static CreateAlertResult newlyCreated(AlertDetailResponse alert) {
        return new CreateAlertResult(alert, true);
    }

    static CreateAlertResult alreadyExisted(AlertDetailResponse alert) {
        return new CreateAlertResult(alert, false);
    }
}
