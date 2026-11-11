package com.leul.havilor.knowledge.domain.enums;

// Where a piece of knowledge comes from.
public enum KnowledgeSourceType {
    PLAYBOOK_STEP,  // what to check, how, and why it matters
    CLOSED_ALERT    // a finished case: what it looked like and what the analyst decided
}
