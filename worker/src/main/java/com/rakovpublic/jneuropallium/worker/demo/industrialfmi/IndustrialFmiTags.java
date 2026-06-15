/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

/** Stable signal tags used by the industrial FMI skid demo. */
public final class IndustrialFmiTags {
    private IndustrialFmiTags() {}

    public static final String TEMP_PV = "SKID.TIC101.PV";
    public static final String TEMP_SP = "SKID.TIC101.SP";
    public static final String FLOW_PV = "SKID.FIC101.PV";
    public static final String PUMP_SPEED_PV = "SKID.P101.SPEED.PV";
    public static final String VALVE_POSITION_PV = "SKID.CV101.POSITION.PV";
    public static final String HEATER_POWER_PV = "SKID.HTR101.POWER.PV";
    public static final String SUCTION_PRESSURE = "SKID.SUCTION.PRESSURE";
    public static final String HIGH_TEMP_INTERLOCK = "SKID.INTERLOCK.HIGH_TEMP";
    public static final String LOW_FLOW_INTERLOCK = "SKID.INTERLOCK.LOW_FLOW";
    public static final String LOW_SUCTION_INTERLOCK = "SKID.INTERLOCK.LOW_SUCTION";
    public static final String OPERATOR_MANUAL_MODE = "SKID.OPERATOR.MANUAL_MODE";
    public static final String OPERATOR_MANUAL_VALVE = "SKID.OPERATOR.MANUAL_VALVE";
    public static final String OPERATOR_MANUAL_PUMP = "SKID.OPERATOR.MANUAL_PUMP";

    public static final String VALVE_CMD = "SKID.CV101.CMD";
    public static final String PUMP_SPEED_SP = "SKID.P101.SPEED.SP";
    public static final String HEATER_POWER_CMD = "SKID.HTR101.POWER.CMD";

    public static final String VIBRATION = "SKID.P101.VIBRATION";
    public static final String BEARING_TEMP = "SKID.P101.BEARING_TEMP";
    public static final String PUMP_POWER_KW = "SKID.P101.POWER_KW";

    public static final String ADVISORY_PUMP_SPEED = "SKID.ADVISORY.PUMP_SPEED";
    public static final String ADVISORY_MAINTENANCE_PRIORITY = "SKID.ADVISORY.MAINTENANCE_PRIORITY";
    public static final String ADVISORY_BEARING_RISK = "SKID.ADVISORY.BEARING_RISK";
    public static final String ADVISORY_ENERGY_MODE = "SKID.ADVISORY.ENERGY_MODE";

    public static final String LOOP_COOLING = "TIC-101-COOLING";
    public static final String LOOP_PUMP = "FIC-101-PUMP";
    public static final String LOOP_HEATER = "TIC-101-HEATER";
}
