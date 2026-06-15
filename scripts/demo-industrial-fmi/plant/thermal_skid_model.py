from __future__ import annotations

from dataclasses import asdict, dataclass
import math
import random
from typing import Any, Dict


def clamp(value: float, low: float, high: float) -> float:
    return max(low, min(high, value))


@dataclass
class ThermalSkidInputs:
    coolingValveCmd: float = 35.0
    pumpSpeedCmd: float = 45.0
    heaterPowerCmd: float = 35.0
    processLoadFlow: float = 0.45
    ambientTemperature: float = 23.0
    faultValveStuck: bool = False
    faultPumpWear: float = 0.0
    faultTempSensorDrift: float = 0.0
    faultThermalRunawayKw: float = 0.0


@dataclass
class ThermalSkidState:
    processTemperature: float = 45.0
    coolingValvePosition: float = 35.0
    pumpSpeedActual: float = 45.0
    bearingTemperature: float = 36.0


@dataclass
class ThermalSkidOutputs:
    processTemperature: float
    measuredTemperature: float
    circulationFlow: float
    suctionPressure: float
    coolingValvePosition: float
    pumpSpeedActual: float
    pumpPowerKw: float
    vibrationRms: float
    bearingTemperature: float
    highTemperatureInterlock: bool
    lowFlowInterlock: bool
    lowSuctionInterlock: bool

    def as_dict(self) -> Dict[str, Any]:
        return asdict(self)


class ThermalSkidModel:
    """Deterministic heated circulation and heat-exchanger skid model.

    The state evolves continuously through every ``step`` call. Noise is
    deterministic for a fixed seed and call sequence, which makes scenario
    traces reproducible while still exercising validation logic.
    """

    HIGH_TEMP_TRIP_C = 92.0
    LOW_FLOW_TRIP = 0.23
    LOW_SUCTION_TRIP_BAR = 0.42

    def __init__(self, seed: int = 4101) -> None:
        self.inputs = ThermalSkidInputs()
        self.state = ThermalSkidState()
        self.time = 0.0
        self._rng = random.Random(seed)
        self._last_outputs = self._compute_outputs(sensor_noise=0.0)

    def set_input(self, name: str, value: Any) -> None:
        if not hasattr(self.inputs, name):
            raise KeyError(f"unknown ThermalSkid input: {name}")
        if name == "faultValveStuck":
            setattr(self.inputs, name, bool(value))
        else:
            setattr(self.inputs, name, float(value))

    def get_output(self, name: str) -> Any:
        if not hasattr(self._last_outputs, name):
            raise KeyError(f"unknown ThermalSkid output: {name}")
        return getattr(self._last_outputs, name)

    def snapshot(self) -> Dict[str, Any]:
        data: Dict[str, Any] = {}
        data.update(asdict(self.inputs))
        data.update(asdict(self.state))
        data.update(self._last_outputs.as_dict())
        data["simulationTime"] = self.time
        return data

    def step(self, dt: float) -> ThermalSkidOutputs:
        if dt <= 0:
            raise ValueError("dt must be positive")

        inp = self.inputs
        st = self.state
        wear = clamp(inp.faultPumpWear, 0.0, 1.0)
        load = clamp(inp.processLoadFlow, 0.0, 1.0)
        pump_cmd = clamp(inp.pumpSpeedCmd, 0.0, 100.0)
        valve_cmd = clamp(inp.coolingValveCmd, 0.0, 100.0)
        heater_cmd = clamp(inp.heaterPowerCmd, 0.0, 100.0)

        pump_tau = 1.10 + 0.60 * wear
        st.pumpSpeedActual += (pump_cmd - st.pumpSpeedActual) * clamp(dt / pump_tau, 0.0, 1.0)

        if not inp.faultValveStuck:
            valve_tau = 0.85
            st.coolingValvePosition += (valve_cmd - st.coolingValvePosition) * clamp(dt / valve_tau, 0.0, 1.0)

        flow = self._flow(wear)
        suction = self._suction_pressure(wear)

        heater_kw = 16.0 * heater_cmd / 100.0 + max(0.0, inp.faultThermalRunawayKw)
        process_heat_kw = 2.0 + 6.0 * load
        cooling_kw = 0.65 * (st.coolingValvePosition / 100.0) * flow * max(0.0, st.processTemperature - inp.ambientTemperature)
        ambient_loss_kw = 0.045 * (st.processTemperature - inp.ambientTemperature)

        # Effective thermal capacity in kJ/C; one kW is one kJ/s.
        thermal_capacity = 90.0
        dtemp = (heater_kw + process_heat_kw - cooling_kw - ambient_loss_kw) * dt / thermal_capacity
        st.processTemperature += dtemp

        power_kw = self._pump_power(wear)
        bearing_target = inp.ambientTemperature + 9.0 + 2.8 * power_kw + 42.0 * wear
        st.bearingTemperature += (bearing_target - st.bearingTemperature) * clamp(dt / 18.0, 0.0, 1.0)

        self.time += dt
        sensor_noise = self._rng.uniform(-0.035, 0.035)
        self._last_outputs = self._compute_outputs(sensor_noise=sensor_noise)
        return self._last_outputs

    def _flow(self, wear: float) -> float:
        speed = clamp(self.state.pumpSpeedActual, 0.0, 100.0) / 100.0
        wear_factor = 1.0 - 0.42 * wear
        suction_factor = clamp(self._suction_pressure(wear) / 0.95, 0.35, 1.15)
        return max(0.0, 1.18 * math.pow(speed, 1.15) * wear_factor * suction_factor)

    def _suction_pressure(self, wear: float) -> float:
        speed = clamp(self.state.pumpSpeedActual, 0.0, 100.0)
        load = clamp(self.inputs.processLoadFlow, 0.0, 1.0)
        return 1.05 - 0.0055 * speed - 0.28 * wear + 0.08 * load

    def _pump_power(self, wear: float) -> float:
        speed = clamp(self.state.pumpSpeedActual, 0.0, 100.0) / 100.0
        return 0.18 + 8.2 * math.pow(speed, 3.0) * (1.0 + 0.55 * wear)

    def _vibration(self, wear: float) -> float:
        speed = clamp(self.state.pumpSpeedActual, 0.0, 100.0) / 100.0
        noise = self._rng.uniform(-0.015, 0.015)
        return max(0.0, 0.55 + 1.45 * speed + 7.4 * wear * math.pow(speed, 1.4) + noise)

    def _compute_outputs(self, sensor_noise: float) -> ThermalSkidOutputs:
        wear = clamp(self.inputs.faultPumpWear, 0.0, 1.0)
        flow = self._flow(wear)
        suction = self._suction_pressure(wear)
        pump_power = self._pump_power(wear)
        measured = self.state.processTemperature + self.inputs.faultTempSensorDrift + sensor_noise
        bearing = self.state.bearingTemperature + self._rng.uniform(-0.025, 0.025)
        return ThermalSkidOutputs(
            processTemperature=self.state.processTemperature,
            measuredTemperature=measured,
            circulationFlow=flow,
            suctionPressure=suction,
            coolingValvePosition=self.state.coolingValvePosition,
            pumpSpeedActual=self.state.pumpSpeedActual,
            pumpPowerKw=pump_power,
            vibrationRms=self._vibration(wear),
            bearingTemperature=bearing,
            highTemperatureInterlock=self.state.processTemperature >= self.HIGH_TEMP_TRIP_C,
            lowFlowInterlock=flow < self.LOW_FLOW_TRIP,
            lowSuctionInterlock=suction < self.LOW_SUCTION_TRIP_BAR,
        )
