/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.AcuityNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.AlertSeverity;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.ClinicalConsequenceModelNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.ContraindicationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.DifferentialDiagnosisNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.DrugInteractionMemoryNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.FindingCategory;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IAcuityNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IClinicalConsequenceModelNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IContraindicationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IDifferentialDiagnosisNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IDrugInteractionMemoryNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IPatientContextNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.ITrendDetectorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IVitalMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IWaveformAnalysisNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.PatientContextNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.Sex;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.TrendDetectorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.VitalMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.VitalType;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.WaveformAnalysisNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.WaveformType;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ImagingFindingSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.LabResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.MedicationAdminSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.WaveformSignal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalProcessorsTest {

    // ---------- VitalMonitorProcessor ----------

    @Test
    void vitalMonitorProcessor_emitsAlertOnExcursion() {
        VitalMonitorProcessor p = new VitalMonitorProcessor();
        IVitalMonitorNeuron n = new VitalMonitorNeuron();
        List<ISignal> out = p.process(new VitalSignal(VitalType.HR, 250, 0L, "p"), n);
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof AdverseEventAlertSignal);
        assertEquals(AlertSeverity.CRITICAL, ((AdverseEventAlertSignal) out.get(0)).getSeverity());
    }

    @Test
    void vitalMonitorProcessor_silentOnNormal() {
        VitalMonitorProcessor p = new VitalMonitorProcessor();
        IVitalMonitorNeuron n = new VitalMonitorNeuron();
        List<ISignal> out = p.process(new VitalSignal(VitalType.HR, 80, 0L, "p"), n);
        assertTrue(out.isEmpty());
    }

    @Test
    void vitalMonitorProcessor_metadata() {
        VitalMonitorProcessor p = new VitalMonitorProcessor();
        assertEquals(IVitalMonitorNeuron.class, p.getNeuronClass());
        assertEquals(VitalSignal.class, p.getSignalClass());
        assertFalse(p.hasMerger());
        assertNotNull(p.getDescription());
    }

    @Test
    void vitalMonitorProcessor_nullSafe() {
        VitalMonitorProcessor p = new VitalMonitorProcessor();
        assertTrue(p.process(null, new VitalMonitorNeuron()).isEmpty());
        assertTrue(p.process(new VitalSignal(VitalType.HR, 80, 0L, "p"), null).isEmpty());
    }

    // ---------- VitalTrendProcessor ----------

    @Test
    void vitalTrendProcessor_feedsWindow() {
        VitalTrendProcessor p = new VitalTrendProcessor();
        ITrendDetectorNeuron n = new TrendDetectorNeuron();
        for (int i = 0; i < 5; i++) {
            assertTrue(p.process(new VitalSignal(VitalType.HR, 60 + i, i, "p"), n).isEmpty());
        }
        assertEquals(5, n.samplesFor(VitalType.HR));
    }

    @Test
    void vitalTrendProcessor_metadata() {
        VitalTrendProcessor p = new VitalTrendProcessor();
        assertEquals(ITrendDetectorNeuron.class, p.getNeuronClass());
        assertEquals(VitalSignal.class, p.getSignalClass());
    }

    // ---------- VitalAcuityProcessor ----------

    @Test
    void vitalAcuityProcessor_updatesScore() {
        VitalAcuityProcessor p = new VitalAcuityProcessor();
        IAcuityNeuron n = new AcuityNeuron();
        p.process(new VitalSignal(VitalType.HR, 35, 0L, "p"), n);
        p.process(new VitalSignal(VitalType.SPO2, 80, 0L, "p"), n);
        assertTrue(n.rawScore() > 0);
    }

    @Test
    void vitalAcuityProcessor_metadata() {
        VitalAcuityProcessor p = new VitalAcuityProcessor();
        assertEquals(IAcuityNeuron.class, p.getNeuronClass());
        assertEquals(VitalSignal.class, p.getSignalClass());
    }

    // ---------- WaveformAnalysisProcessor ----------

    @Test
    void waveformAnalysisProcessor_emitsAsystoleAlert() {
        WaveformAnalysisProcessor p = new WaveformAnalysisProcessor();
        IWaveformAnalysisNeuron n = new WaveformAnalysisNeuron();
        WaveformSignal w = new WaveformSignal(WaveformType.ECG, new double[100], 250.0, "p");
        List<ISignal> out = p.process(w, n);
        assertEquals(1, out.size());
        assertEquals("ECG_ASYSTOLE_LIKELY", ((AdverseEventAlertSignal) out.get(0)).getEventCode());
    }

    @Test
    void waveformAnalysisProcessor_metadata() {
        WaveformAnalysisProcessor p = new WaveformAnalysisProcessor();
        assertEquals(IWaveformAnalysisNeuron.class, p.getNeuronClass());
        assertEquals(WaveformSignal.class, p.getSignalClass());
    }

    // ---------- DemographicContextProcessor ----------

    @Test
    void demographicContextProcessor_updatesContext() {
        DemographicContextProcessor p = new DemographicContextProcessor();
        IPatientContextNeuron n = new PatientContextNeuron();
        p.process(new DemographicSignal(80, Sex.FEMALE,
                Arrays.asList("N18.6"), Arrays.asList("BETA_LACTAM_ANAPHYLAXIS"), "p1"), n);
        assertEquals(80, n.getAgeYears());
        assertTrue(n.hasComorbidity("N18.6"));
        assertTrue(n.hasAllergy("BETA_LACTAM_ANAPHYLAXIS"));
        assertTrue(n.getVulnerabilityFactor() > 1.0);
    }

    @Test
    void demographicContextProcessor_metadata() {
        DemographicContextProcessor p = new DemographicContextProcessor();
        assertEquals(IPatientContextNeuron.class, p.getNeuronClass());
        assertEquals(DemographicSignal.class, p.getSignalClass());
    }

    // ---------- MedicationRegimenProcessor ----------

    @Test
    void medicationRegimenProcessor_addsActiveDrug() {
        MedicationRegimenProcessor p = new MedicationRegimenProcessor();
        IDrugInteractionMemoryNeuron n = new DrugInteractionMemoryNeuron();
        p.process(new MedicationAdminSignal("WARFARIN", 5, "mg", "po", 0L, "p1"), n);
        assertTrue(n.getActive().contains("WARFARIN"));
    }

    @Test
    void medicationRegimenProcessor_skipsNullCode() {
        MedicationRegimenProcessor p = new MedicationRegimenProcessor();
        IDrugInteractionMemoryNeuron n = new DrugInteractionMemoryNeuron();
        p.process(new MedicationAdminSignal(null, 5, "mg", "po", 0L, "p1"), n);
        assertTrue(n.getActive().isEmpty());
    }

    @Test
    void medicationRegimenProcessor_metadata() {
        MedicationRegimenProcessor p = new MedicationRegimenProcessor();
        assertEquals(IDrugInteractionMemoryNeuron.class, p.getNeuronClass());
        assertEquals(MedicationAdminSignal.class, p.getSignalClass());
    }

    // ---------- LabEvidenceProcessor ----------

    @Test
    void labEvidenceProcessor_updatesPosteriorOnAbnormal() {
        LabEvidenceProcessor p = new LabEvidenceProcessor();
        IDifferentialDiagnosisNeuron n = new DifferentialDiagnosisNeuron();
        n.seed("J18.9");
        n.seed("I21.9");
        // analyteCode "WBC:J18.9" routes the LR to the pneumonia candidate
        p.process(new LabResultSignal("WBC:J18.9", 18.0, "10^9/L",
                new double[]{4, 11}, 0L, "p1"), n);
        assertTrue(n.posteriorOf("J18.9") > n.posteriorOf("I21.9"));
    }

    @Test
    void labEvidenceProcessor_skipsWhenNoIcdHint() {
        LabEvidenceProcessor p = new LabEvidenceProcessor();
        IDifferentialDiagnosisNeuron n = new DifferentialDiagnosisNeuron();
        n.seed("J18.9");
        double before = n.posteriorOf("J18.9");
        p.process(new LabResultSignal("WBC", 18.0, "10^9/L",
                new double[]{4, 11}, 0L, "p1"), n);
        assertEquals(before, n.posteriorOf("J18.9"), 1e-9);
    }

    @Test
    void labEvidenceProcessor_metadata() {
        LabEvidenceProcessor p = new LabEvidenceProcessor();
        assertEquals(IDifferentialDiagnosisNeuron.class, p.getNeuronClass());
        assertEquals(LabResultSignal.class, p.getSignalClass());
    }

    // ---------- ImagingEvidenceProcessor ----------

    @Test
    void imagingEvidenceProcessor_criticalRaisesPosterior() {
        ImagingEvidenceProcessor p = new ImagingEvidenceProcessor();
        IDifferentialDiagnosisNeuron n = new DifferentialDiagnosisNeuron();
        n.seed("J93.9");   // pneumothorax
        n.seed("J18.9");
        p.process(new ImagingFindingSignal("X-RAY", "CHEST|J93.9",
                FindingCategory.CRITICAL, 0.9, "p1"), n);
        assertTrue(n.posteriorOf("J93.9") > n.posteriorOf("J18.9"));
    }

    @Test
    void imagingEvidenceProcessor_normalReducesPosterior() {
        ImagingEvidenceProcessor p = new ImagingEvidenceProcessor();
        IDifferentialDiagnosisNeuron n = new DifferentialDiagnosisNeuron();
        n.seed("J93.9");
        n.seed("J18.9");
        double before = n.posteriorOf("J93.9");
        p.process(new ImagingFindingSignal("X-RAY", "CHEST|J93.9",
                FindingCategory.NORMAL, 0.9, "p1"), n);
        assertTrue(n.posteriorOf("J93.9") < before);
    }

    @Test
    void imagingEvidenceProcessor_metadata() {
        ImagingEvidenceProcessor p = new ImagingEvidenceProcessor();
        assertEquals(IDifferentialDiagnosisNeuron.class, p.getNeuronClass());
        assertEquals(ImagingFindingSignal.class, p.getSignalClass());
    }

    // ---------- ContraindicationProcessor ----------

    @Test
    void contraindicationProcessor_emitsVetoOnAllergyMatch() {
        IPatientContextNeuron ctx = new PatientContextNeuron();
        ctx.update(new DemographicSignal(40, Sex.MALE, null,
                Arrays.asList("BETA_LACTAM_ANAPHYLAXIS"), "p1"));
        IContraindicationNeuron filter = new ContraindicationNeuron();
        filter.setContext(ctx);

        ContraindicationProcessor p = new ContraindicationProcessor();
        List<ISignal> out = p.process(new TreatmentProposalSignal("AMOXICILLIN",
                0.7, 0.2, "advisory", "p1"), filter);
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof ClinicalVetoSignal);
    }

    @Test
    void contraindicationProcessor_silentOnSafeProposal() {
        IPatientContextNeuron ctx = new PatientContextNeuron();
        ctx.update(new DemographicSignal(40, Sex.MALE, null, null, "p1"));
        IContraindicationNeuron filter = new ContraindicationNeuron();
        filter.setContext(ctx);

        ContraindicationProcessor p = new ContraindicationProcessor();
        List<ISignal> out = p.process(new TreatmentProposalSignal("ACETAMINOPHEN",
                0.5, 0.1, "advisory", "p1"), filter);
        assertTrue(out.isEmpty());
    }

    @Test
    void contraindicationProcessor_metadata() {
        ContraindicationProcessor p = new ContraindicationProcessor();
        assertEquals(IContraindicationNeuron.class, p.getNeuronClass());
        assertEquals(TreatmentProposalSignal.class, p.getSignalClass());
    }

    // ---------- consequence-model neuron is interface-typed in collaborators ----------

    @Test
    void treatmentPlanning_acceptsInterfaceTypedConsequenceModel() {
        IClinicalConsequenceModelNeuron model = new ClinicalConsequenceModelNeuron();
        com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.TreatmentPlanningNeuron planner =
                new com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.TreatmentPlanningNeuron(model);
        assertNotNull(planner);
    }
}
