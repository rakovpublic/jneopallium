package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;

import java.util.List;

public interface IContraindicationNeuron extends IModulatableNeuron {
    void setContext(IPatientContextNeuron p);
    void setDrugInteractions(IDrugInteractionMemoryNeuron d);
    void setGuidelines(IGuidelineMemoryNeuron g);
    void setDefaultCitation(String c);
    void addAllergyRule(String allergyCode, String rxContraindicated);
    void addComorbidityRule(String icd10, String rxContraindicated);
    void addPregnancyContraindication(String rxContraindicated);
    List<String> allergyRulesFor(String allergyCode);
    List<String> comorbidityRulesFor(String icd10);
    List<String> pregnancyContraindications();
    ClinicalVetoSignal evaluate(TreatmentProposalSignal proposal);
}
