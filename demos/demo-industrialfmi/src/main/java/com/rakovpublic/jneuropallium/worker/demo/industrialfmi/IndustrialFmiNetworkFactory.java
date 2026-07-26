/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

public final class IndustrialFmiNetworkFactory {
    public static final String LOOP_GUARDIAN_MODEL_ID = "industrial-loop-guardian";
    public static final String LOOP_GUARDIAN_MODEL_RESOURCE_ROOT = "model/industrial-loop-guardian/";

    private IndustrialFmiNetworkFactory() {}

    public static IndustrialFmiController controller(IndustrialFmiControllerConfig config) {
        return new IndustrialFmiController(config);
    }

    public static EquipmentHealthProcessor equipmentHealthProcessor() {
        return new EquipmentHealthProcessor();
    }

    public static String loopGuardianModelDescriptorResource() {
        return LOOP_GUARDIAN_MODEL_RESOURCE_ROOT + "model-descriptor.json";
    }
}
