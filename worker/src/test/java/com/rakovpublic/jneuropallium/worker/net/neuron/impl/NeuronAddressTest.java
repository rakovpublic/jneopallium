package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NeuronAddressTest {

    @Test
    void testEquals_sameValues_returnsTrue() {
        NeuronAddress a = new NeuronAddress(1, 10L);
        NeuronAddress b = new NeuronAddress(1, 10L);
        assertEquals(a, b);
    }

    @Test
    void testEquals_differentLayer_returnsFalse() {
        NeuronAddress a = new NeuronAddress(1, 10L);
        NeuronAddress b = new NeuronAddress(2, 10L);
        assertNotEquals(a, b);
    }

    @Test
    void testEquals_differentNeuron_returnsFalse() {
        NeuronAddress a = new NeuronAddress(1, 10L);
        NeuronAddress b = new NeuronAddress(1, 20L);
        assertNotEquals(a, b);
    }

    @Test
    void testHashCode_equalObjects_sameHashCode() {
        NeuronAddress a = new NeuronAddress(1, 10L);
        NeuronAddress b = new NeuronAddress(1, 10L);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testEquals_self_returnsTrue() {
        NeuronAddress a = new NeuronAddress(1, 10L);
        assertEquals(a, a);
    }

    @Test
    void testEquals_null_returnsFalse() {
        NeuronAddress a = new NeuronAddress(1, 10L);
        assertNotEquals(null, a);
    }

    @Test
    void testGettersSetters() {
        NeuronAddress a = new NeuronAddress();
        a.setLayerId(5);
        a.setNeuronId(99L);

        assertEquals(5, a.getLayerId());
        assertEquals(99L, a.getNeuronId());
    }
}
