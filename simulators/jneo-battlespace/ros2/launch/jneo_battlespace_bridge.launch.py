from launch import LaunchDescription
from launch.actions import DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration
from launch_ros.actions import Node


def generate_launch_description():
    bridge_config = LaunchConfiguration("bridge_config")
    return LaunchDescription(
        [
            DeclareLaunchArgument(
                "bridge_config",
                default_value="simulators/jneo-battlespace/ros2/config/ros-gz-bridge.yaml",
            ),
            Node(
                package="ros_gz_bridge",
                executable="parameter_bridge",
                name="jneo_battlespace_ros_gz_bridge",
                parameters=[{"config_file": bridge_config}],
                output="screen",
            ),
        ]
    )
