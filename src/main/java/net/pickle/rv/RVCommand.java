package net.pickle.rv;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.pickle.rv.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class RVCommand {

    private RVCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("rv")
                .then(ClientCommandManager.literal("list")
                        .executes(RVCommand::listRoutes))
                .then(ClientCommandManager.literal("validate")
                        .then(ClientCommandManager.argument("routeName", StringArgumentType.word())
                                .suggests(RVCommand::suggestRoutes)
                                .executes(context -> validateRoute(context, 3, List.of()))
                                .then(ClientCommandManager.argument("range", IntegerArgumentType.integer(1, 3))
                                        .executes(context -> validateRoute(context, IntegerArgumentType.getInteger(context, "range"), List.of()))
                                        .then(ClientCommandManager.argument("blocks", StringArgumentType.greedyString())
                                                .suggests(RVCommand::suggestBlocks)
                                                .executes(context -> validateRoute(
                                                        context,
                                                        IntegerArgumentType.getInteger(context, "range"),
                                                        parseBlocks(StringArgumentType.getString(context, "blocks"))
                                                ))))))
                .then(ClientCommandManager.literal("prep")
                        .then(ClientCommandManager.argument("route", StringArgumentType.word())
                                .suggests(RVCommand::suggestRoutes)
                                .executes(RVCommand::prepRoute)));
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestRoutes(
            CommandContext<FabricClientCommandSource> context,
            SuggestionsBuilder builder
    ) {
        try {
            Map<String, List<Waypoint>> routes = JSONLoader.loadRoutesFromDefaultLocation();
            routes.keySet().forEach(builder::suggest);
        } catch (Exception ignored) {
        }
        return builder.buildFuture();
    }

    private static int prepRoute(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        String route = StringArgumentType.getString(context, "route");

        try {
            Map<String, List<Waypoint>> routes = JSONLoader.loadRoutesFromDefaultLocation();
            List<Waypoint> waypoints = routes.get(route);
            List<Block> list = new ArrayList<>();

            BuiltInRegistries.BLOCK.getOptional(Identifier.tryParse("minecraft:coal_ore"))
                    .ifPresent(list::add);

            if (waypoints == null) {
                source.sendError(Message.error("Route not found: " + route));
                return 0;
            }

            RouteValidatorHelper.ValidationResult result =
                    RouteValidatorHelper.validateWaypoints(source.getWorld(), waypoints, 3, list);

            String validWaypointJson = RouteExportHelper.toValidatedWaypointJson(result.validWaypoints());

            Minecraft mc = Minecraft.getInstance();
            source.sendFeedback(Message.info("Unloading old waypoints..."));
            mc.getConnection().sendCommand("sho unload");
            mc.keyboardHandler.setClipboard(validWaypointJson);
            source.sendFeedback(RouteValidatorHelper.formatResult(result));
            mc.getConnection().sendCommand("sho load");
            source.sendFeedback(Message.info("New waypoints loaded!"));

            return 1;
        } catch (Exception e) {
            source.sendError(Message.error("Prep failed: " + e.getMessage()));
            return 0;
        }
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestBlocks(
            CommandContext<FabricClientCommandSource> context,
            SuggestionsBuilder builder
    ) {
        try {
            String remaining = builder.getRemaining();
            String prefix = remaining.contains(",")
                    ? remaining.substring(0, remaining.lastIndexOf(',') + 1)
                    : "";

            String lastToken = remaining.contains(",")
                    ? remaining.substring(remaining.lastIndexOf(',') + 1).trim()
                    : remaining.trim();

            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceKey<Block> key = BuiltInRegistries.BLOCK.getResourceKey(block).orElse(null);
                if (key == null) {
                    continue;
                }

                String idString = key.identifier().toString();
                if (lastToken.isEmpty() || idString.startsWith(lastToken)) {
                    builder.suggest(prefix + idString);
                }
            }
        } catch (Exception ignored) {
        }
        return builder.buildFuture();
    }

    private static List<Block> parseBlocks(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        List<Block> blocks = new ArrayList<>();
        for (String part : input.split(",")) {
            String id = part.trim();
            if (id.isEmpty()) {
                continue;
            }

            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                continue;
            }

            Block block = BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null);
            if (block != null) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    private static int listRoutes(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();

        try {
            Map<String, List<Waypoint>> routes = JSONLoader.loadRoutesFromDefaultLocation();
            String routeNames = routes.keySet().stream().collect(Collectors.joining(", "));
            source.sendFeedback(routeNames.isBlank()
                    ? Message.warning("No routes found.")
                    : Message.info("Routes: " + routeNames));
            return 1;
        } catch (Exception e) {
            source.sendError(Message.error("Failed to load routes: " + e.getMessage()));
            return 0;
        }
    }

    private static int validateRoute(CommandContext<FabricClientCommandSource> context, int range, List<Block> blocks) {
        FabricClientCommandSource source = context.getSource();
        String routeName = StringArgumentType.getString(context, "routeName");

        try {
            Map<String, List<Waypoint>> routes = JSONLoader.loadRoutesFromDefaultLocation();
            List<Waypoint> waypoints = routes.get(routeName);

            if (waypoints == null) {
                source.sendError(Message.error("Route not found: " + routeName));
                return 0;
            }

            RouteValidatorHelper.ValidationResult result =
                    RouteValidatorHelper.validateWaypoints(source.getWorld(), waypoints, range, blocks);

            source.sendFeedback(RouteValidatorHelper.formatResult(result));

            String json = RouteExportHelper.toValidatedWaypointJson(result.validWaypoints());
            source.sendFeedback(Message.copyButton("Click to copy validated JSON", json));

            return 1;
        } catch (Exception e) {
            source.sendError(Message.error("Validation failed: " + e.getMessage()));
            return 0;
        }
    }
}