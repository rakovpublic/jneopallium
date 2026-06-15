/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

public final class IndustrialFmiNetworkFactory {
    private IndustrialFmiNetworkFactory() {}

    public static IndustrialFmiController controller(IndustrialFmiControllerConfig config) {
        return new IndustrialFmiController(config);
    }

    public static EquipmentHealthProcessor equipmentHealthProcessor() {
        return new EquipmentHealthProcessor();
    }
}
