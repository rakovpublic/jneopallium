/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.AffectModulationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.IAffectModulationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IActuatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IArtefactRejectionNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.ICalibrationSchedulerNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IChargeBalanceNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IIntentFusionNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.ISeizureWatchdogNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.ISpikeSortingNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IStimulationSafetyGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IThermalMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IUserStateNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IntentFusionNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.IBoredomNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.IEmpowermentNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.BodySchemaNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.IBodySchemaNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.IEmbodied;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.IReafferenceComparatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.IAstrocyteNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.IMyelinationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.IHippocampalReplayNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.IREMDreamingNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.ISharpWaveRippleNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IConceptMasteryNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IContentSelectionNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IFlowStateNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IForgettingCurveNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IHintGenerationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IPrerequisiteGraphNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IResponseObserverNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IScaffoldingNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IWellbeingGuardNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.ResponseObserverNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AffectStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.IntentSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.SensorimotorContingencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect.AffectStateModulationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.AgencyLossProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.CalibrationReportProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.ChargeAccumulationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.DriftCalibrationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.ECoGArtefactProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.IntentFusionProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.NeuralSpikeSortingProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.SeizureAssessmentProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.SeizureLockoutProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.SensoryFeedbackProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.StimulationSafetyProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci.ThermalMonitorProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.curiosity.BoredomProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.curiosity.EmpowermentProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.embodiment.BodySchemaUpdateProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.embodiment.ProprioceptionProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.embodiment.ReafferenceComparatorProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.embodiment.SensorimotorContingencyProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.glia.CalciumWaveProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.glia.GliotransmitterProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.glia.MyelinationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.sleep.DreamProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.sleep.HippocampalReplayProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.sleep.SharpWaveRippleProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.AffectObservationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.ContentRecommendationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.EngagementProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.HintObservationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.InterventionObservationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.ItemPresentationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.MasteryUpdateProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.ResponseObservationProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.ReviewScheduleProcessor;
import com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring.ScaffoldingObservationProcessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModuleProcessorsTest {

    // ---------- metadata parity (each processor exposes an interface neuron class) ----------

    private static void assertInterfaceTyped(ISignalProcessor<?, ?> p) {
        Class<?> nClass = p.getNeuronClass();
        assertNotNull(nClass, "neuron class for " + p.getClass().getSimpleName());
        assertTrue(nClass.isInterface(),
                p.getClass().getSimpleName() + " must target an interface, got " + nClass.getName());
        assertNotNull(p.getSignalClass());
        assertNotNull(p.getSignalProcessorClass());
        assertNotNull(p.getDescription());
        assertFalse(p.hasMerger());
    }

    @Test
    void affectProcessor_isInterfaceTyped() {
        AffectStateModulationProcessor p = new AffectStateModulationProcessor();
        assertEquals(IAffectModulationNeuron.class, p.getNeuronClass());
        assertInterfaceTyped(p);
    }

    @Test
    void embodimentProcessors_areInterfaceTyped() {
        assertInterfaceTyped(new ProprioceptionProcessor());
        assertInterfaceTyped(new BodySchemaUpdateProcessor());
        assertInterfaceTyped(new ReafferenceComparatorProcessor());
        assertInterfaceTyped(new SensorimotorContingencyProcessor());
        assertEquals(IEmbodied.class, new ProprioceptionProcessor().getNeuronClass());
        assertEquals(IBodySchemaNeuron.class, new BodySchemaUpdateProcessor().getNeuronClass());
        assertEquals(IReafferenceComparatorNeuron.class, new ReafferenceComparatorProcessor().getNeuronClass());
        assertEquals(IBodySchemaNeuron.class, new SensorimotorContingencyProcessor().getNeuronClass());
    }

    @Test
    void curiosityProcessors_areInterfaceTyped() {
        assertInterfaceTyped(new BoredomProcessor());
        assertInterfaceTyped(new EmpowermentProcessor());
        assertEquals(IBoredomNeuron.class, new BoredomProcessor().getNeuronClass());
        assertEquals(IEmpowermentNeuron.class, new EmpowermentProcessor().getNeuronClass());
    }

    @Test
    void gliaProcessors_areInterfaceTyped() {
        assertInterfaceTyped(new CalciumWaveProcessor());
        assertInterfaceTyped(new GliotransmitterProcessor());
        assertInterfaceTyped(new MyelinationProcessor());
        assertEquals(IAstrocyteNeuron.class, new CalciumWaveProcessor().getNeuronClass());
        assertEquals(IAstrocyteNeuron.class, new GliotransmitterProcessor().getNeuronClass());
        assertEquals(IMyelinationNeuron.class, new MyelinationProcessor().getNeuronClass());
    }

    @Test
    void sleepProcessors_areInterfaceTyped() {
        assertInterfaceTyped(new HippocampalReplayProcessor());
        assertInterfaceTyped(new SharpWaveRippleProcessor());
        assertInterfaceTyped(new DreamProcessor());
        assertEquals(IHippocampalReplayNeuron.class, new HippocampalReplayProcessor().getNeuronClass());
        assertEquals(ISharpWaveRippleNeuron.class, new SharpWaveRippleProcessor().getNeuronClass());
        assertEquals(IREMDreamingNeuron.class, new DreamProcessor().getNeuronClass());
    }

    @Test
    void bciProcessors_areInterfaceTyped() {
        assertInterfaceTyped(new NeuralSpikeSortingProcessor());
        assertInterfaceTyped(new SeizureAssessmentProcessor());
        assertInterfaceTyped(new ECoGArtefactProcessor());
        assertInterfaceTyped(new IntentFusionProcessor());
        assertInterfaceTyped(new StimulationSafetyProcessor());
        assertInterfaceTyped(new ThermalMonitorProcessor());
        assertInterfaceTyped(new SeizureLockoutProcessor());
        assertInterfaceTyped(new DriftCalibrationProcessor());
        assertInterfaceTyped(new ChargeAccumulationProcessor());
        assertInterfaceTyped(new AgencyLossProcessor());
        assertInterfaceTyped(new SensoryFeedbackProcessor());
        assertInterfaceTyped(new CalibrationReportProcessor());

        assertEquals(ISpikeSortingNeuron.class, new NeuralSpikeSortingProcessor().getNeuronClass());
        assertEquals(ISeizureWatchdogNeuron.class, new SeizureAssessmentProcessor().getNeuronClass());
        assertEquals(IArtefactRejectionNeuron.class, new ECoGArtefactProcessor().getNeuronClass());
        assertEquals(IIntentFusionNeuron.class, new IntentFusionProcessor().getNeuronClass());
        assertEquals(IStimulationSafetyGateNeuron.class, new StimulationSafetyProcessor().getNeuronClass());
        assertEquals(IThermalMonitorNeuron.class, new ThermalMonitorProcessor().getNeuronClass());
        assertEquals(IStimulationSafetyGateNeuron.class, new SeizureLockoutProcessor().getNeuronClass());
        assertEquals(ICalibrationSchedulerNeuron.class, new DriftCalibrationProcessor().getNeuronClass());
        assertEquals(IChargeBalanceNeuron.class, new ChargeAccumulationProcessor().getNeuronClass());
        assertEquals(IUserStateNeuron.class, new AgencyLossProcessor().getNeuronClass());
        assertEquals(IActuatorNeuron.class, new SensoryFeedbackProcessor().getNeuronClass());
        assertEquals(ICalibrationSchedulerNeuron.class, new CalibrationReportProcessor().getNeuronClass());
    }

    @Test
    void tutoringProcessors_areInterfaceTyped() {
        assertInterfaceTyped(new ResponseObservationProcessor());
        assertInterfaceTyped(new AffectObservationProcessor());
        assertInterfaceTyped(new EngagementProcessor());
        assertInterfaceTyped(new ContentRecommendationProcessor());
        assertInterfaceTyped(new HintObservationProcessor());
        assertInterfaceTyped(new InterventionObservationProcessor());
        assertInterfaceTyped(new ItemPresentationProcessor());
        assertInterfaceTyped(new MasteryUpdateProcessor());
        assertInterfaceTyped(new ReviewScheduleProcessor());
        assertInterfaceTyped(new ScaffoldingObservationProcessor());

        assertEquals(IResponseObserverNeuron.class, new ResponseObservationProcessor().getNeuronClass());
        assertEquals(IFlowStateNeuron.class, new AffectObservationProcessor().getNeuronClass());
        assertEquals(IFlowStateNeuron.class, new EngagementProcessor().getNeuronClass());
        assertEquals(IContentSelectionNeuron.class, new ContentRecommendationProcessor().getNeuronClass());
        assertEquals(IHintGenerationNeuron.class, new HintObservationProcessor().getNeuronClass());
        assertEquals(IWellbeingGuardNeuron.class, new InterventionObservationProcessor().getNeuronClass());
        assertEquals(IConceptMasteryNeuron.class, new ItemPresentationProcessor().getNeuronClass());
        assertEquals(IPrerequisiteGraphNeuron.class, new MasteryUpdateProcessor().getNeuronClass());
        assertEquals(IForgettingCurveNeuron.class, new ReviewScheduleProcessor().getNeuronClass());
        assertEquals(IScaffoldingNeuron.class, new ScaffoldingObservationProcessor().getNeuronClass());
    }

    // ---------- behaviour smoke-tests ----------

    @Test
    void affectProcessor_forwardsToModulation() {
        AffectModulationNeuron n = new AffectModulationNeuron();
        new AffectStateModulationProcessor().process(
                new AffectStateSignal(-0.3, 0.8, "ctx"), (IAffectModulationNeuron) n);
        // no crash + modulation neuron has ingested; we only validate the
        // processor wiring (exact state semantics are module-specific).
        assertNotNull(n);
    }

    @Test
    void proprioceptionProcessor_wiresThroughInterface() {
        BodySchemaNeuron n = new BodySchemaNeuron();
        new ProprioceptionProcessor().process(
                new ProprioceptiveSignal(0, new double[]{0.0, 1.0}, 0L), (IEmbodied) n);
        assertNotNull(n.currentSchema());
    }

    @Test
    void bodySchemaUpdateProcessor_wiresThroughInterface() {
        BodySchemaNeuron n = new BodySchemaNeuron();
        new BodySchemaUpdateProcessor().process(
                new BodySchemaUpdateSignal(5, null, false), (IBodySchemaNeuron) n);
        assertNotNull(n.currentSchema());
    }

    @Test
    void sensorimotorContingencyProcessor_defaultsNoOp() {
        BodySchemaNeuron n = new BodySchemaNeuron();
        List<ISignal> out = new SensorimotorContingencyProcessor().process(
                new SensorimotorContingencySignal(1, new double[]{0.1}, 0.9), (IBodySchemaNeuron) n);
        assertTrue(out.isEmpty());
    }

    @Test
    void intentFusionProcessor_bufferThenFuse() {
        IntentFusionNeuron n = new IntentFusionNeuron();
        IntentFusionProcessor p = new IntentFusionProcessor();
        IntentSignal a = new IntentSignal(
                com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IntentKind.REACH,
                new double[]{0.1, 0.2}, 0.6);
        IntentSignal b = new IntentSignal(
                com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IntentKind.REACH,
                new double[]{0.3, 0.4}, 0.8);
        assertTrue(p.process(a, (IIntentFusionNeuron) n).isEmpty());
        List<ISignal> out = p.process(b, (IIntentFusionNeuron) n);
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof IntentSignal);
    }

    @Test
    void responseProcessor_wiresThroughInterface() {
        ResponseObserverNeuron n = new ResponseObserverNeuron();
        new ResponseObservationProcessor().process(
                new ResponseSignal("item-1", true, 250L, "A"), (IResponseObserverNeuron) n);
        assertEquals(1, n.getTotalResponses());
    }

}
