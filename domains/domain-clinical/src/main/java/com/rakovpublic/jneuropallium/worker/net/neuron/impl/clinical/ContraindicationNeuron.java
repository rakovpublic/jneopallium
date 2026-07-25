/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer 4 contraindication filter. Specialises the autonomous-AI
 * {@code EthicalPriorityNeuron} — it is a <b>hard</b> filter that emits
 * {@link ClinicalVetoSignal} (a {@code HarmVetoSignal} specialisation) on
 * a match. Examples: β-lactam class proposed for an anaphylaxis history;
 * nephrotoxic drug for end-stage renal disease; teratogen for pregnant
 * patient; DDI severity ≥ 4 against the current regimen.
 * Loop=1 / Epoch=1.
 */
public class ContraindicationNeuron extends ModulatableNeuron implements IContraindicationNeuron {

    /** Allergy-code → contraindicated RxNorm class/codes. */
    private final Map<String, List<String>> allergyRules = new HashMap<>();
    /** Comorbidity-code → contraindicated RxNorm class/codes. */
    private final Map<String, List<String>> comorbidityRules = new HashMap<>();
    /** Pregnancy-contraindicated RxNorm class/codes. */
    private final List<String> pregnancyContra = new ArrayList<>();

    private IPatientContextNeuron patient;
    private IDrugInteractionMemoryNeuron ddi;
    private IGuidelineMemoryNeuron guidelines;
    private String defaultCitation = "internal-rule";

    public ContraindicationNeuron() {
        super();
        seedDefaults();
    }

    public ContraindicationNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        seedDefaults();
    }

    /** Minimal conservative default rules; a deployment replaces these. */
    private void seedDefaults() {
        allergyRules.put("BETA_LACTAM_ANAPHYLAXIS",
                new ArrayList<>(java.util.Arrays.asList("PENICILLIN", "AMOXICILLIN", "CEFTRIAXONE", "CEFEPIME")));
        allergyRules.put("SULFA_ANAPHYLAXIS",
                new ArrayList<>(java.util.Arrays.asList("SULFAMETHOXAZOLE", "TMP_SMX")));
        comorbidityRules.put("N18.6",   // end-stage renal disease
                new ArrayList<>(java.util.Arrays.asList("NSAID", "IBUPROFEN", "NAPROXEN", "KETOROLAC")));
        comorbidityRules.put("K72.0",   // hepatic failure
                new ArrayList<>(java.util.Arrays.asList("ACETAMINOPHEN_HIGH")));
        pregnancyContra.addAll(java.util.Arrays.asList("WARFARIN", "ISOTRETINOIN", "METHOTREXATE", "ACE_INHIBITOR"));
    }

    public void setContext(IPatientContextNeuron p) { this.patient = p; }
    public void setDrugInteractions(IDrugInteractionMemoryNeuron d) { this.ddi = d; }
    public void setGuidelines(IGuidelineMemoryNeuron g) { this.guidelines = g; }
    public void setDefaultCitation(String c) { this.defaultCitation = c == null ? "internal-rule" : c; }

    public void addAllergyRule(String allergyCode, String rxContraindicated) {
        allergyRules.computeIfAbsent(allergyCode, k -> new ArrayList<>()).add(rxContraindicated);
    }

    public void addComorbidityRule(String icd10, String rxContraindicated) {
        comorbidityRules.computeIfAbsent(icd10, k -> new ArrayList<>()).add(rxContraindicated);
    }

    public void addPregnancyContraindication(String rxContraindicated) {
        if (rxContraindicated != null) pregnancyContra.add(rxContraindicated);
    }

    public List<String> allergyRulesFor(String allergyCode) {
        return Collections.unmodifiableList(allergyRules.getOrDefault(allergyCode, Collections.emptyList()));
    }
    public List<String> comorbidityRulesFor(String icd10) {
        return Collections.unmodifiableList(comorbidityRules.getOrDefault(icd10, Collections.emptyList()));
    }
    public List<String> pregnancyContraindications() {
        return Collections.unmodifiableList(pregnancyContra);
    }

    /**
     * Evaluate a proposed treatment. Returns a non-null veto signal if any
     * hard rule fires; otherwise null. Never returns a "warning" — that is
     * by design: a clinical veto is binary.
     */
    public ClinicalVetoSignal evaluate(TreatmentProposalSignal proposal) {
        if (proposal == null) return null;
        String rx = proposal.getRxNormOrProcedureCode();
        if (rx == null) return null;
        String pid = proposal.getPatientId();

        if (patient != null) {
            for (String allergy : patient.getAllergies()) {
                List<String> banned = allergyRules.get(allergy);
                if (banned != null && containsIgnoreCase(banned, rx)) {
                    return veto(proposal, HarmVerdict.CATASTROPHIC,
                            "Patient allergy " + allergy + " contraindicates " + rx,
                            "allergy:" + allergy, pid);
                }
            }
            for (String c : patient.getComorbidities()) {
                List<String> banned = comorbidityRules.get(c);
                if (banned != null && containsIgnoreCase(banned, rx)) {
                    return veto(proposal, HarmVerdict.HARMFUL,
                            "Comorbidity " + c + " contraindicates " + rx,
                            "comorbidity:" + c, pid);
                }
            }
            if (patient.isPregnant() && containsIgnoreCase(pregnancyContra, rx)) {
                return veto(proposal, HarmVerdict.CATASTROPHIC,
                        "Pregnancy contraindicates " + rx,
                        "pregnancy", pid);
            }
        }

        if (ddi != null && ddi.isContraindicatedWithRegimen(rx)) {
            return veto(proposal, HarmVerdict.HARMFUL,
                    "Major drug interaction with current regimen for " + rx,
                    "ddi", pid);
        }

        if (guidelines != null && proposal.getRationale() != null) {
            // Attempt to parse icd10 hint from rationale; optional path.
        }
        return null;
    }

    private ClinicalVetoSignal veto(TreatmentProposalSignal proposal, HarmVerdict sev,
                                    String reason, String reasonTag, String pid) {
        List<String> alternatives = new ArrayList<>();
        String citation = defaultCitation + "[" + reasonTag + "]";
        return new ClinicalVetoSignal(
                "proposal:" + proposal.getRxNormOrProcedureCode(),
                reason, sev, citation, alternatives, pid);
    }

    private static boolean containsIgnoreCase(List<String> list, String s) {
        for (String e : list) if (e != null && e.equalsIgnoreCase(s)) return true;
        return false;
    }
}
