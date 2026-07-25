/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;

import java.util.List;

/**
 * Pure functions: MAVLink message ↔ Jneopallium typed signal
 * (12-MAVLINK.md §5).
 *
 * <p>The mapper is intentionally minimal: each method takes the already-decoded
 * MAVLink payload (one of the dronefleet records) plus the binding and
 * returns a typed Jneopallium signal. Connection-level concerns (system_id
 * routing, decimation, expected-systems whitelist) live in
 * {@link MavlinkClientService}.
 */
public final class MavlinkSignalMapper {

    /** RADIO_STATUS thresholds at which we mark the link "anomalous" (§5). */
    private static final int RSSI_LOW_THRESHOLD = 30;
    private static final int NOISE_HIGH_THRESHOLD = 80;

    /** GLOBAL_POSITION_INT → ProprioceptiveSignal (own MAV) (§5). */
    public IInputSignal toOwnPosition(MavlinkMessageBinding b,
                                      io.dronefleet.mavlink.common.GlobalPositionInt msg,
                                      int systemId,
                                      long ts) {
        // lat/lon scaled by 1e7; alt mm; vx/vy/vz cm/s.
        double lat = msg.lat() / 1e7;
        double lon = msg.lon() / 1e7;
        double alt = msg.alt() / 1000.0;
        double relAlt = msg.relativeAlt() / 1000.0;
        double vx = msg.vx() / 100.0;
        double vy = msg.vy() / 100.0;
        double vz = msg.vz() / 100.0;
        double hdg = msg.hdg() / 100.0;
        ProprioceptiveSignal sig = new ProprioceptiveSignal(
                systemId, new double[]{lat, lon, alt, relAlt, vx, vy, vz, hdg}, ts);
        sig.setName(b.signalTag() == null ? b.bindingId() : b.signalTag());
        return sig;
    }

    /** GLOBAL_POSITION_INT → PeerObservationSignal (drone observed by another) (§5). */
    public IInputSignal toPeerObservation(MavlinkMessageBinding b,
                                          io.dronefleet.mavlink.common.GlobalPositionInt msg,
                                          int systemId) {
        double lat = msg.lat() / 1e7;
        double lon = msg.lon() / 1e7;
        double alt = msg.alt() / 1000.0;
        double vx = msg.vx() / 100.0;
        double vy = msg.vy() / 100.0;
        double vz = msg.vz() / 100.0;
        String peerId = b.peerId() == null ? Integer.toString(systemId) : b.peerId();
        PeerObservationSignal sig = new PeerObservationSignal(
                peerId, new double[]{lat, lon, alt}, new double[]{vx, vy, vz}, 1.0);
        sig.setName(b.signalTag() == null ? b.bindingId() : b.signalTag());
        return sig;
    }

    /** ATTITUDE → ProprioceptiveSignal (§5). */
    public IInputSignal toAttitude(MavlinkMessageBinding b,
                                   io.dronefleet.mavlink.common.Attitude msg,
                                   int systemId,
                                   long ts) {
        ProprioceptiveSignal sig = new ProprioceptiveSignal(
                systemId,
                new double[]{
                        msg.roll(), msg.pitch(), msg.yaw(),
                        msg.rollspeed(), msg.pitchspeed(), msg.yawspeed()
                },
                ts);
        sig.setName(b.signalTag() == null ? b.bindingId() : b.signalTag());
        return sig;
    }

    /** BATTERY_STATUS → EfficiencySignal (§5). */
    public IInputSignal toBattery(MavlinkMessageBinding b,
                                  io.dronefleet.mavlink.common.BatteryStatus msg) {
        // batteryRemaining: 0..100 percent (-1 for unknown).
        int remaining = msg.batteryRemaining();
        double pct = remaining < 0 ? 0.0 : remaining / 100.0;
        String unit = b.signalTag() == null ? b.bindingId() : b.signalTag();
        return new EfficiencySignal(unit, pct, 1.0);
    }

