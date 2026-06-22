from __future__ import annotations

import os
import platform
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass, asdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]


@dataclass
class DependencyResult:
    name: str
    required: bool
    present: bool
    version: str | None
    detail: str
    guidance: str


class DependencyChecker:
    """Startup dependency checker for the live backend."""

    def check(self, preview_enabled: bool = False) -> dict:
        results = [
            self._java(),
            self._command("mvn", ["mvn", "-version"], "Install Apache Maven and ensure mvn is on PATH."),
            self._python(),
            self._ardupilot_source(),
            self._command(
                "sim_vehicle.py",
                ["sim_vehicle.py", "--help"],
                "Install ArduPilot tools and ensure sim_vehicle.py is on PATH.",
                expect_zero=False,
            ),
            self._command("Gazebo Harmonic gz", ["gz", "--version"], "Install Gazebo Harmonic and ensure gz is on PATH."),
            self._ardupilot_gazebo_plugin(),
            self._command("ROS 2", ["ros2", "--help"], "Install ROS 2 and source its setup script before running."),
            self._ros2_package("ros_gz_bridge"),
            self._ros2_package("rosbridge_server"),
        ]
        if preview_enabled:
            results.append(
                self._command(
                    "GStreamer",
                    ["gst-launch-1.0", "--version"],
                    "Install GStreamer or disable video preview.",
                )
            )
        missing = [asdict(result) for result in results if result.required and not result.present]
        return {
            "platform": platform.platform(),
            "supportedLivePlatform": "Linux",
            "livePlatformSupported": platform.system() == "Linux",
            "results": [asdict(result) for result in results],
            "missing": missing,
            "ok": platform.system() == "Linux" and not missing,
        }

    def check_carla_air(self, preview_enabled: bool = False) -> dict:
        results = [
            self._python(),
            self._carla_air_python_api(),
            self._carla_air_executable(),
        ]
        if preview_enabled:
            results.append(
                self._python_module("imageio_ffmpeg", "Install imageio-ffmpeg for local recording export.")
            )
        missing = [asdict(result) for result in results if result.required and not result.present]
        return {
            "platform": platform.platform(),
            "supportedLivePlatform": "Windows or Linux with CARLA-Air/Unreal runtime",
            "livePlatformSupported": platform.system() in {"Windows", "Linux"},
            "results": [asdict(result) for result in results],
            "missing": missing,
            "ok": not missing,
        }

    def _java(self) -> DependencyResult:
        result = self._command("Java 17+", ["java", "-version"], "Install Java 17+ and ensure java is on PATH.")
        if not result.present:
            return result
        match = re.search(r'version "(\d+)', result.detail)
        major = int(match.group(1)) if match else 0
        if major and major < 17:
            result.present = False
            result.guidance = "Install Java 17 or newer."
        return result

    def _python(self) -> DependencyResult:
        executable = sys.executable or shutil.which("python3") or shutil.which("python")
        if not executable:
            return DependencyResult(
                "Python 3",
                True,
                False,
                None,
                "No Python executable found.",
                "Install Python 3 and ensure python3 is on PATH.",
            )
        result = self._command("Python 3", [executable, "--version"], "Install Python 3 and ensure it is on PATH.")
        if result.present and sys.version_info.major < 3:
            result.present = False
            result.guidance = "Install Python 3 and run the supervisor with it."
        return result

    def _ardupilot_source(self) -> DependencyResult:
        candidates = [
            os.environ.get("ARDUPILOT_HOME"),
            os.environ.get("ARDUPILOT_ROOT"),
            str(Path.home() / "ardupilot"),
        ]
        found = next((Path(path) for path in candidates if path and Path(path).exists()), None)
        return DependencyResult(
            name="ArduPilot source or installation",
            required=True,
            present=found is not None,
            version=None,
            detail=str(found) if found else "No ARDUPILOT_HOME/ARDUPILOT_ROOT or ~/ardupilot found.",
            guidance="Clone ArduPilot and set ARDUPILOT_HOME to that checkout.",
        )

    def _ardupilot_gazebo_plugin(self) -> DependencyResult:
        env_path = os.environ.get("ARDUPILOT_GAZEBO_PLUGIN_PATH")
        present = bool(env_path and Path(env_path).exists())
        detail = env_path or "ARDUPILOT_GAZEBO_PLUGIN_PATH is not set."
        return DependencyResult(
            name="ardupilot_gazebo plugin",
            required=True,
            present=present,
            version=None,
            detail=detail,
            guidance="Build the official ArduPilot Gazebo plugin and set ARDUPILOT_GAZEBO_PLUGIN_PATH.",
        )

    def _ros2_package(self, package_name: str) -> DependencyResult:
        if not shutil.which("ros2"):
            return DependencyResult(
                name=package_name,
                required=True,
                present=False,
                version=None,
                detail="ros2 command is not on PATH.",
                guidance=f"Install ROS 2 package {package_name} and source setup.bash.",
            )
        return self._command(
            package_name,
            ["ros2", "pkg", "prefix", package_name],
            f"Install ROS 2 package {package_name}.",
        )

    def _python_module(self, module_name: str, guidance: str) -> DependencyResult:
        try:
            completed = subprocess.run(
                [sys.executable, "-c", f"import {module_name}; print(getattr({module_name}, '__version__', 'installed'))"],
                capture_output=True,
                text=True,
                timeout=8,
                check=False,
            )
            detail = (completed.stdout + completed.stderr).strip()
            return DependencyResult(
                module_name,
                True,
                completed.returncode == 0,
                detail.splitlines()[0] if detail else None,
                detail or f"Python module {module_name} imported.",
                guidance,
            )
        except Exception as exc:
            return DependencyResult(module_name, True, False, None, str(exc), guidance)

    def _carla_air_python_api(self) -> DependencyResult:
        candidates = [
            os.environ.get("CARLAAIR_PYTHON_EXE"),
            os.environ.get("CARLA_AIR_PYTHON"),
            os.environ.get("CARLA_PYTHON"),
            str(REPO_ROOT / ".codex-tools" / "envs" / "carlaAir" / "python.exe"),
            str(REPO_ROOT / ".codex-tools" / "envs" / "carla-0916-py312" / "python.exe"),
            sys.executable,
            shutil.which("python"),
        ]
        for candidate in candidates:
            if not candidate:
                continue
            executable = Path(candidate)
            if not executable.exists():
                continue
            try:
                completed = subprocess.run(
                    [
                        str(executable),
                        "-c",
                        "import carla, airsim, sys; print(sys.executable)",
                    ],
                    capture_output=True,
                    text=True,
                    timeout=8,
                    check=False,
                )
                detail = (completed.stdout + completed.stderr).strip()
                if completed.returncode == 0:
                    return DependencyResult(
                        "CARLA-Air Python API",
                        True,
                        True,
                        "carla+airsim",
                        detail,
                        "CARLA-Air Python environment is ready.",
                    )
            except Exception:
                continue
        return DependencyResult(
            "CARLA-Air Python API",
            True,
            False,
            None,
            "No Python executable with both carla and airsim modules was found.",
            "Run CarlaAir SetupEnv.bat or set CARLAAIR_PYTHON_EXE to the CarlaAir python.exe.",
        )

    def _carla_air_executable(self) -> DependencyResult:
        candidates = [
            os.environ.get("CARLA_AIR_HOME"),
            os.environ.get("CARLA_HOME"),
            str(REPO_ROOT / ".codex-tools" / "carla-air" / "CarlaAir-v0.1.7-Windows11-x86_64"),
            str(REPO_ROOT / ".codex-tools" / "carla"),
            str(Path.home() / "CARLA"),
        ]
        executable_names = [
            "CarlaUE4.exe",
            "CarlaUnreal.exe",
            "CarlaUE4-Win64-Shipping.exe",
            "CarlaUnreal-Win64-Shipping.exe",
            "CarlaUE4.sh",
            "CarlaUnreal.sh",
        ]
        found_path = None
        for command in executable_names:
            located = shutil.which(command)
            if located:
                found_path = Path(located)
                break
        if found_path is None:
            for base in candidates:
                if not base:
                    continue
                root = Path(base)
                for command in executable_names:
                    matches = list(root.rglob(command)) if root.exists() else []
                    if matches:
                        found_path = matches[0]
                        break
                if found_path:
                    break
        return DependencyResult(
            name="CARLA-Air Unreal runtime",
            required=True,
            present=found_path is not None,
            version=None,
            detail=str(found_path) if found_path else "No CARLA_AIR_HOME/CARLA_HOME runtime or CARLA executable found.",
            guidance="Install CARLA-Air/CARLA and set CARLA_AIR_HOME or put the Unreal runtime executable on PATH.",
        )

    @staticmethod
    def _command(
        name: str,
        command: list[str],
        guidance: str,
        expect_zero: bool = True,
    ) -> DependencyResult:
        executable = shutil.which(command[0])
        if executable is None:
            return DependencyResult(name, True, False, None, f"{command[0]} not found on PATH.", guidance)
        try:
            completed = subprocess.run(command, capture_output=True, text=True, timeout=8, check=False)
            detail = (completed.stdout + completed.stderr).strip()
            present = completed.returncode == 0 if expect_zero else True
            first_line = detail.splitlines()[0] if detail else executable
            return DependencyResult(name, True, present, first_line, detail[:2000], guidance)
        except Exception as exc:
            return DependencyResult(name, True, False, None, str(exc), guidance)
