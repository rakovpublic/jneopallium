from __future__ import annotations

from pathlib import Path
import shutil
import tempfile
from typing import Any, Dict, Iterable


class PythonModelAdapter:
    """Fallback adapter used by fast unit tests and offline metric generation."""

    def __init__(self, seed: int = 4101) -> None:
        import sys

        plant_dir = Path(__file__).resolve().parents[1] / "plant"
        if str(plant_dir) not in sys.path:
            sys.path.insert(0, str(plant_dir))
        from thermal_skid_model import ThermalSkidModel

        self.model = ThermalSkidModel(seed=seed)

    def initialize(self) -> None:
        return None

    def set(self, name: str, value: Any) -> None:
        self.model.set_input(name, value)

    def get(self, name: str) -> Any:
        return self.model.get_output(name)

    def do_step(self, current_time: float, step_size: float) -> None:
        self.model.step(step_size)

    def snapshot(self, names: Iterable[str]) -> Dict[str, Any]:
        return {name: self.get(name) for name in names}

    def close(self) -> None:
        return None


class FmpyFmuAdapter:
    """FMI 2.0 Co-Simulation adapter backed by fmpy.FMU2Slave."""

    def __init__(self, fmu_path: Path) -> None:
        try:
            from fmpy import extract, read_model_description
            from fmpy.fmi2 import FMU2Slave
        except ImportError as exc:  # pragma: no cover - depends on optional wheel
            raise RuntimeError("fmpy is required to execute the generated FMU") from exc

        self._extract_dir = Path(tempfile.mkdtemp(prefix="thermal-skid-fmu-"))
        self._read_model_description = read_model_description
        self._extract = extract
        self._fmu_cls = FMU2Slave
        self.fmu_path = Path(fmu_path)
        self.model_description = read_model_description(str(self.fmu_path))
        self.unzip_dir = extract(str(self.fmu_path), unzipdir=str(self._extract_dir))
        self.value_references = {
            variable.name: variable.valueReference
            for variable in self.model_description.modelVariables
        }
        self.boolean_names = {
            variable.name
            for variable in self.model_description.modelVariables
            if variable.type == "Boolean"
        }
        self.fmu = FMU2Slave(
            guid=self.model_description.guid,
            unzipDirectory=self.unzip_dir,
            modelIdentifier=self.model_description.coSimulation.modelIdentifier,
            instanceName="thermal-skid",
        )

    def initialize(self) -> None:
        self.fmu.instantiate()
        self.fmu.setupExperiment(startTime=0.0)
        self.fmu.enterInitializationMode()
        self.fmu.exitInitializationMode()

    def set(self, name: str, value: Any) -> None:
        vr = [self.value_references[name]]
        if name in self.boolean_names:
            self.fmu.setBoolean(vr, [bool(value)])
        else:
            self.fmu.setReal(vr, [float(value)])

    def get(self, name: str) -> Any:
        vr = [self.value_references[name]]
        if name in self.boolean_names:
            return bool(self.fmu.getBoolean(vr)[0])
        return float(self.fmu.getReal(vr)[0])

    def do_step(self, current_time: float, step_size: float) -> None:
        self.fmu.doStep(currentCommunicationPoint=current_time, communicationStepSize=step_size)

    def snapshot(self, names: Iterable[str]) -> Dict[str, Any]:
        return {name: self.get(name) for name in names}

    def close(self) -> None:
        try:
            self.fmu.terminate()
            self.fmu.freeInstance()
        finally:
            shutil.rmtree(self._extract_dir, ignore_errors=True)
