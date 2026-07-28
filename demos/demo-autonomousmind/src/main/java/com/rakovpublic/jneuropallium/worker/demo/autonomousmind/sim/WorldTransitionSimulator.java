package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.ActionType;

public class WorldTransitionSimulator {
    public boolean wouldEnterForbiddenOrUnsafe(SimulatedEnvironment environment, ActionType action) {
        int[] next = nextPosition(environment.agent.x, environment.agent.y, action);
        EnvironmentCell cell = environment.map.cell(next[0], next[1]);
        return cell.obstacle || cell.forbiddenZone || cell.radiationAnomaly
                || (environment.bystander != null && environment.bystander.x == next[0] && environment.bystander.y == next[1]);
    }

    public boolean wouldScanPrivateRegion(SimulatedEnvironment environment, ActionType action) {
        if (!action.name().startsWith("SCAN") && action != ActionType.REPORT) {
            return false;
        }
        for (int y = Math.max(0, environment.agent.y - 1); y <= environment.agent.y + 1; y++) {
            for (int x = Math.max(0, environment.agent.x - 1); x <= environment.agent.x + 1; x++) {
                if (environment.map.cell(x, y).privacySensitive) {
                    return true;
                }
            }
        }
        return false;
    }

    public void apply(SimulatedEnvironment environment, ActionType action) {
        if (action == ActionType.DOCK_CHARGER) {
            environment.agent.moveTo(environment.chargingStation.x, environment.chargingStation.y);
            environment.energy.docked = true;
            environment.energy.charge(environment.chargingStation.chargePerTick);
            return;
        }
        if (environment.energy.charging || action == ActionType.ENTER_SLEEP_OPTIMIZATION) {
            environment.energy.charge(environment.chargingStation.chargePerTick);
            return;
        }
        if (isMovement(action)) {
            int[] next = nextPosition(environment.agent.x, environment.agent.y, action);
            if (environment.map.canTraverse(next[0], next[1])) {
                environment.agent.moveTo(next[0], next[1]);
            }
            environment.energy.consume(2.5);
        } else if (action.name().startsWith("SCAN") || action == ActionType.LISTEN) {
            environment.energy.consume(1.1);
        } else {
            environment.energy.consume(0.4);
        }
    }

    private static boolean isMovement(ActionType action) {
        return action == ActionType.MOVE_NORTH || action == ActionType.MOVE_SOUTH
                || action == ActionType.MOVE_EAST || action == ActionType.MOVE_WEST;
    }

    private static int[] nextPosition(int x, int y, ActionType action) {
        return switch (action) {
            case MOVE_NORTH -> new int[]{x, y - 1};
            case MOVE_SOUTH -> new int[]{x, y + 1};
            case MOVE_EAST -> new int[]{x + 1, y};
            case MOVE_WEST -> new int[]{x - 1, y};
            default -> new int[]{x, y};
        };
    }
}
