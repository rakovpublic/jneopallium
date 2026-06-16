from __future__ import annotations

from dataclasses import dataclass, asdict
from pathlib import Path


@dataclass(frozen=True)
class VehicleBinding:
    uavId: str
    systemId: int
    instance: int
    mavlinkEndpoint: str
    rosNamespace: str
    cameraTopic: str
    imuTopic: str
    rangeTopic: str
    odometryTopic: str
    outputDir: str
    sitlLog: str


def generate_vehicle_map(count: int, run_dir: Path) -> dict:
    if count < 1:
        raise ValueError("vehicle count must be positive")
    vehicles: list[VehicleBinding] = []
    for index in range(count):
        number = index + 1
        namespace = f"/uav_{number}"
        mavlink_port = 14550 + index * 10
        vehicle_dir = run_dir / f"uav-{number}"
        vehicles.append(
            VehicleBinding(
                uavId=f"uav-{number}",
                systemId=number,
                instance=index,
                mavlinkEndpoint=f"udp://127.0.0.1:{mavlink_port}",
                rosNamespace=namespace,
                cameraTopic=f"{namespace}/fpv/image",
                imuTopic=f"{namespace}/imu",
                rangeTopic=f"{namespace}/range",
                odometryTopic=f"{namespace}/odometry",
                outputDir=str(vehicle_dir),
                sitlLog=str(vehicle_dir / "sitl.log"),
            )
        )
    return {"vehicles": [asdict(vehicle) for vehicle in vehicles]}

