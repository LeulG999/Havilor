package com.leul.havilor.knowledge.service;

import com.leul.havilor.common.ai.EmbeddingClient;
import com.leul.havilor.knowledge.api.response.KnowledgeMatchResponse;
import com.leul.havilor.knowledge.domain.enums.KnowledgeSourceType;
import com.leul.havilor.knowledge.repository.KnowledgeChunkRepository;
import com.leul.havilor.playbook.api.response.PlaybookStepResponse;
import com.leul.havilor.playbook.api.response.PlaybookSummaryResponse;
import com.leul.havilor.playbook.service.PlaybookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

// Havilor's knowledge for retrieval-augmented generation (RAG): texts stored with their embeddings, searched by
// meaning. Without an embedding key it does nothing: no indexing, and every search returns [].
// Depends on playbook (to index its steps); the alert module calls this one, never the other way round.
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeChunkRepository repository;
    private final EmbeddingClient embeddingClient;
    private final PlaybookService playbookService;
    private final Clock clock;

    public KnowledgeService(KnowledgeChunkRepository repository, EmbeddingClient embeddingClient,
                            PlaybookService playbookService, Clock clock) {
        this.repository = repository;
        this.embeddingClient = embeddingClient;
        this.playbookService = playbookService;
        this.clock = clock;
    }

    // Every playbook step not indexed yet, in one embedding request. Run at startup (KnowledgeStartup).
    // Returns how many steps were indexed.
    public int indexPlaybookSteps() {
        if (!embeddingClient.isConfigured()) {
            return 0;
        }
        List<PlaybookStepResponse> steps = playbookService.list().stream()
                .map(PlaybookSummaryResponse::id)
                .flatMap(playbookId -> playbookService.getSteps(playbookId).stream())
                .filter(step -> !repository.exists(KnowledgeSourceType.PLAYBOOK_STEP, step.id()))
                .toList();
        if (steps.isEmpty()) {
            return 0;
        }
        List<String> texts = steps.stream().map(KnowledgeService::stepText).toList();
        List<float[]> embeddings = embeddingClient.embedDocuments(texts);
        for (int i = 0; i < steps.size(); i++) {
            repository.insert(KnowledgeSourceType.PLAYBOOK_STEP, steps.get(i).id(), texts.get(i), embeddings.get(i),
                    clock.instant());
        }
        return steps.size();
    }

    // Called by the alert module when an alert is closed. Its own transaction (REQUIRES_NEW): a failure here
    // rolls back only this insert, and the caller catches it, so closing the alert never fails because of it.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexClosedAlert(Long alertId, String content) {
        if (!embeddingClient.isConfigured()) {
            return;
        }
        float[] embedding = embeddingClient.embedDocuments(List.of(content)).getFirst();
        repository.insert(KnowledgeSourceType.CLOSED_ALERT, alertId, content, embedding, clock.instant());
    }

    // The `limit` most similar chunks. Knowledge is a bonus for the AI assistant, so a failing embedding API
    // means "no knowledge this time" (logged), not a failed request.
    public List<KnowledgeMatchResponse> search(String query, int limit) {
        if (!embeddingClient.isConfigured()) {
            return List.of();
        }
        try {
            return repository.findNearest(embeddingClient.embedQuery(query), limit);
        } catch (RuntimeException e) {
            log.warn("Knowledge search failed; continuing without reference knowledge", e);
            return List.of();
        }
    }

    static String stepText(PlaybookStepResponse step) {
        return "Playbook step: " + step.title()
                + "\nWhy it matters: " + step.whyItMatters()
                + "\nQuestion: " + step.question()
                + (step.toolGuide() == null ? "" : "\nHow to check: " + step.toolGuide());
    }
}
