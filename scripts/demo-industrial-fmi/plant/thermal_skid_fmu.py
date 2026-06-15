from __future__ import annotations

try:
    from pythonfmu import Boolean, Fmi2Causality, Fmi2Slave, Real
except ImportError as exc:  # pragma: no cover - exercised by build tooling
    raise SystemExit(
        "pythonfmu is required to build ThermalSkid.fmu. "
        "Install scripts/demo-industrial-fmi/plant/requirements.txt"
    ) from exc

from thermal_skid_model import ThermalSkidModel


class ThermalSkid(Fmi2Slave):
    author = "Jneopallium demo"
    description = "Heated circulation and heat-exchanger skid"

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.model = ThermalSkidModel(seed=4101)

        self.coolingValveCmd = self.model.inputs.coolingValveCmd
        self.pumpSpeedCmd = self.model.inputs.pumpSpeedCmd
        self.heaterPowerCmd = self.model.inputs.heaterPowerCmd
        self.processLoadFlow = self.model.inputs.processLoadFlow
        self.ambientTemperature = self.model.inputs.ambientTemperature
        self.faultValveStuck = self.model.inputs.faultValveStuck
        self.faultPumpWear = self.model.inputs.faultPumpWear
        self.faultTempSensorDrift = self.model.inputs.faultTempSensorDrift
        self.faultThermalRunawayKw = self.model.inputs.faultThermalRunawayKw

        out = self.model.snapshot()
        self.processTemperature = out["processTemperature"]
        self.measuredTemperature = out["measuredTemperature"]
        self.circulationFlow = out["circulationFlow"]
        self.suctionPressure = out["suctionPressure"]
        self.coolingValvePosition = out["coolingValvePosition"]
        self.pumpSpeedActual = out["pumpSpeedActual"]
        self.pumpPowerKw = out["pumpPowerKw"]
        self.vibrationRms = out["vibrationRms"]
        self.bearingTemperature = out["bearingTemperature"]
        self.highTemperatureInterlock = out["highTemperatureInterlock"]
        self.lowFlowInterlock = out["lowFlowInterlock"]
        self.lowSuctionInterlock = out["lowSuctionInterlock"]

        for name in (
            "coolingValveCmd",
            "pumpSpeedCmd",
            "heaterPowerCmd",
            "processLoadFlow",
            "ambientTemperature",
            "faultPumpWear",
            "faultTempSensorDrift",
            "faultThermalRunawayKw",
        ):
            self.register_variable(Real(name, causality=Fmi2Causality.input))
        self.register_variable(Boolean("faultValveStuck", causality=Fmi2Causality.input))

        for name in (
            "processTemperature",
            "measuredTemperature",
            "circulationFlow",
            "suctionPressure",
            "coolingValvePosition",
            "pumpSpeedActual",
            "pumpPowerKw",
            "vibrationRms",
            "bearingTemperature",
        ):
            self.register_variable(Real(name, causality=Fmi2Causality.output))
        for name in (
            "highTemperatureInterlock",
            "lowFlowInterlock",
            "lowSuctionInterlock",
        ):
            self.register_variable(Boolean(name, causality=Fmi2Causality.output))

    def do_step(self, current_time, step_size):
        for name in (
            "coolingValveCmd",
            "pumpSpeedCmd",
            "heaterPowerCmd",
            "processLoadFlow",
            "ambientTemperature",
            "faultValveStuck",
            "faultPumpWear",
            "faultTempSensorDrift",
            "faultThermalRunawayKw",
        ):
            self.model.set_input(name, getattr(self, name))
        outputs = self.model.step(step_size)
        for name, value in outputs.as_dict().items():
            setattr(self, name, value)
        return True
