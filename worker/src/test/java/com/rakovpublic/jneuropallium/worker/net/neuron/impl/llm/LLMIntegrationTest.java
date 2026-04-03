package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LLMIntegrationTest {

    private ISignalChain mockChain;

    @BeforeEach
    void setUp() {
        mockChain = mock(ISignalChain.class);
        when(mockChain.getProcessingChain()).thenReturn(List.of());
        when(mockChain.getDescription()).thenReturn("test");
    }

    // ── LLMConfig ─────────────────────────────────────────────────────────────

    @Test
    void llmConfig_defaultsAreDisabled() {
        LLMConfig cfg = new LLMConfig();
        assertFalse(cfg.isEnabled());
        assertEquals("http://localhost:11434", cfg.getEndpoint());
        assertEquals(5000, cfg.getMaxLatencyMs());
        assertEquals(300, cfg.getCacheTtlSeconds());
        assertEquals("llama3", cfg.getModel());
        assertEquals(5, cfg.getCircuitBreakerFailureThreshold());
        assertEquals(30000, cfg.getCircuitBreakerHalfOpenProbeIntervalMs());
    }

    @Test
    void llmConfig_settersWork() {
        LLMConfig cfg = new LLMConfig();
        cfg.setEnabled(true);
        cfg.setEndpoint("http://example.com");
        cfg.setApiKey("key");
        cfg.setMaxLatencyMs(2000);
        cfg.setCacheTtlSeconds(60);
        cfg.setModel("gpt4");
        cfg.setCircuitBreakerFailureThreshold(3);
        cfg.setCircuitBreakerHalfOpenProbeIntervalMs(10000);

        assertTrue(cfg.isEnabled());
        assertEquals("http://example.com", cfg.getEndpoint());
        assertEquals("key", cfg.getApiKey());
        assertEquals(2000, cfg.getMaxLatencyMs());
        assertEquals(60, cfg.getCacheTtlSeconds());
        assertEquals("gpt4", cfg.getModel());
        assertEquals(3, cfg.getCircuitBreakerFailureThreshold());
        assertEquals(10000, cfg.getCircuitBreakerHalfOpenProbeIntervalMs());
    }

    // ── Payload classes ───────────────────────────────────────────────────────

    @Test
    void llmQueryItem_gettersAndSetters() {
        LLMQueryItem item = new LLMQueryItem("q1", "What is X?", "ctx", 5);
        assertEquals("q1", item.getQueryId());
        assertEquals("What is X?", item.getQueryText());
        assertEquals("ctx", item.getContext());
        assertEquals(5, item.getPriority());

        item.setQueryId("q2");
        item.setQueryText("Why?");
        item.setContext("ctx2");
        item.setPriority(10);
        assertEquals("q2", item.getQueryId());
        assertEquals("Why?", item.getQueryText());
        assertEquals("ctx2", item.getContext());
        assertEquals(10, item.getPriority());
    }

    @Test
    void llmResponseItem_gettersAndSetters() {
        LLMResponseItem item = new LLMResponseItem("q1", "answer", 0.8);
        assertEquals("q1", item.getQueryId());
        assertEquals("answer", item.getResponseText());
        assertEquals(0.8, item.getRawConfidence(), 1e-9);

        item.setQueryId("q2");
        item.setResponseText("other");
        item.setRawConfidence(0.3);
        assertEquals("q2", item.getQueryId());
        assertEquals("other", item.getResponseText());
        assertEquals(0.3, item.getRawConfidence(), 1e-9);
    }

    @Test
    void llmConfidenceItem_gettersAndSetters() {
        LLMConfidenceItem item = new LLMConfidenceItem("q1", 0.9, true, "ok");
        assertEquals("q1", item.getQueryId());
        assertEquals(0.9, item.getVerifiedConfidence(), 1e-9);
        assertTrue(item.isApplicable());
        assertEquals("ok", item.getVerificationNote());

        item.setQueryId("q2");
        item.setVerifiedConfidence(0.2);
        item.setApplicable(false);
        item.setVerificationNote("failed");
        assertEquals("q2", item.getQueryId());
        assertEquals(0.2, item.getVerifiedConfidence(), 1e-9);
        assertFalse(item.isApplicable());
        assertEquals("failed", item.getVerificationNote());
    }

    @Test
    void llmTimeoutItem_gettersAndSetters() {
        LLMTimeoutItem item = new LLMTimeoutItem("q1", 5500L, "exceeded");
        assertEquals("q1", item.getQueryId());
        assertEquals(5500L, item.getElapsedMs());
        assertEquals("exceeded", item.getReason());

        item.setQueryId("q2");
        item.setElapsedMs(1000L);
        item.setReason("disabled");
        assertEquals("q2", item.getQueryId());
        assertEquals(1000L, item.getElapsedMs());
        assertEquals("disabled", item.getReason());
    }

    // ── Signals ───────────────────────────────────────────────────────────────

    @Test
    void llmQuerySignal_loopAndEpoch() {
        LLMQuerySignal s = new LLMQuerySignal(new LLMQueryItem("q1", "text", "ctx", 1),
                0, 0L, 3, "desc", false, "in", false, false, "name");
        assertEquals(2, s.getLoop());
        assertEquals(2L, s.getEpoch());
        assertEquals(LLMQuerySignal.class, s.getCurrentSignalClass());
        assertEquals(LLMQueryItem.class, s.getParamClass());
    }

    @Test
    void llmQuerySignal_copySignal_preservesName() {
        LLMQuerySignal s = new LLMQuerySignal(new LLMQueryItem("q1", "x", "c", 2),
                1, 10L, 5, "d", true, "inp", false, true, "myName");
        LLMQuerySignal copy = s.copySignal();
        assertEquals("myName", copy.getName());
        assertEquals("q1", copy.getValue().getQueryId());
        assertEquals(2, copy.getLoop());
    }

    @Test
    void llmResponseSignal_loopAndEpoch() {
        LLMResponseSignal s = new LLMResponseSignal(new LLMResponseItem("q1", "ans", 0.7),
                0, 0L, 3, "desc", false, "in", false, false, "rname");
        assertEquals(2, s.getLoop());
        assertEquals(2L, s.getEpoch());
        assertEquals(LLMResponseSignal.class, s.getCurrentSignalClass());
        assertEquals(LLMResponseItem.class, s.getParamClass());
    }

    @Test
    void llmResponseSignal_copySignal_preservesName() {
        LLMResponseSignal s = new LLMResponseSignal(new LLMResponseItem("q2", "resp", 0.6),
                0, 0L, 1, "", false, "", false, false, "rn");
        LLMResponseSignal copy = s.copySignal();
        assertEquals("rn", copy.getName());
        assertEquals("q2", copy.getValue().getQueryId());
    }

    @Test
    void llmConfidenceSignal_loopAndEpoch() {
        LLMConfidenceSignal s = new LLMConfidenceSignal(new LLMConfidenceItem("q1", 0.9, true, "ok"),
                0, 0L, 2, "desc", false, "in", false, false, "cn");
        assertEquals(2, s.getLoop());
        assertEquals(3L, s.getEpoch());
        assertEquals(LLMConfidenceSignal.class, s.getCurrentSignalClass());
        assertEquals(LLMConfidenceItem.class, s.getParamClass());
    }

    @Test
    void llmTimeoutSignal_loopAndEpoch() {
        LLMTimeoutSignal s = new LLMTimeoutSignal(new LLMTimeoutItem("q1", 6000L, "timeout"),
                0, 0L, 1, "desc", false, "in", false, false, "tn");
        assertEquals(1, s.getLoop());
        assertEquals(1L, s.getEpoch());
        assertEquals(LLMTimeoutSignal.class, s.getCurrentSignalClass());
        assertEquals(LLMTimeoutItem.class, s.getParamClass());
    }

    @Test
    void llmTimeoutSignal_copySignal_preservesName() {
        LLMTimeoutSignal s = new LLMTimeoutSignal(new LLMTimeoutItem("q3", 1000L, "r"),
                0, 0L, 1, "", false, "", false, false, "tname");
        LLMTimeoutSignal copy = s.copySignal();
        assertEquals("tname", copy.getName());
        assertEquals("q3", copy.getValue().getQueryId());
    }

    // ── LLMFallbackNeuron — circuit breaker ───────────────────────────────────

    @Test
    void fallbackNeuron_initialStateClosed() {
        LLMFallbackNeuron n = new LLMFallbackNeuron(1L, mockChain, 1L, 5, 30000);
        assertEquals(LLMFallbackNeuron.CircuitState.CLOSED, n.getState());
        assertTrue(n.isLLMCallAllowed());
    }

    @Test
    void fallbackNeuron_opensAfterThresholdFailures() {
        LLMFallbackNeuron n = new LLMFallbackNeuron(1L, mockChain, 1L, 3, 30000);
        n.recordFailure();
        n.recordFailure();
        assertEquals(LLMFallbackNeuron.CircuitState.CLOSED, n.getState());
        n.recordFailure();
        assertEquals(LLMFallbackNeuron.CircuitState.OPEN, n.getState());
        assertFalse(n.isLLMCallAllowed());
    }

    @Test
    void fallbackNeuron_successResetsClosed() {
        LLMFallbackNeuron n = new LLMFallbackNeuron(1L, mockChain, 1L, 2, 30000);
        n.recordFailure();
        assertEquals(1, n.getConsecutiveFailures());
        n.recordSuccess();
        assertEquals(0, n.getConsecutiveFailures());
        assertEquals(LLMFallbackNeuron.CircuitState.CLOSED, n.getState());
    }

    @Test
    void fallbackNeuron_halfOpenSuccessTransitionsToClosed() {
        LLMFallbackNeuron n = new LLMFallbackNeuron(1L, mockChain, 1L, 1, 0);
        n.recordFailure(); // → OPEN
        assertEquals(LLMFallbackNeuron.CircuitState.OPEN, n.getState());
        // probe interval = 0 → immediately transitions to HALF_OPEN on next getState()
        assertEquals(LLMFallbackNeuron.CircuitState.HALF_OPEN, n.getState());
        assertTrue(n.isLLMCallAllowed());
        n.recordSuccess(); // HALF_OPEN → CLOSED
        assertEquals(LLMFallbackNeuron.CircuitState.CLOSED, n.getState());
    }

    @Test
    void fallbackNeuron_halfOpenFailureReopens() {
        LLMFallbackNeuron n = new LLMFallbackNeuron(1L, mockChain, 1L, 1, 0);
        n.recordFailure(); // CLOSED → OPEN
        n.getState();      // OPEN → HALF_OPEN
        n.recordFailure(); // HALF_OPEN → OPEN
        assertEquals(LLMFallbackNeuron.CircuitState.OPEN, n.getState());
    }

    @Test
    void fallbackNeuron_reset_restoresClosedState() {
        LLMFallbackNeuron n = new LLMFallbackNeuron(1L, mockChain, 1L, 1, 30000);
        n.recordFailure();
        assertEquals(LLMFallbackNeuron.CircuitState.OPEN, n.getState());
        n.reset();
        assertEquals(LLMFallbackNeuron.CircuitState.CLOSED, n.getState());
        assertEquals(0, n.getConsecutiveFailures());
    }

    // ── LLMVerificationNeuron ─────────────────────────────────────────────────

    @Test
    void verificationNeuron_nullResponseNotApplicable() {
        LLMVerificationNeuron n = new LLMVerificationNeuron(1L, mockChain, 1L);
        LLMResponseItem item = new LLMResponseItem("q1", null, 0.9);
        LLMResponseSignal signal = new LLMResponseSignal(item, 0, 0L, 1, "", false, "", false, false, "r");
        LLMConfidenceSignal cs = n.verify(signal);
        assertFalse(cs.getValue().isApplicable());
        assertEquals(0.0, cs.getValue().getVerifiedConfidence(), 1e-9);
    }

    @Test
    void verificationNeuron_blankResponseNotApplicable() {
        LLMVerificationNeuron n = new LLMVerificationNeuron(1L, mockChain, 1L);
        LLMResponseItem item = new LLMResponseItem("q1", "   ", 0.9);
        LLMResponseSignal signal = new LLMResponseSignal(item, 0, 0L, 1, "", false, "", false, false, "r");
        LLMConfidenceSignal cs = n.verify(signal);
        assertFalse(cs.getValue().isApplicable());
    }

    @Test
    void verificationNeuron_highConfidenceResponse_isApplicable() {
        LLMVerificationNeuron n = new LLMVerificationNeuron(1L, mockChain, 1L);
        String longEnoughText = "This is a valid response with enough length.";
        LLMResponseItem item = new LLMResponseItem("q1", longEnoughText, 0.9);
        LLMResponseSignal signal = new LLMResponseSignal(item, 0, 0L, 1, "", false, "", false, false, "r");
        LLMConfidenceSignal cs = n.verify(signal);
        assertTrue(cs.getValue().isApplicable());
        assertEquals("q1", cs.getValue().getQueryId());
    }

    @Test
    void verificationNeuron_shortTextReducesConfidence() {
        LLMVerificationNeuron n = new LLMVerificationNeuron(1L, mockChain, 1L);
        n.setApplicabilityThreshold(0.4);
        LLMResponseItem item = new LLMResponseItem("q1", "Hi", 1.0); // len=2 → * 0.3 = 0.3
        LLMResponseSignal signal = new LLMResponseSignal(item, 0, 0L, 1, "", false, "", false, false, "r");
        LLMConfidenceSignal cs = n.verify(signal);
        assertEquals(0.3, cs.getValue().getVerifiedConfidence(), 1e-9);
        assertFalse(cs.getValue().isApplicable()); // 0.3 < 0.4
    }

    @Test
    void verificationNeuron_customValidatorAdjustsConfidence() {
        LLMVerificationNeuron n = new LLMVerificationNeuron(1L, mockChain, 1L);
        // validator halves the confidence
        n.addValidator((item, conf) -> conf * 0.5);
        String text = "Sufficiently long response text here.";
        LLMResponseItem item = new LLMResponseItem("q1", text, 0.8);
        LLMResponseSignal signal = new LLMResponseSignal(item, 0, 0L, 1, "", false, "", false, false, "r");
        LLMConfidenceSignal cs = n.verify(signal);
        assertEquals(0.4, cs.getValue().getVerifiedConfidence(), 1e-9);
    }

    @Test
    void verificationNeuron_confidenceClampedTo1() {
        LLMVerificationNeuron n = new LLMVerificationNeuron(1L, mockChain, 1L);
        // validator tries to push > 1.0
        n.addValidator((item, conf) -> 2.0);
        LLMResponseItem item = new LLMResponseItem("q1", "Normal response text here.", 0.5);
        LLMResponseSignal signal = new LLMResponseSignal(item, 0, 0L, 1, "", false, "", false, false, "r");
        LLMConfidenceSignal cs = n.verify(signal);
        assertEquals(1.0, cs.getValue().getVerifiedConfidence(), 1e-9);
    }

    // ── Processors ────────────────────────────────────────────────────────────

    @Test
    void llmQueryProcessor_metadata() {
        LLMQueryProcessor p = new LLMQueryProcessor();
        assertFalse(p.hasMerger());
        assertEquals(LLMQueryProcessor.class, p.getSignalProcessorClass());
        assertEquals(ILLMCapable.class, p.getNeuronClass());
        assertEquals(LLMQuerySignal.class, p.getSignalClass());
        assertNotNull(p.getDescription());
    }

    @Test
    void llmQueryProcessor_nullInput_returnsEmptyList() {
        LLMQueryProcessor p = new LLMQueryProcessor();
        ILLMCapable neuron = mock(ILLMCapable.class);
        List<?> result = p.process(null, neuron);
        assertTrue(result.isEmpty());
        verify(neuron, never()).submitQuery(any());
    }

    @Test
    void llmQueryProcessor_validInput_callsSubmitQuery() {
        LLMQueryProcessor p = new LLMQueryProcessor();
        ILLMCapable neuron = mock(ILLMCapable.class);
        LLMQuerySignal signal = new LLMQuerySignal(new LLMQueryItem("q1", "t", "c", 1),
                0, 0L, 1, "", false, "", false, false, "n");
        List<?> result = p.process(signal, neuron);
        assertTrue(result.isEmpty());
        verify(neuron).submitQuery(signal);
    }

    @Test
    void llmResponseProcessor_metadata() {
        LLMResponseProcessor p = new LLMResponseProcessor();
        assertFalse(p.hasMerger());
        assertEquals(LLMResponseProcessor.class, p.getSignalProcessorClass());
        assertEquals(LLMResponseSignal.class, p.getSignalClass());
        assertNotNull(p.getDescription());
    }

    @Test
    void llmResponseProcessor_forwardsCopyOfSignal() {
        LLMResponseProcessor p = new LLMResponseProcessor();
        com.rakovpublic.jneuropallium.worker.net.neuron.INeuron neuron =
                mock(com.rakovpublic.jneuropallium.worker.net.neuron.INeuron.class);
        LLMResponseSignal signal = new LLMResponseSignal(new LLMResponseItem("q1", "resp", 0.7),
                0, 0L, 1, "", false, "", false, false, "rn");
        List<?> result = p.process(signal, neuron);
        assertEquals(1, result.size());
        LLMResponseSignal copy = (LLMResponseSignal) result.get(0);
        assertEquals("q1", copy.getValue().getQueryId());
        assertNotSame(signal, copy);
    }

    @Test
    void llmTimeoutProcessor_recordsFailureAndForwardsSignal() {
        LLMFallbackNeuron n = new LLMFallbackNeuron(1L, mockChain, 1L, 5, 30000);
        LLMTimeoutProcessor p = new LLMTimeoutProcessor(n);
        LLMTimeoutSignal signal = new LLMTimeoutSignal(new LLMTimeoutItem("q1", 6000L, "slow"),
                0, 0L, 1, "", false, "", false, false, "tn");
        List<?> result = p.process(signal, n);
        assertEquals(1, result.size());
        assertEquals(1, n.getConsecutiveFailures());
    }

    @Test
    void llmFallbackResponseProcessor_recordsSuccessAndForwardsSignal() {
        LLMFallbackNeuron n = new LLMFallbackNeuron(1L, mockChain, 1L, 5, 30000);
        n.recordFailure(); // set 1 failure
        LLMFallbackResponseProcessor p = new LLMFallbackResponseProcessor(n);
        LLMResponseSignal signal = new LLMResponseSignal(new LLMResponseItem("q1", "ok", 0.8),
                0, 0L, 1, "", false, "", false, false, "rn");
        List<?> result = p.process(signal, n);
        assertEquals(1, result.size());
        assertEquals(0, n.getConsecutiveFailures()); // reset after success
    }

    // ── LLMKnowledgeNeuron ────────────────────────────────────────────────────

    @Test
    void knowledgeNeuron_disabledByDefault_llmNotAvailable() {
        LLMConfig cfg = new LLMConfig(); // enabled=false
        LLMKnowledgeNeuron n = new LLMKnowledgeNeuron(1L, mockChain, 1L, cfg);
        assertFalse(n.isLLMAvailable());
    }

    @Test
    void knowledgeNeuron_enabledConfig_llmAvailable() {
        LLMConfig cfg = new LLMConfig();
        cfg.setEnabled(true);
        LLMKnowledgeNeuron n = new LLMKnowledgeNeuron(1L, mockChain, 1L, cfg);
        assertTrue(n.isLLMAvailable());
    }

    @Test
    void knowledgeNeuron_getCachedResponse_missingKey_returnsEmpty() {
        LLMConfig cfg = new LLMConfig();
        cfg.setEnabled(true);
        LLMKnowledgeNeuron n = new LLMKnowledgeNeuron(1L, mockChain, 1L, cfg);
        Optional<LLMResponseSignal> result = n.getCachedResponse("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void knowledgeNeuron_submitQuery_whenDisabled_producesTimeoutSignal() {
        LLMConfig cfg = new LLMConfig(); // disabled
        LLMKnowledgeNeuron n = new LLMKnowledgeNeuron(1L, mockChain, 1L, cfg);
        LLMQuerySignal signal = new LLMQuerySignal(new LLMQueryItem("q1", "text", "ctx", 1),
                0, 0L, 3, "", false, "", false, false, "n");
        n.submitQuery(signal);
        List<ISignal> results = n.result;
        assertEquals(1, results.size());
        assertInstanceOf(LLMTimeoutSignal.class, results.get(0));
        LLMTimeoutSignal timeout = (LLMTimeoutSignal) results.get(0);
        assertEquals("q1", timeout.getValue().getQueryId());
    }

    @Test
    void knowledgeNeuron_setEndpointAndLatency_updatesConfig() {
        LLMConfig cfg = new LLMConfig();
        LLMKnowledgeNeuron n = new LLMKnowledgeNeuron(1L, mockChain, 1L, cfg);
        n.setLLMEndpoint("http://new-host:8080");
        n.setMaxLatency(3000);
        assertEquals("http://new-host:8080", n.getConfig().getEndpoint());
        assertEquals(3000, n.getConfig().getMaxLatencyMs());
    }
}
