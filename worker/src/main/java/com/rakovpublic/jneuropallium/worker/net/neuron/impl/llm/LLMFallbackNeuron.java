/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit-breaker neuron (Layer 3).
 * Routes processing to internal knowledge when the LLM is unavailable.
 *
 * <p>Circuit breaker states:
 * <pre>
 *   CLOSED ──(failures >= threshold)──► OPEN
 *   OPEN   ──(probe interval elapsed)──► HALF_OPEN
 *   HALF_OPEN ──(success)──► CLOSED
 *   HALF_OPEN ──(failure)──► OPEN
 * </pre>
 *
 * <p>In OPEN state all LLM signals are rerouted to fallback processing immediately.
 */
public class LLMFallbackNeuron extends Neuron implements ILLMFallbackNeuron {

    private static final Logger logger = LogManager.getLogger(LLMFallbackNeuron.class);

    public enum CircuitState {
        CLOSED, HALF_OPEN, OPEN
    }

    private volatile CircuitState state = CircuitState.CLOSED;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastOpenedAt = new AtomicLong(0);

    private int failureThreshold;
    private long halfOpenProbeIntervalMs;

    public LLMFallbackNeuron(Long id, ISignalChain signalChain, Long run,
                              int failureThreshold, long halfOpenProbeIntervalMs) {
        super(id, signalChain, run);
        this.failureThreshold = failureThreshold;
        this.halfOpenProbeIntervalMs = halfOpenProbeIntervalMs;
        this.currentNeuronClass = LLMFallbackNeuron.class;
        this.addSignalProcessor(LLMTimeoutSignal.class, new LLMTimeoutProcessor(this));
        this.addSignalProcessor(LLMResponseSignal.class, new LLMFallbackResponseProcessor(this));
    }

    /**
     * Called when a successful LLM response is received.
     * Resets failure counter; transitions HALF_OPEN → CLOSED.
     */
    public void recordSuccess() {
        consecutiveFailures.set(0);
        if (state == CircuitState.HALF_OPEN) {
            logger.info("Circuit breaker: HALF_OPEN → CLOSED (LLM recovered)");
            state = CircuitState.CLOSED;
        }
    }

    /**
     * Called when an LLM timeout or error occurs.
     * Increments failure counter and may open the circuit.
     */
    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (state == CircuitState.CLOSED && failures >= failureThreshold) {
            logger.warn("Circuit breaker: CLOSED → OPEN after {} consecutive failures", failures);
            state = CircuitState.OPEN;
            lastOpenedAt.set(System.currentTimeMillis());
        } else if (state == CircuitState.HALF_OPEN) {
            logger.warn("Circuit breaker: HALF_OPEN → OPEN (probe failed)");
            state = CircuitState.OPEN;
            lastOpenedAt.set(System.currentTimeMillis());
        }
    }

    /**
     * Checks whether the circuit should transition from OPEN to HALF_OPEN
     * based on the elapsed probe interval, and updates state accordingly.
     *
     * @return current circuit state after possible transition
     */
    public CircuitState getState() {
        if (state == CircuitState.OPEN) {
            long elapsed = System.currentTimeMillis() - lastOpenedAt.get();
            if (elapsed >= halfOpenProbeIntervalMs) {
                logger.info("Circuit breaker: OPEN → HALF_OPEN (probe interval elapsed: {}ms)", elapsed);
                state = CircuitState.HALF_OPEN;
            }
        }
        return state;
    }

    /**
     * @return true when the circuit is CLOSED or HALF_OPEN (LLM calls allowed)
     */
    public boolean isLLMCallAllowed() {
        CircuitState current = getState();
        return current == CircuitState.CLOSED || current == CircuitState.HALF_OPEN;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public long getHalfOpenProbeIntervalMs() {
        return halfOpenProbeIntervalMs;
    }

    public void setHalfOpenProbeIntervalMs(long halfOpenProbeIntervalMs) {
        this.halfOpenProbeIntervalMs = halfOpenProbeIntervalMs;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /** Reset to initial CLOSED state (for testing / reconfiguration). */
    public void reset() {
        state = CircuitState.CLOSED;
        consecutiveFailures.set(0);
        lastOpenedAt.set(0);
    }
}
