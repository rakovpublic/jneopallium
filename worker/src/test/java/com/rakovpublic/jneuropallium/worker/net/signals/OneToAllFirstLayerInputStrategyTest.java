package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OneToAllFirstLayerInputStrategyTest {

    private OneToAllFirstLayerInputStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new OneToAllFirstLayerInputStrategy();
    }

    @Test
    void getClazz_returnsFullyQualifiedName() {
        assertEquals("com.rakovpublic.jneuropallium.worker.net.signals.OneToAllFirstLayerInputStrategy",
                strategy.getClazz());
    }

    @Test
    void setClazz_updatesField() {
        strategy.setClazz("custom.Class");
        assertEquals("custom.Class", strategy.getClazz());
    }

    @Test
    void getInputs_multipleNeuronsInLayer0_usesLayer0() {
        ILayersMeta layersMeta = mock(ILayersMeta.class);
        ILayerMeta layer0 = mock(ILayerMeta.class);
        INeuron neuron1 = mock(INeuron.class);
        INeuron neuron2 = mock(INeuron.class);

        when(layersMeta.getLayerByPosition(0)).thenReturn(layer0);
        when(layer0.getNeurons()).thenReturn(List.of(neuron1, neuron2));
        when(layer0.getID()).thenReturn(0);
        when(neuron1.getId()).thenReturn(1L);
        when(neuron2.getId()).thenReturn(2L);
        when(neuron1.canProcess(any(ISignal.class))).thenReturn(true);
        when(neuron2.canProcess(any(ISignal.class))).thenReturn(false);
        when(layersMeta.getLayers()).thenReturn(List.of(layer0));

        ISignal sig = mock(ISignal.class);
        CopyOnWriteArrayList<ISignal> signals = new CopyOnWriteArrayList<>(List.of(sig));

        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result =
                strategy.getInputs(layersMeta, signals);

        // Layer 0 has 2 neurons, so it stays on layer0
        assertTrue(result.containsKey(0));
        assertEquals(2, result.get(0).size());
        // neuron1 can process the signal, neuron2 cannot
        assertEquals(1, result.get(0).get(1L).size());
        assertEquals(0, result.get(0).get(2L).size());
    }

    @Test
    void getInputs_singleNeuronInLayer0_withLayer1Available_usesLayer1() {
        ILayersMeta layersMeta = mock(ILayersMeta.class);
        ILayerMeta layer0 = mock(ILayerMeta.class);
        ILayerMeta layer1 = mock(ILayerMeta.class);
        INeuron neuron = mock(INeuron.class);

        when(layersMeta.getLayerByPosition(0)).thenReturn(layer0);
        when(layersMeta.getLayerByPosition(1)).thenReturn(layer1);
        // Single neuron in layer0 triggers fallback to layer1
        when(layer0.getNeurons()).thenReturn(List.of(mock(INeuron.class)));
        when(layer1.getNeurons()).thenReturn(List.of(neuron));
        when(layer1.getID()).thenReturn(1);
        when(neuron.getId()).thenReturn(10L);
        when(neuron.canProcess(any(ISignal.class))).thenReturn(true);
        // 2 layers present
        when(layersMeta.getLayers()).thenReturn(List.of(layer0, layer1));

        ISignal sig = mock(ISignal.class);
        CopyOnWriteArrayList<ISignal> signals = new CopyOnWriteArrayList<>(List.of(sig));

        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result =
                strategy.getInputs(layersMeta, signals);

        // After fix: key should be layerMeta.getID() = 1, not hardcoded 0
        assertTrue(result.containsKey(1));
        assertFalse(result.containsKey(0));
        assertEquals(1, result.get(1).get(10L).size());
    }

    @Test
    void getInputs_singleNeuronInLayer0_noLayer1_doesNotThrow() {
        ILayersMeta layersMeta = mock(ILayersMeta.class);
        ILayerMeta layer0 = mock(ILayerMeta.class);
        INeuron neuron = mock(INeuron.class);

        when(layersMeta.getLayerByPosition(0)).thenReturn(layer0);
        // Single neuron but only 1 layer total — bounds check prevents getLayerByPosition(1)
        when(layer0.getNeurons()).thenReturn(List.of(neuron));
        when(layer0.getID()).thenReturn(0);
        when(neuron.getId()).thenReturn(5L);
        when(neuron.canProcess(any(ISignal.class))).thenReturn(true);
        when(layersMeta.getLayers()).thenReturn(List.of(layer0));

        ISignal sig = mock(ISignal.class);
        CopyOnWriteArrayList<ISignal> signals = new CopyOnWriteArrayList<>(List.of(sig));

        // After fix: must not throw IndexOutOfBoundsException
        assertDoesNotThrow(() -> strategy.getInputs(layersMeta, signals));

        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result =
                strategy.getInputs(layersMeta, signals);
        assertTrue(result.containsKey(0));
    }

    @Test
    void getInputs_emptySignals_allNeuronsHaveEmptyList() {
        ILayersMeta layersMeta = mock(ILayersMeta.class);
        ILayerMeta layer0 = mock(ILayerMeta.class);
        INeuron neuron = mock(INeuron.class);

        when(layersMeta.getLayerByPosition(0)).thenReturn(layer0);
        when(layer0.getNeurons()).thenReturn(List.of(neuron, mock(INeuron.class)));
        when(layer0.getID()).thenReturn(0);
        when(neuron.getId()).thenReturn(1L);
        when(layersMeta.getLayers()).thenReturn(List.of(layer0));

        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result =
                strategy.getInputs(layersMeta, new CopyOnWriteArrayList<>());

        assertTrue(result.containsKey(0));
        assertEquals(0, result.get(0).get(1L).size());
    }
}
