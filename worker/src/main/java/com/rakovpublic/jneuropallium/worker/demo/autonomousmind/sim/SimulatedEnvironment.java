package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindScenario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class SimulatedEnvironment {
    public final EnvironmentMap map;
    public final AgentOperationalState agent;
    public final EnergyState energy;
    public final ChargingStation chargingStation;
    public final BystanderState bystander;
    public final SensorRig sensorRig = new SensorRig();
    public final HumanOwnerCommand ownerCommand;
    public final Random random;

    public SimulatedEnvironment(AutonomousMindScenario scenario) {
        this.map = EnvironmentMap.fromRows(scenario.map);
        this.agent = new AgentOperationalState(map.agentX, map.agentY);
        this.energy = new EnergyState(scenario.initialEnergy, scenario.initiallyCharging);
        this.chargingStation = map.chargingStation;
        this.bystander = map.bystander;
        this.ownerCommand = new HumanOwnerCommand(scenario.ownerTask);
        this.random = new Random(scenario.seed);
        if (scenario.ownerTask != null) {
            agent.activeTaskId = scenario.ownerTask.taskId;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("agent", agent.toMap());
        row.put("energy", energy.toMap());
        row.put("charger", chargingStation == null ? null : chargingStation.toMap());
        row.put("bystander", bystander == null ? null : bystander.toMap());
        row.put("ownerCommand", ownerCommand.toMap());
        row.put("map", map.summary());
        return row;
    }
}
