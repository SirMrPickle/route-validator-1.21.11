package net.pickle.rv.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class RouteValidatorHelper {

    private RouteValidatorHelper() {
    }

    public record BlockXYZ(int x, int y, int z) {
    }

    public record ValidationResult(
            int validCount,
            int invalidCount,
            List<Waypoint> validWaypoints,
            List<Waypoint> invalidWaypoints
    ) {
    }

    public static BlockXYZ parseLocation(String location) {
        String[] parts = location.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid location format: " + location);
        }

        int x = (int) Math.floor(Double.parseDouble(parts[0]));
        int y = (int) Math.floor(Double.parseDouble(parts[1]));
        int z = (int) Math.floor(Double.parseDouble(parts[2]));

        return new BlockXYZ(x, y, z);
    }

    public static BlockPos toBlockPos(String location) {
        BlockXYZ xyz = parseLocation(location);
        return new BlockPos(xyz.x(), xyz.y(), xyz.z());
    }

    public static boolean isCoalOre(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.COAL_ORE);
    }

    public static boolean hasCoalOreInRange(Level level, BlockPos center, int range) {
        int actualRange = Math.max(0, range - 1);

        for (int dx = -actualRange; dx <= actualRange; dx++) {
            for (int dy = -actualRange; dy <= actualRange; dy++) {
                for (int dz = -actualRange; dz <= actualRange; dz++) {
                    if (isCoalOre(level, center.offset(dx, dy, dz))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static ValidationResult validateWaypoints(Level level, List<Waypoint> waypoints, int range) {
        List<Waypoint> validWaypoints = new ArrayList<>();
        List<Waypoint> invalidWaypoints = new ArrayList<>();

        for (Waypoint waypoint : waypoints) {
            BlockPos pos = toBlockPos(waypoint.location);

            if (hasCoalOreInRange(level, pos, range)) {
                validWaypoints.add(waypoint);
            } else {
                invalidWaypoints.add(waypoint);
            }
        }

        return new ValidationResult(
                validWaypoints.size(),
                invalidWaypoints.size(),
                validWaypoints,
                invalidWaypoints
        );
    }

    public static MutableComponent formatResult(ValidationResult result) {
        return Component.literal("[")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                .append(Component.literal("R").withStyle(net.minecraft.ChatFormatting.GOLD))
                .append(Component.literal("V").withStyle(net.minecraft.ChatFormatting.AQUA))
                .append(Component.literal("] ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
                .append(Component.literal("Validation complete: ").withStyle(net.minecraft.ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(result.validCount())).withStyle(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal(" valid, ").withStyle(net.minecraft.ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(result.invalidCount())).withStyle(net.minecraft.ChatFormatting.RED))
                .append(Component.literal(" invalid").withStyle(net.minecraft.ChatFormatting.WHITE));
    }
}