/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DiagnosisHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ImagingFindingSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.LabResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.MedicationAdminSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.WaveformSignal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalModuleTest {

    // ---------- enums: cardinality ----------

    @Test
    void vitalType_has6() { assertEquals(6, VitalType.values().length); }

    @Test
    void waveformType_has3() { assertEquals(3, WaveformType.values().length); }

    @Test
    void findingCategory_has5() { assertEquals(5, FindingCategory.values().length); }

    @Test
    void sex_has4() { assertEquals(4, Sex.values().length); }

    @Test
    void alertSeverity_has4() { assertEquals(4, AlertSeverity.values().length); }

    // ---------- signals: ProcessingFrequency ----------

    @Test
    void vitalSignal_isLoop1Epoch1() {
        assertEquals(1L, VitalSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, VitalSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void waveformSignal_isLoop1Epoch1() {
        assertEquals(1L, WaveformSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, WaveformSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void labResultSignal_isLoop2Epoch3() {
        assertEquals(3L, LabResultSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, LabResultSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void imagingFindingSignal_isLoop2Epoch5() {
        assertEquals(5L, ImagingFindingSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, ImagingFindingSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void medicationAdminSignal_isLoop2Epoch2() {
        assertEquals(2L, MedicationAdminSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, MedicationAdminSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void demographicSignal_isLoop2Epoch10() {
        assertEquals(10L, DemographicSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(2, DemographicSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void diagnosisHypothesisSignal_isLoop1Epoch2() {
        assertEquals(2L, DiagnosisHypothesisSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, DiagnosisHypothesisSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void treatmentProposalSignal_isLoop1Epoch3() {
        assertEquals(3L, TreatmentProposalSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, TreatmentProposalSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void adverseEventAlertSignal_isLoop1Epoch1() {
        assertEquals(1L, AdverseEventAlertSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, AdverseEventAlertSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void clinicalVetoSignal_isLoop1Epoch1() {
        assertEquals(1L, ClinicalVetoSignal.PROCESSING_FREQUENCY.getEpoch());
        assertEquals(1, ClinicalVetoSignal.PROCESSING_FREQUENCY.getLoop());
    }

    @Test
    void clinicalVetoSignal_extendsHarmVetoSignal() {
        ClinicalVetoSignal c = new ClinicalVetoSignal("plan-1", "too dangerous",
                HarmVerdict.HARMFUL, "AHA-2023-12-5", Arrays.asList("ALT1"), "p1");
        assertTrue(c instanceof HarmVetoSignal, "ClinicalVetoSignal must extend HarmVetoSignal");
        assertEquals("AHA-2023-12-5", c.getGuidelineCitation());
        assertEquals("p1", c.getPatientId());
        assertEquals(1, c.getAlternativeCodes().size());
    }

    @Test
    void clinicalVetoSignal_copyPreservesFields() {
        ClinicalVetoSignal c = new ClinicalVetoSignal("plan-1", "x",
                HarmVerdict.CATASTROPHIC, "cite", Arrays.asList("A", "B"), "p9");
        ClinicalVetoSignal d = (ClinicalVetoSignal) c.copySignal();
        assertEquals("plan-1", d.getActionPlanId());
        assertEquals("cite", d.getGuidelineCitation());
        assertEquals(2, d.getAlternativeCodes().size());
        assertEquals("p9", d.getPatientId());
    }

    @Test
    void labResult_abnormalDetection() {
        LabResultSignal l = new LabResultSignal("2345-7", 250.0, "mg/dL",
                new double[]{70, 100}, 0L, "p");
        assertTrue(l.isAbnormal());
    }

    @Test
    void demographic_pediatricGeriatricFlags() {
        DemographicSignal ped = new DemographicSignal(5, Sex.FEMALE, null, null, "p");
        DemographicSignal ger = new DemographicSignal(80, Sex.MALE, null, null, "p");
        DemographicSignal adult = new DemographicSignal(40, Sex.MALE, null, null, "p");
        assertTrue(ped.isPediatric());
        assertFalse(ped.isGeriatric());
        assertTrue(ger.isGeriatric());
        assertFalse(ger.isPediatric());
        assertFalse(adult.isPediatric());
        assertFalse(adult.isGeriatric());
    }

    // ---------- VitalMonitorNeuron ----------

    @Test
    void vitalMonitor_noAlertInBand() {
        VitalMonitorNeuron v = new VitalMonitorNeuron();
        AdverseEventAlertSignal a = v.observe(new VitalSignal(VitalType.HR, 80, 0L, "p"));
        assertNull(a);
    }

    @Test
    void vitalMonitor_criticalOnFarExcursion() {
        VitalMonitorNeuron v = new VitalMonitorNeuron();
        AdverseEventAlertSignal a = v.observe(new VitalSignal(VitalType.HR, 250, 0L, "p"));
        assertNotNull(a);
        assertEquals(AlertSeverity.CRITICAL, a.getSeverity());
        assertTrue(a.getEventCode().startsWith("HR_"));
    }

    @Test
    void vitalMonitor_warningOnMildExcursion() {
        VitalMonitorNeuron v = new VitalMonitorNeuron();
        AdverseEventAlertSignal a = v.observe(new VitalSignal(VitalType.SPO2, 83, 0L, "p"));
        assertNotNull(a);
        assertEquals(AlertSeverity.WARNING, a.getSeverity());
    }

    @Test
    void vitalMonitor_customGuardrail() {
        VitalMonitorNeuron v = new VitalMonitorNeuron();
        v.setGuardrail(VitalType.HR, 60, 90);
        assertNotNull(v.observe(new VitalSignal(VitalType.HR, 95, 0L, "p")));
    }

    // ---------- WaveformAnalysisNeuron ----------

    @Test
    void waveform_detectsAsystoleFromFlatEcg() {
        WaveformAnalysisNeuron w = new WaveformAnalysisNeuron();
        double[] flat = new double[100];
        AdverseEventAlertSignal a = w.analyse(new WaveformSignal(WaveformType.ECG, flat, 250.0, "p"));
        assertNotNull(a);
        assertEquals(AlertSeverity.CRITICAL, a.getSeverity());
        assertEquals("ECG_ASYSTOLE_LIKELY", a.getEventCode());
    }

    @Test
    void waveform_vfSuspectedOnHighZcr() {
        WaveformAnalysisNeuron w = new WaveformAnalysisNeuron();
        double[] noisy = new double[200];
        for (int i = 0; i < noisy.length; i++) noisy[i] = (i % 2 == 0) ? 1.0 : -1.0;
        AdverseEventAlertSignal a = w.analyse(new WaveformSignal(WaveformType.ECG, noisy, 250.0, "p"));
        assertNotNull(a);
        assertEquals(AlertSeverity.CRITICAL, a.getSeverity());
        assertEquals("ECG_VF_SUSPECTED", a.getEventCode());
    }

    @Test
    void waveform_normalEcgProducesNothing() {
        WaveformAnalysisNeuron w = new WaveformAnalysisNeuron();
        double[] s = new double[200];
        for (int i = 0; i < s.length; i++) s[i] = Math.sin(i * 0.1);
        AdverseEventAlertSignal a = w.analyse(new WaveformSignal(WaveformType.ECG, s, 250.0, "p"));
        assertNull(a);
    }

    // ---------- TrendDetectorNeuron ----------

    @Test
    void trend_risingDetected() {
        TrendDetectorNeuron t = new TrendDetectorNeuron();
        t.setSlopeUpThreshold(0.1);
        TrendDetectorNeuron.Trend last = TrendDetectorNeuron.Trend.FLAT;
        for (int i = 0; i < 10; i++) {
            last = t.observe(new VitalSignal(VitalType.HR, 60 + i * 5, i, "p"));
        }
        assertEquals(TrendDetectorNeuron.Trend.UP, last);
    }

    @Test
    void trend_fallingDetected() {
        TrendDetectorNeuron t = new TrendDetectorNeuron();
        t.setSlopeUpThreshold(0.1);
        TrendDetectorNeuron.Trend last = TrendDetectorNeuron.Trend.FLAT;
        for (int i = 0; i < 10; i++) {
            last = t.observe(new VitalSignal(VitalType.HR, 100 - i * 5, i, "p"));
        }
        assertEquals(TrendDetectorNeuron.Trend.DOWN, last);
    }

    @Test
    void trend_flatOnConstant() {
        TrendDetectorNeuron t = new TrendDetectorNeuron();
        TrendDetectorNeuron.Trend last = TrendDetectorNeuron.Trend.FLAT;
        for (int i = 0; i < 10; i++) {
            last = t.observe(new VitalSignal(VitalType.HR, 80, i, "p"));
        }
        assertEquals(TrendDetectorNeuron.Trend.FLAT, last);
    }

    // ---------- PatientContextNeuron ----------

    @Test
    void patientContext_pediatricRaisesVulnerability() {
        PatientContextNeuron p = new PatientContextNeuron();
        p.update(new DemographicSignal(1, Sex.MALE, null, null, "p1"));
        assertTrue(p.getVulnerabilityFactor() > 1.9);
    }

    @Test
    void patientContext_geriatricRaisesVulnerability() {
        PatientContextNeuron p = new PatientContextNeuron();
        p.update(new DemographicSignal(85, Sex.FEMALE, null, null, "p1"));
        assertTrue(p.getVulnerabilityFactor() >= 1.7);
    }

    @Test
    void patientContext_pregnantFlagged() {
        PatientContextNeuron p = new PatientContextNeuron();
        p.update(new DemographicSignal(30, Sex.FEMALE, Arrays.asList("Z34.90"), null, "p1"));
        assertTrue(p.isPregnant());
        assertTrue(p.getVulnerabilityFactor() > 1.0);
    }

    @Test
    void patientContext_allergyLookupCaseInsensitive() {
        PatientContextNeuron p = new PatientContextNeuron();
        p.update(new DemographicSignal(30, Sex.MALE, null,
                Arrays.asList("BETA_LACTAM_ANAPHYLAXIS"), "p1"));
        assertTrue(p.hasAllergy("beta_lactam_anaphylaxis"));
        assertFalse(p.hasAllergy("SULFA"));
    }

    // ---------- AcuityNeuron ----------

    @Test
    void acuity_lowOnNormalVitals() {
        AcuityNeuron a = new AcuityNeuron();
        a.ingest(new VitalSignal(VitalType.HR, 70, 0L, "p"));
        a.ingest(new VitalSignal(VitalType.SPO2, 98, 0L, "p"));
        a.ingest(new VitalSignal(VitalType.BP_SYS, 120, 0L, "p"));
        a.ingest(new VitalSignal(VitalType.TEMP, 37.0, 0L, "p"));
        a.ingest(new VitalSignal(VitalType.RESP, 14, 0L, "p"));
        assertEquals(0, a.rawScore());
        assertEquals(1.0, a.harmThresholdMultiplier(), 1e-9);
    }

    @Test
    void acuity_highOnCriticalVitals() {
        AcuityNeuron a = new AcuityNeuron();
        a.ingest(new VitalSignal(VitalType.HR, 35, 0L, "p"));
        a.ingest(new VitalSignal(VitalType.SPO2, 80, 0L, "p"));
        a.ingest(new VitalSignal(VitalType.BP_SYS, 70, 0L, "p"));
        a.ingest(new VitalSignal(VitalType.RESP, 6, 0L, "p"));
        assertTrue(a.rawScore() >= 7);
        assertTrue(a.harmThresholdMultiplier() > 2.5);
    }

    // ---------- DifferentialDiagnosisNeuron ----------

    @Test
    void differential_bayesianUpdateSkewsPosterior() {
        DifferentialDiagnosisNeuron dx = new DifferentialDiagnosisNeuron();
        dx.seed("J18.9");   // pneumonia
        dx.seed("I21.9");   // MI
        dx.seed("J44.1");   // COPD exacerbation
        dx.update("J18.9", 5.0, "lab:elevated-wbc");
        Double p1 = dx.posteriorOf("J18.9");
        Double p2 = dx.posteriorOf("I21.9");
        assertNotNull(p1);
        assertNotNull(p2);
        assertTrue(p1 > p2);
    }

    @Test
    void differential_enforcesMaxCandidates() {
        DifferentialDiagnosisNeuron dx = new DifferentialDiagnosisNeuron();
        dx.setMaxCandidates(3);
        for (int i = 0; i < 10; i++) dx.seed("I" + i);
        assertTrue(dx.size() <= 3);
    }

    @Test
    void differential_thresholdFiltering() {
        DifferentialDiagnosisNeuron dx = new DifferentialDiagnosisNeuron();
        dx.setPosteriorThreshold(0.4);
        dx.seed("A");
        dx.seed("B");
        dx.seed("C");
        List<DiagnosisHypothesisSignal> r = dx.ranked();
        assertEquals(0, r.size(), "uniform prior below 0.4 threshold => empty ranked output");
    }

    // ---------- GuidelineMemoryNeuron ----------

    @Test
    void guideline_lookupAndCitation() {
        GuidelineMemoryNeuron g = new GuidelineMemoryNeuron();
        g.store(new GuidelineMemoryNeuron.Guideline("J18.9", "community-acquired pneumonia protocol",
                "IDSA-2019-12-3", Arrays.asList("AMOXICILLIN"),
                Arrays.asList("CEFTRIAXONE"), 1L));
        assertEquals("IDSA-2019-12-3", g.citeFor("J18.9"));
        assertTrue(g.isFirstLine("J18.9", "AMOXICILLIN"));
        assertTrue(g.isContraindicatedByGuideline("J18.9", "CEFTRIAXONE"));
    }

    // ---------- DrugInteractionMemoryNeuron ----------

    @Test
    void ddi_detectsHazard() {
        DrugInteractionMemoryNeuron d = new DrugInteractionMemoryNeuron();
        d.addInteraction(new DrugInteractionMemoryNeuron.Interaction(
                "WARFARIN", "ASPIRIN", 3, "additive bleeding", "FDA"));
        d.addActive("WARFARIN");
        assertEquals(1, d.hazardsFor("ASPIRIN").size());
        assertEquals(3, d.maxSeverityFor("ASPIRIN"));
        assertFalse(d.isContraindicatedWithRegimen("ASPIRIN"));
    }

    @Test
    void ddi_severityFourIsContraindication() {
        DrugInteractionMemoryNeuron d = new DrugInteractionMemoryNeuron();
        d.addInteraction(new DrugInteractionMemoryNeuron.Interaction(
                "MAOI", "MEPERIDINE", 4, "serotonin syndrome", "FDA"));
        d.addActive("MAOI");
        assertTrue(d.isContraindicatedWithRegimen("MEPERIDINE"));
    }

    // ---------- ClinicalConsequenceModelNeuron ----------

    @Test
    void consequenceModel_benefitScalesWithDose() {
        ClinicalConsequenceModelNeuron m = new ClinicalConsequenceModelNeuron();
        ClinicalConsequenceModelNeuron.PkPdParams p =
                new ClinicalConsequenceModelNeuron.PkPdParams(0.9, 0.7, 10, 0.9, 5.0, 40.0);
        ClinicalConsequenceModelNeuron.Forecast f1 = m.simulate(100, 70, p, 1.0);
        ClinicalConsequenceModelNeuron.Forecast f2 = m.simulate(500, 70, p, 1.0);
        assertTrue(f2.expectedBenefit >= f1.expectedBenefit);
    }

    @Test
    void consequenceModel_toxicDoseRaisesRisk() {
        ClinicalConsequenceModelNeuron m = new ClinicalConsequenceModelNeuron();
        ClinicalConsequenceModelNeuron.PkPdParams p =
                new ClinicalConsequenceModelNeuron.PkPdParams(0.9, 0.3, 5, 0.9, 2.0, 3.0);
        ClinicalConsequenceModelNeuron.Forecast tox = m.simulate(10_000, 70, p, 1.5);
        assertTrue(tox.toxicityRisk > 0);
        assertTrue(tox.expectedRisk > 0);
    }

    // ---------- TreatmentPlanningNeuron ----------

    @Test
    void treatmentPlanning_returnsAdvisorySignals() {
        TreatmentPlanningNeuron planner = new TreatmentPlanningNeuron();
        planner.setPatientId("p1");
        ClinicalConsequenceModelNeuron.PkPdParams p =
                new ClinicalConsequenceModelNeuron.PkPdParams(0.9, 0.5, 5, 0.9, 1.0, 8.0);
        List<TreatmentPlanningNeuron.Candidate> cands = new ArrayList<>();
        cands.add(new TreatmentPlanningNeuron.Candidate("AMOXICILLIN", 500, p, "J18.9"));
        cands.add(new TreatmentPlanningNeuron.Candidate("CEFTRIAXONE", 1000, p, "J18.9"));
        List<TreatmentProposalSignal> out = planner.plan(cands);
        assertEquals(2, out.size());
        for (TreatmentProposalSignal sig : out) {
            assertNotNull(sig.getRationale());
            assertTrue(sig.getRationale().contains("advisory"));
            assertEquals("p1", sig.getPatientId());
        }
    }

    // ---------- ContraindicationNeuron ----------

    @Test
    void contraindication_allergyTriggersVeto() {
        PatientContextNeuron ctx = new PatientContextNeuron();
        ctx.update(new DemographicSignal(40, Sex.FEMALE, null,
                Arrays.asList("BETA_LACTAM_ANAPHYLAXIS"), "p1"));
        ContraindicationNeuron filter = new ContraindicationNeuron();
        filter.setContext(ctx);
        TreatmentProposalSignal p = new TreatmentProposalSignal("AMOXICILLIN",
                0.7, 0.2, "advisory only", "p1");
        ClinicalVetoSignal v = filter.evaluate(p);
        assertNotNull(v);
        assertEquals(HarmVerdict.CATASTROPHIC, v.getSeverity());
        assertTrue(v instanceof HarmVetoSignal);
        assertEquals("p1", v.getPatientId());
    }

    @Test
    void contraindication_comorbidityTriggersVeto() {
        PatientContextNeuron ctx = new PatientContextNeuron();
        ctx.update(new DemographicSignal(70, Sex.MALE, Arrays.asList("N18.6"),
                null, "p1"));
        ContraindicationNeuron filter = new ContraindicationNeuron();
        filter.setContext(ctx);
        TreatmentProposalSignal p = new TreatmentProposalSignal("IBUPROFEN",
                0.5, 0.3, "advisory only", "p1");
        ClinicalVetoSignal v = filter.evaluate(p);
        assertNotNull(v);
    }

    @Test
    void contraindication_pregnancyTriggersVeto() {
        PatientContextNeuron ctx = new PatientContextNeuron();
        ctx.update(new DemographicSignal(30, Sex.FEMALE, Arrays.asList("Z34.90"),
                null, "p1"));
        ContraindicationNeuron filter = new ContraindicationNeuron();
        filter.setContext(ctx);
        TreatmentProposalSignal p = new TreatmentProposalSignal("WARFARIN",
                0.5, 0.3, "advisory only", "p1");
        ClinicalVetoSignal v = filter.evaluate(p);
        assertNotNull(v);
    }

    @Test
    void contraindication_safeProposalPassesClean() {
        PatientContextNeuron ctx = new PatientContextNeuron();
        ctx.update(new DemographicSignal(40, Sex.MALE, null, null, "p1"));
        ContraindicationNeuron filter = new ContraindicationNeuron();
        filter.setContext(ctx);
        TreatmentProposalSignal p = new TreatmentProposalSignal("ACETAMINOPHEN",
                0.5, 0.1, "advisory only", "p1");
        assertNull(filter.evaluate(p));
    }

    @Test
    void contraindication_ddiFourIsVetoed() {
        DrugInteractionMemoryNeuron ddi = new DrugInteractionMemoryNeuron();
        ddi.addInteraction(new DrugInteractionMemoryNeuron.Interaction(
                "MAOI", "MEPERIDINE", 4, "serotonin syndrome", "FDA"));
        ddi.addActive("MAOI");
        ContraindicationNeuron filter = new ContraindicationNeuron();
        filter.setDrugInteractions(ddi);
        TreatmentProposalSignal p = new TreatmentProposalSignal("MEPERIDINE",
                0.5, 0.2, "advisory only", "p1");
        ClinicalVetoSignal v = filter.evaluate(p);
        assertNotNull(v);
    }

    // ---------- RecommendationNeuron ----------

    @Test
    void recommendation_isAdvisoryOnly() {
        RecommendationNeuron r = new RecommendationNeuron();
        assertEquals("advisory", r.getMode());
        assertEquals("advisory", RecommendationNeuron.MODE);
    }

    @Test
    void recommendation_vetoedCandidateExcludedFromRecommend() {
        PatientContextNeuron ctx = new PatientContextNeuron();
        ctx.update(new DemographicSignal(40, Sex.MALE, null,
                Arrays.asList("BETA_LACTAM_ANAPHYLAXIS"), "p1"));
        ContraindicationNeuron filter = new ContraindicationNeuron();
        filter.setContext(ctx);

        RecommendationNeuron rec = new RecommendationNeuron();
        rec.setContraindicationFilter(filter);
        rec.setTopK(5);

        List<TreatmentProposalSignal> cands = Arrays.asList(
                new TreatmentProposalSignal("AMOXICILLIN", 0.9, 0.1, "x", "p1"),
                new TreatmentProposalSignal("AZITHROMYCIN", 0.6, 0.2, "x", "p1"));

        List<RecommendationNeuron.Recommendation> out = rec.recommend(cands);
        assertEquals(1, out.size());
        assertEquals("AZITHROMYCIN", out.get(0).proposal.getRxNormOrProcedureCode());

        List<RecommendationNeuron.Recommendation> rejected = rec.rejected(cands);
        assertEquals(1, rejected.size());
        assertTrue(rejected.get(0).vetoed);
    }

    @Test
    void recommendation_sortedByBenefitMinusRisk() {
        RecommendationNeuron rec = new RecommendationNeuron();
        List<TreatmentProposalSignal> cands = Arrays.asList(
                new TreatmentProposalSignal("A", 0.3, 0.1, "x", "p1"),
                new TreatmentProposalSignal("B", 0.9, 0.1, "x", "p1"),
                new TreatmentProposalSignal("C", 0.6, 0.5, "x", "p1"));
        List<RecommendationNeuron.Recommendation> out = rec.recommend(cands);
        assertEquals(3, out.size());
        assertEquals("B", out.get(0).proposal.getRxNormOrProcedureCode());
        assertEquals("A", out.get(1).proposal.getRxNormOrProcedureCode());
    }

    // ---------- ClinicalConfig ----------

    @Test
    void config_modeCannotBeChangedToAutonomous() {
        ClinicalConfig c = new ClinicalConfig();
        assertEquals("advisory", c.getRecommendationMode());
        assertThrows(IllegalArgumentException.class, () -> c.setRecommendationMode("autonomous"));
        assertThrows(IllegalArgumentException.class, () -> c.setRecommendationMode(null));
    }

    @Test
    void config_confirmationCannotBeDisabled() {
        ClinicalConfig c = new ClinicalConfig();
        assertTrue(c.isRecommendationConfirmationRequired());
        assertThrows(IllegalArgumentException.class, () -> c.setRecommendationConfirmationRequired(false));
    }

    @Test
    void config_affectCuriositySleepDisabledByPolicy() {
        ClinicalConfig c = new ClinicalConfig();
        assertTrue(c.isAffectDisabled());
        assertTrue(c.isCuriosityDisabled());
        assertTrue(c.isSleepDisabled());
    }

    @Test
    void config_defaultGuardrailsMatchSpec() {
        ClinicalConfig c = new ClinicalConfig();
        assertArrayEquals(new double[]{40, 150}, c.getVitalGuardrails().get(VitalType.HR));
        assertArrayEquals(new double[]{88, 100}, c.getVitalGuardrails().get(VitalType.SPO2));
        assertArrayEquals(new double[]{80, 200}, c.getVitalGuardrails().get(VitalType.BP_SYS));
    }
}
