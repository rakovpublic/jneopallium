/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 *
 * Live narrated demo of the Self-Supervised Maintenance Guardian. It loads the
 * deployed, label-free fitted model and a telemetry replay, runs both through
 * the REAL Java neurons, and prints a readable transcript of:
 *   1. a label-free maintenance advisory raised on a degrading asset;
 *   2. nuisance advisories on a healthy asset;
 *   3. operator feedback adapting the thresholds live — no redeploy — so the
 *      nuisances are suppressed while the real fault is still caught.
 *
 * Usage: java ... SelfSupervisedMaintenanceDemo <fitted-model.json> <replay.json>
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.CrossSensorReconstructionNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.FeedbackAdaptationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.MaintenanceHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.SsAdvisoryGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.AssetTelemetrySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.HealthHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.MaintenanceAdvisorySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.OperatorFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ReconResidualSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SelfSupervisedMaintenanceDemo {

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);   // stable "1.23" formatting for the transcript
        String fittedPath = args.length > 0 ? args[0]
                : "worker/src/main/resources/model/self-supervised-maintenance/fitted-model.json";
        String replayPath = args.length > 1 ? args[1]
                : "target/jneopallium-ss-maintenance-demo/replay.json";

        ObjectMapper om = new ObjectMapper();
        JsonNode fitted = om.readTree(new File(fittedPath));

        List<String> sensors = new ArrayList<>();
        for (JsonNode s : fitted.get("sensors")) sensors.add(s.asText());
        int S = sensors.size();

        double[][] means = matrix(fitted.get("regimeMeans"));
        double[][] stds = matrix(fitted.get("regimeStds"));
        double[][] cross = new double[S][];
        JsonNode cw = fitted.get("crossWeights");
        for (int i = 0; i < S; i++) cross[i] = vector(cw.get(sensors.get(i)));
        JsonNode base = fitted.get("healthBaseline");
        double bMean = base.get("mean").asDouble();
        double bP99 = base.get("p99").asDouble();
        double bP999 = base.get("p999").asDouble();

        // ---- the deployed model, exactly as the runtime builds it ----
        CrossSensorReconstructionNeuron recon = new CrossSensorReconstructionNeuron();
        recon.setSensorOrder(sensors);
        recon.setRegimeMeans(means);
        recon.setRegimeStds(stds);
        recon.setCrossWeights(cross);

        MaintenanceHypothesisNeuron hyp = new MaintenanceHypothesisNeuron();
        hyp.setBaseline(bMean, bP99, bP999);

        FeedbackAdaptationNeuron fb = new FeedbackAdaptationNeuron();
        SsAdvisoryGateNeuron gate = new SsAdvisoryGateNeuron();
        gate.setDefaultThreshold(1.0);
        gate.setDeduplicationTicks(60L);

        JsonNode replay = om.readTree(new File(replayPath));

        banner();
        System.out.printf("Loaded fitted model: %d sensors, baseline p99=%.2f p999=%.2f%n",
                S, bP99, bP999);
        System.out.printf("Replaying %d telemetry frames through the live neurons...%n%n",
                replay.size());

        int healthyAdvisories = 0, healthySuppressedAfterLearning = 0, faultAdvisories = 0;
        boolean learned = false;

        for (JsonNode f : replay) {
            String asset = f.get("asset").asText();
            long tick = f.get("tick").asLong();
            int regime = f.get("regime").asInt();
            Map<String, Double> sv = new LinkedHashMap<>();
            JsonNode sn = f.get("sensors");
            for (String s : sensors) sv.put(s, sn.get(s).asDouble());

            ReconResidualSignal r = recon.reconstruct(new AssetTelemetrySignal(asset, regime, sv, tick));
            HealthHypothesisSignal h = hyp.assess(r);
            boolean healthy = asset.equals("PUMP-101");

            // note whether a healthy nuisance would have fired under the ORIGINAL
            // threshold but is now suppressed by the learned one
            if (healthy && learned && h.getEvidence() >= 1.0
                    && h.getEvidence() < gate.thresholdFor(h.getFaultFamily())) {
                healthySuppressedAfterLearning++;
            }

            MaintenanceAdvisorySignal adv = gate.gate(h);
            if (adv == null) continue;

            if (healthy) {
                healthyAdvisories++;
                System.out.printf("t=%d  ADVISORY  %-9s %-14s sev=%.2f evidence=%.2f  (healthy asset)%n",
                        tick, adv.getAssetId(), adv.getFaultFamily(), adv.getSeverity(), h.getEvidence());
                ThresholdUpdateSignal u = fb.onFeedback(new OperatorFeedbackSignal(
                        asset, adv.getFaultFamily(), false, h.getDomainShift(), "operator", tick));
                if (u != null) {
                    gate.onThresholdUpdate(u);
                    learned = true;
                    System.out.printf("          operator -> FALSE POSITIVE. "
                            + "threshold[%s] raised to %.2f (learned live, no redeploy)%n",
                            u.getFaultFamily(), u.getThreshold());
                }
            } else {
                faultAdvisories++;
                System.out.printf("t=%d  ADVISORY  %-9s %-14s sev=%.2f evidence=%.2f lead~%d ticks unc=%.2f%n",
                        tick, adv.getAssetId(), adv.getFaultFamily(), adv.getSeverity(),
                        h.getEvidence(), adv.getLeadTimeTicks(), adv.getUncertainty());
                ThresholdUpdateSignal u = fb.onFeedback(new OperatorFeedbackSignal(
                        asset, adv.getFaultFamily(), true, h.getDomainShift(), "operator", tick));
                if (u != null) {
                    gate.onThresholdUpdate(u);
                    System.out.printf("          operator -> CONFIRMED. work order raised; "
                            + "threshold[%s] kept keen at %.2f%n", u.getFaultFamily(), u.getThreshold());
                }
            }
        }

        System.out.println();
        System.out.println("---------------------------------------------------------------");
        System.out.printf("Real fault (PUMP-102) advisories raised ... %d%n", faultAdvisories);
        System.out.printf("Healthy nuisance advisories (before learning) %d%n", healthyAdvisories);
        System.out.printf("Healthy nuisances suppressed after feedback . %d%n", healthySuppressedAfterLearning);
        System.out.printf("Final bearing_damage threshold .............. %.2f (started 1.00)%n",
                gate.thresholdFor("bearing_damage"));
        System.out.println("Safety posture .............................. ADVISORY (never actuates)");
        System.out.println("---------------------------------------------------------------");
    }

    private static double[][] matrix(JsonNode n) {
        double[][] m = new double[n.size()][];
        for (int i = 0; i < n.size(); i++) m[i] = vector(n.get(i));
        return m;
    }

    private static double[] vector(JsonNode n) {
        double[] v = new double[n.size()];
        for (int i = 0; i < n.size(); i++) v[i] = n.get(i).asDouble();
        return v;
    }

    private static void banner() {
        System.out.println("===============================================================");
        System.out.println(" Self-Supervised Maintenance Guardian - live demo");
        System.out.println(" Label-free detection + continuous learning (advisory only)");
        System.out.println("===============================================================");
    }
}
