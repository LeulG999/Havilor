package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.Action;
import com.leul.havilor.alert.domain.enums.ActionResult;
import com.leul.havilor.alert.domain.enums.ActionType;

import java.time.Instant;

public record ActionResponse(
        Long id,
        Long alertId,
        ActionType type,
        ActionResult result,
        String note,
        Long performedBy,
        Instant performedAt
) {

    public static ActionResponse from(Action action) {
        return new ActionResponse(action.getId(), action.getAlertId(), action.getType(), action.getResult(),
                action.getNote(), action.getPerformedBy(), action.getPerformedAt());
    }
}