    /** SYS_STATUS → AlarmSignal when any sensor-health flag is set (§5). */
    public IInputSignal toSysStatus(MavlinkMessageBinding b,
                                    io.dronefleet.mavlink.common.SysStatus msg,
                                    int systemId,
                                    long ts) {
        long present = msg.onboardControlSensorsPresent() == null
                ? 0L : msg.onboardControlSensorsPresent().value();
        long enabled = msg.onboardControlSensorsEnabled() == null
                ? 0L : msg.onboardControlSensorsEnabled().value();
        long health = msg.onboardControlSensorsHealth() == null
                ? 0L : msg.onboardControlSensorsHealth().value();
        // Healthy = (enabled & present) ⊆ health. Anything enabled-but-unhealthy → alarm.
        long unhealthy = (enabled & present) & ~health;
        if (unhealthy == 0 && msg.dropRateComm() == 0) return null;

        AlarmPriority pri = unhealthy != 0 ? AlarmPriority.HIGH : AlarmPriority.LOW;
        String tag = (b.signalTag() != null ? b.signalTag()
                : (b.signalTagPrefix() != null ? b.signalTagPrefix() : b.bindingId()))
                + "." + systemId;
        return new AlarmSignal(pri, tag,
                "SENSOR_UNHEALTHY:0x" + Long.toHexString(unhealthy)
                        + " drop=" + msg.dropRateComm(),
                ts);
    }

    /** STATUSTEXT → AlarmSignal (severity → priority) (§5). */
    public IInputSignal toStatusText(MavlinkMessageBinding b,
                                     io.dronefleet.mavlink.common.Statustext msg,
                                     int systemId,
                                     long ts) {
        AlarmPriority pri = mapSeverity(msg.severity() == null ? null : msg.severity().entry());
        String prefix = b.signalTagPrefix() != null ? b.signalTagPrefix()
                : (b.signalTag() != null ? b.signalTag() : b.bindingId());
        String tag = prefix + "." + systemId;
        String text = msg.text() == null ? "" : msg.text();
        return new AlarmSignal(pri, tag, "STATUSTEXT:" + text, ts);
    }

    /** RADIO_STATUS → AnomalyScoreSignal when RSSI/noise crosses thresholds (§5). */
    public IInputSignal toRadioStatus(MavlinkMessageBinding b,
                                      io.dronefleet.mavlink.common.RadioStatus msg,
                                      int systemId) {
        int rssi = msg.rssi();
        int noise = msg.noise();
        if (rssi >= RSSI_LOW_THRESHOLD && noise <= NOISE_HIGH_THRESHOLD) return null;

        double rssiPart = rssi >= RSSI_LOW_THRESHOLD ? 0.0
                : (RSSI_LOW_THRESHOLD - rssi) / (double) RSSI_LOW_THRESHOLD;
        double noisePart = noise <= NOISE_HIGH_THRESHOLD ? 0.0
                : (noise - NOISE_HIGH_THRESHOLD) / (double) (255 - NOISE_HIGH_THRESHOLD);
        double score = Math.max(0.0, Math.min(1.0, Math.max(rssiPart, noisePart)));

        String entity = (b.signalTag() != null ? b.signalTag() : b.bindingId()) + "." + systemId;
        return new AnomalyScoreSignal(entity, score,
                List.of("rssi=" + rssi, "noise=" + noise, "rxerrors=" + msg.rxerrors()));
    }

    /** Heartbeat-loss alarm (§5; emitted by {@link MavlinkClientService}). */
    public AlarmSignal peerOffline(int systemId, long ts) {
        return new AlarmSignal(AlarmPriority.HIGH,
                "DRONE." + systemId, "PEER_OFFLINE", ts);
    }

    /** GPS_RAW_INT → ProprioceptiveSignal — partial fix info; map similarly to GLOBAL_POSITION_INT. */
    public IInputSignal toGpsRaw(MavlinkMessageBinding b,
                                 io.dronefleet.mavlink.common.GpsRawInt msg,
                                 int systemId,
                                 long ts) {
        double lat = msg.lat() / 1e7;
        double lon = msg.lon() / 1e7;
        double alt = msg.alt() / 1000.0;
        ProprioceptiveSignal sig = new ProprioceptiveSignal(
                systemId, new double[]{lat, lon, alt, msg.satellitesVisible()}, ts);
        sig.setName(b.signalTag() == null ? b.bindingId() : b.signalTag());
        return sig;
    }

    private static AlarmPriority mapSeverity(io.dronefleet.mavlink.common.MavSeverity sev) {
        if (sev == null) return AlarmPriority.JOURNAL;
        return switch (sev) {
            case MAV_SEVERITY_EMERGENCY, MAV_SEVERITY_ALERT, MAV_SEVERITY_CRITICAL -> AlarmPriority.URGENT;
            case MAV_SEVERITY_ERROR, MAV_SEVERITY_WARNING -> AlarmPriority.HIGH;
            case MAV_SEVERITY_NOTICE -> AlarmPriority.LOW;
            case MAV_SEVERITY_INFO, MAV_SEVERITY_DEBUG -> AlarmPriority.JOURNAL;
        };
    }
}
