package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.mavlink.MavlinkClientService;
import com.rakovpublic.jneuropallium.worker.bridge.mavlink.MavlinkMessageBinding;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import io.dronefleet.mavlink.common.MavSeverity;
import io.dronefleet.mavlink.common.Statustext;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * {@link IOutputAggregator} for the autonomous-movement egress over the MAVLink bridge.
 *
 * <p>This is the network-output boundary for movement: the movement policy layer emits a
 * {@link MotorCommandSignal} (params {@code [velocityX, velocityY, targetAltitude, duration, yaw]});
 * this aggregator encodes it as an advisory {@code STATUSTEXT} {@code "JM:vx,vy,alt,yaw"} on the
 * binding matched by the signal's tag and sends it via {@link MavlinkClientService}. The thin
 * CARLA-Air bridge subscribes to that STATUSTEXT and executes the velocity/altitude/yaw on the
 * AirSim multirotor — so jneopallium never touches an actuating MAVLink message type
 * (12-MAVLINK.md §3, §5), and the whole loop stays {@code sim -> mavlink -> jneopallium ->
 * OutputAggregator -> mavlink -> sim}.
 *
 * <p>Only a {@link MotorCommandSignal} with {@code execute=true} is forwarded (the §0.1 gate); the
 * horizontal-velocity axis is clamped to the binding's {@code maxClampValue} when configured.
 */
public final class MovementCommandOutputAggregator implements IOutputAggregator {

    private final MavlinkClientService svc;
    private final AbstractBridgeAuditOutput audit;

    public MovementCommandOutputAggregator(MavlinkClientService svc, AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        if (results == null || results.isEmpty()) {
            return;
        }
        for (IResult r : results) {
            if (r == null) {
                continue;
            }
            IResultSignal<?> s = r.getResult();
            if (!(s instanceof MotorCommandSignal mc)) {
                continue;
            }
            Long neuronId = r.getNeuronId();
            try {
                handleMotor(mc, timestamp, run, neuronId);
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, MavlinkClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        null, mc.getName(), magnitude(mc), null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                        null, evidence(neuronId)));
            }
        }
    }

    private void handleMotor(MotorCommandSignal mc, long ts, long run, Long neuronId) {
        // §0.1: only forward when the planning chain marked the command executable.
        if (!mc.isExecute()) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, mc.getName(), magnitude(mc), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE,
                    BridgeSafetyMode.SHADOW, evidence(neuronId)));
            return;
        }
        String tag = mc.getName();
        MavlinkMessageBinding b = tag == null ? null : svc.writeBindingForTag(tag);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, tag, magnitude(mc), null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }
        BridgeSafetyMode mode = svc.config().perTagSafetyMode()
                .getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, magnitude(mc), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }

        double[] params = mc.getParams();
        double vx = params != null && params.length > 0 ? params[0] : 0.0;
        double vy = params != null && params.length > 1 ? params[1] : 0.0;
        double alt = params != null && params.length > 2 ? params[2] : 0.0;
        double yaw = params != null && params.length > 4 ? params[4] : 0.0;

        String modifyReason = null;
        if (b.maxClampValue() != null) {
            double limit = b.maxClampValue();
            double speed = Math.hypot(vx, vy);
            if (speed > limit && speed > 1e-9) {
                double scale = limit / speed;
                vx *= scale;
                vy *= scale;
                modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_HIGH;
            }
        }

        Statustext payload = encode(vx, vy, alt, yaw);
        boolean ok = svc.send(b.bindingId(), payload, ts, run);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, magnitude(mc), Math.hypot(vx, vy),
                    modifyReason, mode, evidence(neuronId)));
        }
    }

    /** {@code JM:vx,vy,alt,yaw} — fits the 50-byte STATUSTEXT limit; parsed by the CARLA-Air bridge. */
    static Statustext encode(double vx, double vy, double alt, double yaw) {
        String body = String.format(Locale.ROOT, "JM:%.2f,%.2f,%.2f,%.0f", vx, vy, alt, yaw);
        if (body.length() > 50) {
            body = body.substring(0, 50);
        }
        return Statustext.builder()
                .severity(MavSeverity.MAV_SEVERITY_INFO)
                .text(body)
                .build();
    }

    private static Double magnitude(MotorCommandSignal mc) {
        return mc == null ? null : mc.getMagnitudeEstimate();
    }

    private static List<String> evidence(Long neuronId) {
        return neuronId == null ? List.of() : List.of(String.valueOf(neuronId));
    }
}
