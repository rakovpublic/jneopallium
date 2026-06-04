package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

public enum ActionType {
    MOVE_NORTH,
    MOVE_SOUTH,
    MOVE_EAST,
    MOVE_WEST,
    WAIT,
    SCAN_VISIBLE,
    SCAN_LIDAR,
    SCAN_DEPTH,
    SCAN_IR,
    SCAN_THERMAL,
    SCAN_UV,
    SCAN_RADIATION,
    SCAN_RADIO,
    SCAN_RADAR,
    LISTEN,
    SCAN_ULTRASOUND,
    SCAN_MAGNETIC,
    SCAN_CHEMICAL,
    REPORT,
    ASK_OWNER,
    DOCK_CHARGER,
    RESUME_TASK,
    ENTER_SLEEP_OPTIMIZATION,
    WAKE_FROM_SLEEP,
    EMERGENCY_STOP
}
