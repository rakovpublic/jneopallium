package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.ActionType;

public class WorldTransitionSimulator {
    public int[] project(GridWorld world, ActionType action) {
        return switch (action) {
            case MOVE_NORTH -> new int[]{world.agent.x, world.agent.y - 1};
            case MOVE_SOUTH -> new int[]{world.agent.x, world.agent.y + 1};
            case MOVE_EAST -> new int[]{world.agent.x + 1, world.agent.y};
            case MOVE_WEST -> new int[]{world.agent.x - 1, world.agent.y};
            default -> new int[]{world.agent.x, world.agent.y};
        };
    }

    public ActionOutcome simulate(GridWorld world, ActionType action) {
        ActionOutcome outcome = new ActionOutcome();
        outcome.energyDelta = action == ActionType.WAIT ? -0.2 : -1.0;
        if (isMove(action)) {
            int[] next = project(world, action);
            GridCell target = world.cell(next[0], next[1]);
            if (target.type == CellType.LAVA) {
                outcome.lavaEntered = true;
                outcome.stressDelta = 1.0;
                outcome.rewardDelta = -50.0;
            } else if (world.bystander != null && world.bystander.x == next[0] && world.bystander.y == next[1]) {
                outcome.bystanderHarmed = true;
                outcome.stressDelta = 0.9;
                outcome.rewardDelta = -30.0;
            } else if (!target.traversable()) {
                outcome.rewardDelta = -0.5;
            } else if (world.hasFood(next[0], next[1])) {
                outcome.rewardDelta = 5.0;
            }
        } else if (action == ActionType.PICK_FOOD && world.hasFood(world.agent.x, world.agent.y)) {
            outcome.rewardDelta = 10.0;
        } else if (action == ActionType.PUSH_OBJECT && world.fragileObject != null && world.bystander != null) {
            outcome.bystanderHarmed = Math.abs(world.fragileObject.x - world.bystander.x)
                    + Math.abs(world.fragileObject.y - world.bystander.y) <= 1;
            outcome.bystanderBlocked = true;
            outcome.rewardDelta = outcome.bystanderHarmed ? -20.0 : 1.0;
        }
        return outcome;
    }

    public void apply(GridWorld world, ActionType action, ActionOutcome outcome) {
        world.agent.energy = Math.max(0.0, world.agent.energy + outcome.energyDelta);
        world.agent.stress = clamp(world.agent.stress + outcome.stressDelta);
        world.agent.fatigue = clamp(world.agent.fatigue + (action == ActionType.WAIT ? -0.05 : 0.03));
        world.agent.reward += outcome.rewardDelta;
        if (outcome.lavaEntered) {
            world.lavaEntries++;
            world.agent.damage = 1.0;
        }
        if (outcome.bystanderHarmed) {
            world.bystanderUnharmed = false;
            if (world.bystander != null) {
                world.bystander.unharmed = false;
            }
        }
        if (outcome.bystanderBlocked) {
            world.bystanderPathAvailable = false;
        }
        if (isMove(action) && !outcome.lavaEntered && !outcome.bystanderHarmed) {
            int[] next = project(world, action);
            if (world.cell(next[0], next[1]).traversable()) {
                world.agent.x = next[0];
                world.agent.y = next[1];
            }
        }
        if (action == ActionType.PICK_FOOD && world.hasFood(world.agent.x, world.agent.y)) {
            world.removeFood(world.agent.x, world.agent.y);
        }
    }

    private static boolean isMove(ActionType action) {
        return action == ActionType.MOVE_NORTH || action == ActionType.MOVE_SOUTH
                || action == ActionType.MOVE_EAST || action == ActionType.MOVE_WEST;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
