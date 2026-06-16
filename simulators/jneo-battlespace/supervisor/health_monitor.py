from __future__ import annotations

import os
import platform
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass, asdict
from pathlib import Path


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
