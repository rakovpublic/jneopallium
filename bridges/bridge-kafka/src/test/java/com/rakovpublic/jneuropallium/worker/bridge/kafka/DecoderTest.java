/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.AnomalyScoreJsonDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.AvroSchemaRegistryDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.LogstashJsonDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.OsqueryDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.PayloadDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.SuricataEveDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.ZeekConnDecoder;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.LogLevel;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.ThreatCategory;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.LogEventSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SyscallSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for each {@link PayloadDecoder} (08-KAFKA.md §4 mapping table). */
class DecoderTest {

    @Test
    void logstashDecoderProducesLogEventSignal() throws Exception {
        String json = """
                {"@timestamp":"2026-05-09T10:00:00Z","level":"WARN","source":"sshd",
                 "host":"vault01","message":"Failed password for root from 10.0.0.5"}
                """;
        List<IInputSignal> out = new LogstashJsonDecoder().decode(
                "logs.security", null, json.getBytes(StandardCharsets.UTF_8), "SEC.AUTH");
        assertEquals(1, out.size());
        LogEventSignal s = (LogEventSignal) out.get(0);
        assertEquals(LogLevel.WARN, s.getLevel());
        assertEquals("sshd", s.getSource());
        assertTrue(s.getFields().containsKey("message"));
        assertEquals("SEC.AUTH", s.getFields().get("_tag"));
    }

    @Test
    void logstashDecoderRejectsMalformedJson() {
        PayloadDecoder d = new LogstashJsonDecoder();
        assertThrows(PayloadDecoder.DecoderException.class,
                () -> d.decode("logs.security", null, "not json{".getBytes(), null));
    }

    @Test
    void zeekConnDecoderProducesPacketSignal() throws Exception {
        String json = """
                {"ts":1715250000.123,"uid":"C123","id.orig_h":"10.0.0.5","id.resp_h":"8.8.8.8",
                 "id.orig_p":50123,"id.resp_p":443,"proto":"tcp","duration":0.5}
                """;
        List<IInputSignal> out = new ZeekConnDecoder().decode(
                "net.zeek.conn", null, json.getBytes(StandardCharsets.UTF_8), "NET.CONN");
        assertEquals(1, out.size());
        PacketSignal p = (PacketSignal) out.get(0);
        assertEquals("10.0.0.5", p.getTuple().getSrcIp());
        assertEquals("8.8.8.8", p.getTuple().getDstIp());
        assertEquals(50123, p.getTuple().getSrcPort());
        assertEquals(443, p.getTuple().getDstPort());
        assertEquals("tcp", p.getTuple().getProto());
        assertNotNull(p.getSummary());
    }

    @Test
    void zeekConnDecoderFailsWithoutTuple() {
        String json = "{\"ts\":1.0,\"id.resp_h\":\"x\"}";
        PayloadDecoder d = new ZeekConnDecoder();
        assertThrows(PayloadDecoder.DecoderException.class,
                () -> d.decode("net.zeek.conn", null, json.getBytes(), null));
    }

    @Test
    void suricataEveDecoderEmitsSignatureAndHypothesis() throws Exception {
        String json = """
                {"event_type":"alert","src_ip":"10.0.0.5","dest_ip":"1.2.3.4",
                 "alert":{"signature_id":"2025001","category":"trojan-activity",
                          "signature":"ET TROJAN test","severity":1}}
                """;
        List<IInputSignal> out = new SuricataEveDecoder().decode(
                "net.suricata.alert", null, json.getBytes(StandardCharsets.UTF_8), "NET.IDS");
        assertEquals(2, out.size());
        SignatureMatchSignal sm = (SignatureMatchSignal) out.get(0);
        assertEquals("2025001", sm.getSignatureId());
        assertTrue(sm.getConfidence() > 0.8);

        ThreatHypothesisSignal th = (ThreatHypothesisSignal) out.get(1);
        assertEquals(ThreatCategory.EXECUTION, th.getCategory());
    }

    @Test
    void suricataEveDecoderSkipsNonAlertEvents() throws Exception {
        // event_type=flow is valid but isn't a signal source.
        String json = "{\"event_type\":\"flow\",\"src_ip\":\"x\"}";
        List<IInputSignal> out = new SuricataEveDecoder().decode(
                "net.suricata.alert", null, json.getBytes(StandardCharsets.UTF_8), null);
        assertTrue(out.isEmpty());
    }

    @Test
    void osqueryDecoderProducesSyscallSignal() throws Exception {
        String json = """
                {"name":"process_events","columns":{
                  "syscall":"59","pid":"12345","path":"/usr/bin/curl",
                  "args":[1,2,3]}}
                """;
        List<IInputSignal> out = new OsqueryDecoder().decode(
                "host.syscalls.osquery", null, json.getBytes(StandardCharsets.UTF_8), "HOST.SYS");
        assertEquals(1, out.size());
        SyscallSignal s = (SyscallSignal) out.get(0);
        assertEquals(59, s.getSyscallNum());
        assertEquals(12345, s.getPid());
        assertEquals("/usr/bin/curl", s.getProcName());
        assertArrayEquals(new long[]{1L, 2L, 3L}, s.getArgs());
    }

    @Test
    void anomalyJsonDecoderProducesAnomalyScoreSignal() throws Exception {
        String json = """
                {"entityId":"host-007","score":0.83,"features":["unusual_login_time","new_geo"]}
                """;
        List<IInputSignal> out = new AnomalyScoreJsonDecoder().decode(
                "endpoint.scores", null, json.getBytes(StandardCharsets.UTF_8), null);
        assertEquals(1, out.size());
        AnomalyScoreSignal a = (AnomalyScoreSignal) out.get(0);
        assertEquals("host-007", a.getEntityId());
        assertEquals(0.83, a.getDeviationScore(), 1e-9);
        assertEquals(2, a.getContributingFeatures().size());
    }

    @Test
    void avroDecoderDelegatesToWrappedJsonDecoder() throws Exception {
        // The "avro bytes" can be anything — the converter just hands canned JSON
        // to the wrapped decoder. Tests deployments without Confluent jars.
        AvroSchemaRegistryDecoder.AvroToJsonConverter conv =
                (topic, value) -> "{\"entityId\":\"e1\",\"score\":0.5}".getBytes();
        AvroSchemaRegistryDecoder d = new AvroSchemaRegistryDecoder(
                conv, new AnomalyScoreJsonDecoder());
        List<IInputSignal> out = d.decode("endpoint.scores", null, new byte[]{0x00}, null);
        assertEquals(1, out.size());
        assertEquals("e1", ((AnomalyScoreSignal) out.get(0)).getEntityId());
    }
}
