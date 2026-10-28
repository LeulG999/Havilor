package com.leul.havilor.alert;

// The service's answer to "create": the alert, and whether this call created it
// (true → 201 Created) or found it already stored (false → 200 OK).
public record CreateAlertResult(AlertResponse alert, boolean created) {

    static CreateAlertResult created(AlertResponse alert) {
        return new CreateAlertResult(alert, true);
    }

    static CreateAlertResult existing(AlertResponse alert) {
        return new CreateAlertResult(alert, false);
    }
}
