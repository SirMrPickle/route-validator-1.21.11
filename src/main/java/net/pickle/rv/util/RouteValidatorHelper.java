package net.pickle.rv.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
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

    public static boolean matchesAny(BlockState state, List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return false;
        }

        for (Block block : blocks) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAllowedBlockInRange(BlockGetter level, BlockPos center, int range, List<Block> blocks) {
        int actualRange = Math.max(0, range - 1);

        for (int dx = -actualRange; dx <= actualRange; dx++) {
            for (int dy = -actualRange; dy <= actualRange; dy++) {
                for (int dz = -actualRange; dz <= actualRange; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (matchesAny(level.getBlockState(pos), blocks)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static ValidationResult validateWaypoints(BlockGetter level, List<Waypoint> waypoints, int range, List<Block> blocks) {
        List<Waypoint> validWaypoints = new ArrayList<>();
        List<Waypoint> invalidWaypoints = new ArrayList<>();

        for (Waypoint waypoint : waypoints) {
            BlockPos pos = toBlockPos(waypoint.location);

            if (hasAllowedBlockInRange(level, pos, range, blocks)) {
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
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("RV").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("Validation complete: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(result.validCount())).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" valid, ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(result.invalidCount())).withStyle(ChatFormatting.RED))
                .append(Component.literal(" invalid").withStyle(ChatFormatting.WHITE));
    }
}