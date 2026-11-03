package com.leul.havilor.alert;

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
