package com.leul.havilor.knowledge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// At startup: index the playbook steps that aren't indexed yet. A failure (no network, bad key) is logged and the
// app starts anyway: knowledge is a bonus, and the next start tries again.
@Component
class KnowledgeStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeStartup.class);

    private final KnowledgeService knowledgeService;

    KnowledgeStartup(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int indexed = knowledgeService.indexPlaybookSteps();
            if (indexed > 0) {
                log.info("Indexed {} playbook steps for knowledge search", indexed);
            }
        } catch (RuntimeException e) {
            log.warn("Could not index the playbook steps; knowledge search will miss them until the next start", e);
        }
    }
}
