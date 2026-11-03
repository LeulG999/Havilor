package com.leul.havilor.playbook;

import java.util.List;

// What another module needs to start following a playbook: which one, which version, and its step ids in order.
public record ActivePlaybook(Long id, int version, List<Long> stepIds) {
}
