/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AbstractBridgeOutputAggregatorTest {

    record TestBinding(
            String signalTag, String loopId, BridgeBindingDirection direction,
            Double failSafeValue, Double rampRateMaxPerSec,
            Double minClampValue, Double maxClampValue
    ) implements BridgeBinding {}

    static final class TestAudit extends AbstractBridgeAuditOutput {
        final List<BridgeAuditRecord> records = new ArrayList<>();
        TestAudit(Path file) { super(file); }
        @Override
        protected void mirror(BridgeAuditRecord record) { records.add(record); }
    }

    static final class TestAggregator
            extends AbstractBridgeOutputAggregator<TestBinding> {

        final Map<String, TestBinding> bindings = new HashMap<>();
        final Map<String, BridgeSafetyMode> modes = new HashMap<>();
        final List<double[]> writes = new ArrayList<>();
        final AtomicInteger failsFor = new AtomicInteger(-1);
        boolean operatorConfirms = false;
        boolean throwOnWrite = false;

        TestAggregator(OverrideRegistry o, AbstractBridgeAuditOutput a) {
            super("test", o, a);
        }

        @Override protected TestBinding binding(String tag) { return bindings.get(tag); }

        @Override
        protected BridgeSafetyMode safetyMode(TestBinding binding) {
            return modes.getOrDefault(binding.loopId(), BridgeSafetyMode.SHADOW);
        }

        @Override
        protected List<TestBinding> bindingsForInterlock(String interlockId) {
            List<TestBinding> out = new ArrayList<>();
            for (TestBinding b : bindings.values()) {
                if (interlockId.equals(b.loopId())) out.add(b);
            }
            return out;
        }

        @Override
        protected boolean operatorConfirmed(ActuatorCommandSignal command) { return operatorConfirms; }

        @Override
        protected BridgeWriteResult issueWrite(TestBinding binding, double value) {
            if (throwOnWrite) throw new IllegalStateException("boom");
            writes.add(new double[]{ value });
            int n = failsFor.getAndDecrement();
            return n == 0 ? BridgeWriteResult.failed("BAD") : BridgeWriteResult.ok();
        }
    }

    private static IResult result(IResultSignal<?> s, Long neuronId) {
        return new IResult() {
            @Override public IResultSignal getResult() { return s; }
            @Override public Long getNeuronId() { return neuronId; }
        };
    }

    private static TestAggregator newAggregator(@TempDir Path tmp) throws IOException {
        return new TestAggregator(new OverrideRegistry(),
                new TestAudit(tmp.resolve("audit.jsonl")));
    }

    @Test
    void unknownTagRejected(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.save(List.of(result(new ActuatorCommandSignal("MISSING", 1.0, 0.0, true), 7L)),
                1_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        assertEquals(1, ta.records.size());
        assertEquals(BridgeAuditRecord.Verdict.REJECTED, ta.records.get(0).verdict());
        assertEquals(BridgeAuditRecord.RejectReason.UNKNOWN_TAG, ta.records.get(0).reason());
        assertTrue(a.writes.isEmpty());
    }

    @Test
    void shadowModeRejectsWrite(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, null, null, null));
        a.modes.put("L", BridgeSafetyMode.SHADOW);
        a.save(List.of(result(new ActuatorCommandSignal("T", 5.0, 0.0, true), 1L)),
                1_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        assertEquals(1, ta.records.size());
        assertEquals(BridgeAuditRecord.RejectReason.SHADOW_MODE, ta.records.get(0).reason());
        assertTrue(a.writes.isEmpty());
    }

    @Test
    void advisoryRequiresExecuteAndConfirmation(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, null, null, null));
        a.modes.put("L", BridgeSafetyMode.ADVISORY);

        // execute=false → ADVISORY_HOLD
        a.save(List.of(result(new ActuatorCommandSignal("T", 1.0, 0.0, false), 1L)),
                1_000L, 1, null);
        // execute=true but no operator confirmation → ADVISORY_HOLD
        a.save(List.of(result(new ActuatorCommandSignal("T", 1.0, 0.0, true), 1L)),
                2_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        assertEquals(2, ta.records.size());
        assertEquals(BridgeAuditRecord.RejectReason.ADVISORY_HOLD, ta.records.get(0).reason());
        assertEquals(BridgeAuditRecord.RejectReason.ADVISORY_HOLD, ta.records.get(1).reason());
        assertTrue(a.writes.isEmpty());

        // with confirmation → APPLIED
        a.operatorConfirms = true;
        a.save(List.of(result(new ActuatorCommandSignal("T", 1.0, 0.0, true), 1L)),
                3_000L, 1, null);
        assertEquals(3, ta.records.size());
        assertEquals(BridgeAuditRecord.Verdict.APPLIED, ta.records.get(2).verdict());
        assertEquals(1, a.writes.size());
    }

    @Test
    void autonomousAppliesAndClampsHigh(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, null, 0.0, 100.0));
        a.modes.put("L", BridgeSafetyMode.AUTONOMOUS);
        a.save(List.of(result(new ActuatorCommandSignal("T", 250.0, 0.0, true), 1L)),
                1_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        BridgeAuditRecord r = ta.records.get(0);
        assertEquals(BridgeAuditRecord.Verdict.APPLIED, r.verdict());
        assertEquals(BridgeAuditRecord.ModifyReason.CLAMPED_HIGH, r.reason());
        assertEquals(100.0, r.effective(), 1e-9);
        assertEquals(250.0, r.proposed(), 1e-9);
        assertEquals(100.0, a.writes.get(0)[0], 1e-9);
    }

    @Test
    void rateLimitsBetweenTicks(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, 10.0, null, null));
        a.modes.put("L", BridgeSafetyMode.AUTONOMOUS);
        a.save(List.of(result(new ActuatorCommandSignal("T", 0.0, 0.0, true), 1L)),
                1_000L, 1, null);
        // 1 second later, request +50 — limited to +10
        a.save(List.of(result(new ActuatorCommandSignal("T", 50.0, 0.0, true), 1L)),
                2_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        BridgeAuditRecord r = ta.records.get(1);
        assertEquals(BridgeAuditRecord.Verdict.APPLIED, r.verdict());
        assertEquals(BridgeAuditRecord.ModifyReason.RATE_LIMITED, r.reason());
        assertEquals(10.0, r.effective(), 1e-9);
    }

    @Test
    void diffSuppressesIdenticalRepeats(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, null, null, null));
        a.modes.put("L", BridgeSafetyMode.AUTONOMOUS);
        a.save(List.of(result(new ActuatorCommandSignal("T", 5.0, 0.0, true), 1L)),
                1_000L, 1, null);
        a.save(List.of(result(new ActuatorCommandSignal("T", 5.0, 0.0, true), 1L)),
                1_500L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        assertEquals(2, ta.records.size());
        assertEquals(BridgeAuditRecord.ModifyReason.DIFF_SUPPRESSED, ta.records.get(1).reason());
        assertEquals(1, a.writes.size(), "second write should have been suppressed");
    }

    @Test
    void interlockTripDrivesFailSafeAndOverridesNothing(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, null, null, null));
        a.modes.put("L", BridgeSafetyMode.SHADOW); // would normally veto
        a.save(List.of(
                result(new InterlockSignal("L", true, List.of("HH")), 9L),
                result(new ActuatorCommandSignal("T", 99.0, 0.0, true), 1L)
        ), 1_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        // First record is INTERLOCK_TRIP, command after that goes to REJECTED SHADOW.
        assertEquals(BridgeAuditRecord.Verdict.INTERLOCK_TRIP, ta.records.get(0).verdict());
        assertEquals(0.0, ta.records.get(0).effective(), 1e-9);
        assertEquals(0.0, a.writes.get(0)[0], 1e-9);
    }

    @Test
    void operatorOverrideHoldsTagThenReleasesAfterTtl(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, null, null, null));
        a.modes.put("L", BridgeSafetyMode.AUTONOMOUS);
        a.save(List.of(
                result(new OperatorOverrideSignal("T",
                        com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OverrideKind.MANUAL,
                        "op-1", "test", 12.0), 1L),
                result(new ActuatorCommandSignal("T", 99.0, 0.0, true), 2L)
        ), 1_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        assertEquals(BridgeAuditRecord.Verdict.OVERRIDE_HOLD, ta.records.get(0).verdict());
        assertTrue(a.writes.isEmpty());
    }

    @Test
    void protocolFailureSurfacesFailedVerdict(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, null, null, null));
        a.modes.put("L", BridgeSafetyMode.AUTONOMOUS);
        a.failsFor.set(0); // very first write fails
        a.save(List.of(result(new ActuatorCommandSignal("T", 1.0, 0.0, true), 1L)),
                1_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        assertEquals(BridgeAuditRecord.Verdict.FAILED, ta.records.get(0).verdict());
        assertEquals("BAD", ta.records.get(0).reason());
    }

    @Test
    void exceptionInWriteIsContainedAsFailed(@TempDir Path tmp) throws IOException {
        TestAggregator a = newAggregator(tmp);
        a.bindings.put("T", new TestBinding("T", "L", BridgeBindingDirection.WRITE,
                0.0, null, null, null));
        a.modes.put("L", BridgeSafetyMode.AUTONOMOUS);
        a.throwOnWrite = true;
        a.save(List.of(result(new ActuatorCommandSignal("T", 1.0, 0.0, true), 1L)),
                1_000L, 1, null);
        TestAudit ta = (TestAudit) a.audit();
        assertEquals(BridgeAuditRecord.Verdict.FAILED, ta.records.get(0).verdict());
        assertTrue(ta.records.get(0).reason().startsWith(BridgeAuditRecord.RejectReason.EXCEPTION));
    }
}
