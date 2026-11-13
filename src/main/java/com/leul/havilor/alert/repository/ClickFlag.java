package com.leul.havilor.alert.repository;

// One click, reduced to what a queue row needs: which alert, and whether the mail gateway let it through.
public record ClickFlag(Long alertId, boolean permitted) {
}
