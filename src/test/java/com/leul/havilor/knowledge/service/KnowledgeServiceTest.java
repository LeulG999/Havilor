package com.leul.havilor.knowledge.service;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.common.ai.AiException;
import com.leul.havilor.common.ai.EmbeddingClient;
import com.leul.havilor.knowledge.repository.KnowledgeChunkRepository;
import com.leul.havilor.playbook.service.PlaybookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Without an embedding key the knowledge module does nothing, and a failing search is "no knowledge".
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeChunkRepository repository;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private PlaybookService playbookService;

    @Test
    void withoutAKeyNothingIsIndexedOrSearched() {
        when(embeddingClient.isConfigured()).thenReturn(false);
        KnowledgeService service = service();

        assertThat(service.indexPlaybookSteps()).isZero();
        service.indexClosedAlert(1L, "Closed alert 1");
        assertThat(service.search("anything", 5)).isEmpty();
        verifyNoInteractions(repository, playbookService);
    }

    @Test
    void aFailingSearchGivesNoKnowledgeInsteadOfAnError() {
        when(embeddingClient.isConfigured()).thenReturn(true);
        when(embeddingClient.embedQuery("invoice")).thenThrow(new AiException("down"));

        assertThat(service().search("invoice", 5)).isEmpty();
    }

    private KnowledgeService service() {
        return new KnowledgeService(repository, embeddingClient, playbookService, FixedClockConfiguration.CLOCK);
    }
}
