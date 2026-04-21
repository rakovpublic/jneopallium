package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CycleProcessingTest {

    private ISignalChain mockChain;

    @BeforeEach
    void setUp() {
        mockChain = mock(ISignalChain.class);
        when(mockChain.getProcessingChain()).thenReturn(List.of());
        when(mockChain.getDescription()).thenReturn("test chain");
    }

    // --- ProcessingFrequency ---

    @Test
    void processingFrequency_gettersAndSetters() {
        ProcessingFrequency pf = new ProcessingFrequency(5L, 10);
        assertEquals(5L, pf.getEpoch());
        assertEquals(10, pf.getLoop());

        pf.setEpoch(20L);
        pf.setLoop(30);
        assertEquals(20L, pf.getEpoch());
        assertEquals(30, pf.getLoop());
    }

    @Test
    void processingFrequency_defaultConstructor() {
        ProcessingFrequency pf = new ProcessingFrequency();
        assertEquals(0L, pf.epoch);
        assertEquals(0, pf.loop);
    }

    // --- ProcessingFrequencySignalItem ---

    @Test
    void processingFrequencySignalItem_gettersAndSetters() {
        ProcessingFrequency freq = new ProcessingFrequency(1L, 2);
        ProcessingFrequencySignalItem item = new ProcessingFrequencySignalItem(SumCycleSignal.class, freq);
        assertEquals(SumCycleSignal.class, item.getSignalClass());
        assertEquals(freq, item.getFrequency());

        ProcessingFrequency freq2 = new ProcessingFrequency(3L, 4);
        item.setSignalClass(MultiplyCycleSignal.class);
        item.setFrequency(freq2);
        assertEquals(MultiplyCycleSignal.class, item.getSignalClass());
        assertEquals(freq2, item.getFrequency());
    }

    @Test
    void processingFrequencySignalItem_defaultConstructor() {
        ProcessingFrequencySignalItem item = new ProcessingFrequencySignalItem();
        assertNull(item.getSignalClass());
        assertNull(item.getFrequency());
    }

    // --- CycleInputSignalUpdateItem ---

    @Test
    void cycleInputSignalUpdateItem_gettersAndSetters() {
        ProcessingFrequency freq = new ProcessingFrequency(1L, 3);
        CycleInputSignalUpdateItem item = new CycleInputSignalUpdateItem(freq, "inputA");
        assertEquals("inputA", item.getName());
        assertEquals(freq, item.getFrequency());

        item.setName("inputB");
        ProcessingFrequency freq2 = new ProcessingFrequency(2L, 6);
        item.setFrequency(freq2);
        assertEquals("inputB", item.getName());
        assertEquals(freq2, item.getFrequency());
    }

    // --- SumCycleSignal ---

    @Test
    void sumCycleSignal_copySignal_preservesValue() {
        SumCycleSignal original = new SumCycleSignal(5, 1, 10L, 3, "desc", false, "in");
        SumCycleSignal copy = original.copySignal();
        assertEquals(5, copy.getValue());
        assertEquals(1, copy.getSourceLayerId());
        assertEquals(10L, copy.getSourceNeuronId());
        assertEquals("desc", copy.getDescription());
        assertEquals("in", copy.getInputName());
    }

    @Test
    void sumCycleSignal_getCurrentSignalClass() {
        SumCycleSignal signal = new SumCycleSignal(1, 0, 0L, 1, "", false, "");
        assertEquals(SumCycleSignal.class, signal.getCurrentSignalClass());
        assertEquals(Integer.class, signal.getParamClass());
    }

    // --- MultiplyCycleSignal ---

    @Test
    void multiplyCycleSignal_copySignal_preservesValue() {
        MultiplyCycleSignal original = new MultiplyCycleSignal(2.5f, 0, 1L, 5, "desc", true, "inp");
        MultiplyCycleSignal copy = original.copySignal();
        assertEquals(2.5f, copy.getValue());
        assertEquals(0, copy.getSourceLayerId());
        assertEquals(1L, copy.getSourceNeuronId());
    }

    @Test
    void multiplyCycleSignal_getCurrentSignalClass() {
        MultiplyCycleSignal signal = new MultiplyCycleSignal(1.0f, 0, 0L, 1, "", false, "");
        assertEquals(MultiplyCycleSignal.class, signal.getCurrentSignalClass());
        assertEquals(Float.class, signal.getParamClass());
    }

    // --- ProcessingFrequencySignal ---

    @Test
    void processingFrequencySignal_copySignal_usesThisName() {
        ProcessingFrequencySignalItem item = new ProcessingFrequencySignalItem(SumCycleSignal.class, new ProcessingFrequency(1L, 2));
        ProcessingFrequencySignal signal = new ProcessingFrequencySignal(item, 0, 1L, 3, "desc", false, "inp", false, true, "mySignalName");
        ProcessingFrequencySignal copy = signal.copySignal();
        // After fix: copy should preserve the original name, not ProcessingFrequency.class.getName()
        assertEquals("mySignalName", copy.getName());
        assertEquals(item, copy.getValue());
    }

    @Test
    void processingFrequencySignal_copySignal_notUsingProcessingFrequencyClassName() {
        ProcessingFrequencySignalItem item = new ProcessingFrequencySignalItem();
        ProcessingFrequencySignal signal = new ProcessingFrequencySignal(item, 0, 0L, 1, "", false, "", false, false, "correctName");
        ProcessingFrequencySignal copy = signal.copySignal();
        assertNotEquals(ProcessingFrequency.class.getName(), copy.getName());
        assertEquals("correctName", copy.getName());
    }

    // --- CycleInputUpdateSignal ---

    @Test
    void cycleInputUpdateSignal_copySignal_usesThisName() {
        CycleInputSignalUpdateItem item = new CycleInputSignalUpdateItem(new ProcessingFrequency(1L, 2), "input1");
        CycleInputUpdateSignal signal = new CycleInputUpdateSignal(item, 0, 1L, 3, "desc", false, "inp", false, true, "myName");
        CycleInputUpdateSignal copy = signal.copySignal();
        // After fix: must be "myName", not ProcessingFrequency.class.getName()
        assertEquals("myName", copy.getName());
    }

    @Test
    void cycleInputUpdateSignal_copySignal_notUsingProcessingFrequencyClassName() {
        CycleInputSignalUpdateItem item = new CycleInputSignalUpdateItem(new ProcessingFrequency(0L, 0), "x");
        CycleInputUpdateSignal signal = new CycleInputUpdateSignal(item, 0, 0L, 1, "", false, "", false, false, "rightName");
        CycleInputUpdateSignal copy = signal.copySignal();
        assertNotEquals(ProcessingFrequency.class.getName(), copy.getName());
        assertEquals("rightName", copy.getName());
    }

    // --- SumCycleSignalProcessor ---

    @Test
    void sumCycleSignalProcessor_addsValueToLoopCount() {
        CycleNeuron neuron = new CycleNeuron(10, mockChain, 1L, 1L);
        SumCycleSignal signal = new SumCycleSignal(5, 0, 0L, 1, "", false, "");
        SumCycleSignalProcessor processor = new SumCycleSignalProcessor();
        List<?> result = processor.process(signal, neuron);
        assertEquals(15, neuron.getLoopCount());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void sumCycleSignalProcessor_metadata() {
        SumCycleSignalProcessor processor = new SumCycleSignalProcessor();
        assertFalse(processor.hasMerger());
        assertEquals(SumCycleSignalProcessor.class, processor.getSignalProcessorClass());
        assertEquals(CycleNeuron.class, processor.getNeuronClass());
        assertEquals(SumCycleSignal.class, processor.getSignalClass());
    }

    @Test
    void sumCycleSignalProcessor_negativeValue_decreasesLoopCount() {
        CycleNeuron neuron = new CycleNeuron(10, mockChain, 1L, 1L);
        SumCycleSignal signal = new SumCycleSignal(-3, 0, 0L, 1, "", false, "");
        new SumCycleSignalProcessor().process(signal, neuron);
        assertEquals(7, neuron.getLoopCount());
    }

    // --- MultiplyCycleSignalProcessor ---

    @Test
    void multiplyCycleSignalProcessor_multipliesLoopCount() {
        CycleNeuron neuron = new CycleNeuron(10, mockChain, 1L, 1L);
        MultiplyCycleSignal signal = new MultiplyCycleSignal(2.0f, 0, 0L, 1, "", false, "");
        MultiplyCycleSignalProcessor processor = new MultiplyCycleSignalProcessor();
        List<?> result = processor.process(signal, neuron);
        assertEquals(20, neuron.getLoopCount());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void multiplyCycleSignalProcessor_metadata() {
        MultiplyCycleSignalProcessor processor = new MultiplyCycleSignalProcessor();
        assertFalse(processor.hasMerger());
        assertEquals(MultiplyCycleSignalProcessor.class, processor.getSignalProcessorClass());
        assertEquals(CycleNeuron.class, processor.getNeuronClass());
        assertEquals(MultiplyCycleSignal.class, processor.getSignalClass());
    }

    @Test
    void multiplyCycleSignalProcessor_zeroValue_setsLoopCountToZero() {
        CycleNeuron neuron = new CycleNeuron(10, mockChain, 1L, 1L);
        MultiplyCycleSignal signal = new MultiplyCycleSignal(0.0f, 0, 0L, 1, "", false, "");
        new MultiplyCycleSignalProcessor().process(signal, neuron);
        assertEquals(0, neuron.getLoopCount());
    }

    // --- ProcessingFrequencySignalProcessor ---

    @Test
    void processingFrequencySignalProcessor_updatesFrequencyMap() {
        CycleNeuron neuron = new CycleNeuron(5, mockChain, 1L, 1L);
        ProcessingFrequency freq = new ProcessingFrequency(2L, 3);
        ProcessingFrequencySignalItem item = new ProcessingFrequencySignalItem(SumCycleSignal.class, freq);
        ProcessingFrequencySignal signal = new ProcessingFrequencySignal(item, 0, 0L, 1, "", false, "", false, false, "name");
        ProcessingFrequencySignalProcessor processor = new ProcessingFrequencySignalProcessor();
        List<?> result = processor.process(signal, neuron);
        assertTrue(result.isEmpty());
        ProcessingFrequency stored = neuron.getSignalProcessingFrequencyMap().get(SumCycleSignal.class);
        assertNotNull(stored);
        assertEquals(2L, stored.getEpoch());
        assertEquals(3, stored.getLoop());
    }

    @Test
    void processingFrequencySignalProcessor_metadata() {
        ProcessingFrequencySignalProcessor processor = new ProcessingFrequencySignalProcessor();
        assertFalse(processor.hasMerger());
        assertEquals(ProcessingFrequencySignalProcessor.class, processor.getSignalProcessorClass());
        assertEquals(CycleNeuron.class, processor.getNeuronClass());
        assertEquals(ProcessingFrequencySignal.class, processor.getSignalClass());
        assertNotNull(processor.getDescription());
    }

    // --- CycleInputUpdateProcessor ---

    @Test
    void cycleInputUpdateProcessor_updatesMatchingInput() {
        IInitInput mockInput = mock(IInitInput.class);
        when(mockInput.getName()).thenReturn("inputA");

        HashMap<IInitInput, ProcessingFrequency> map = new HashMap<>();
        ProcessingFrequency oldFreq = new ProcessingFrequency(0L, 1);
        map.put(mockInput, oldFreq);

        CycleNeuron neuron = new CycleNeuron(5, mockChain, 1L, 1L, new HashMap<>(), map);

        ProcessingFrequency newFreq = new ProcessingFrequency(10L, 20);
        CycleInputSignalUpdateItem item = new CycleInputSignalUpdateItem(newFreq, "inputA");
        CycleInputUpdateSignal signal = new CycleInputUpdateSignal(item, 0, 0L, 1, "", false, "", false, false, "n");

        CycleInputUpdateProcessor processor = new CycleInputUpdateProcessor();
        List<?> result = processor.process(signal, neuron);

        assertTrue(result.isEmpty());
        ProcessingFrequency updated = neuron.getInputProcessingFrequencyHashMap().get(mockInput);
        assertEquals(10L, updated.getEpoch());
        assertEquals(20, updated.getLoop());
    }

    @Test
    void cycleInputUpdateProcessor_noMatchingInput_leavesMapUnchanged() {
        IInitInput mockInput = mock(IInitInput.class);
        when(mockInput.getName()).thenReturn("other");

        ProcessingFrequency freq = new ProcessingFrequency(1L, 5);
        HashMap<IInitInput, ProcessingFrequency> map = new HashMap<>();
        map.put(mockInput, freq);

        CycleNeuron neuron = new CycleNeuron(5, mockChain, 1L, 1L, new HashMap<>(), map);

        CycleInputSignalUpdateItem item = new CycleInputSignalUpdateItem(new ProcessingFrequency(99L, 99), "nonExistent");
        CycleInputUpdateSignal signal = new CycleInputUpdateSignal(item, 0, 0L, 1, "", false, "", false, false, "n");

        new CycleInputUpdateProcessor().process(signal, neuron);

        assertEquals(1L, neuron.getInputProcessingFrequencyHashMap().get(mockInput).getEpoch());
        assertEquals(5, neuron.getInputProcessingFrequencyHashMap().get(mockInput).getLoop());
    }

    @Test
    void cycleInputUpdateProcessor_metadata() {
        CycleInputUpdateProcessor processor = new CycleInputUpdateProcessor();
        assertFalse(processor.hasMerger());
        assertEquals(CycleInputUpdateProcessor.class, processor.getSignalProcessorClass());
        assertEquals(CycleNeuron.class, processor.getNeuronClass());
        assertEquals(CycleInputUpdateSignal.class, processor.getSignalClass());
        assertNotNull(processor.getDescription());
    }

    // --- CycleNeuron ---

    @Test
    void cycleNeuron_initialLoopCount() {
        CycleNeuron neuron = new CycleNeuron(42, mockChain, 1L, 1L);
        assertEquals(42, neuron.getLoopCount());
    }

    @Test
    void cycleNeuron_setLoopCount() {
        CycleNeuron neuron = new CycleNeuron(0, mockChain, 1L, 1L);
        neuron.setLoopCount(99);
        assertEquals(99, neuron.getLoopCount());
    }

    @Test
    void cycleNeuron_signalProcessingFrequencyMapInitialized() {
        CycleNeuron neuron = new CycleNeuron(0, mockChain, 1L, 1L);
        assertNotNull(neuron.getSignalProcessingFrequencyMap());
        assertTrue(neuron.getSignalProcessingFrequencyMap().isEmpty());
    }
}
