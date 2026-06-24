package net.pickle.rv;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.pickle.rv.util.*;

import java.util.List;
import java.util.Map;
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
                                .executes(context -> checkRoute(context, 3))
                                .then(ClientCommandManager.argument("range", IntegerArgumentType.integer(1, 5))
                                        .executes(context -> checkRoute(context, IntegerArgumentType.getInteger(context, "range"))))));
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestRoutes(
            CommandContext<FabricClientCommandSource> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        try {
            Map<String, List<Waypoint>> routes = JSONLoader.loadRoutesFromDefaultLocation();
            routes.keySet().forEach(builder::suggest);
        } catch (Exception ignored) {
        }
        return builder.buildFuture();
    }

    private static int listRoutes(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();

        try {
            Map<String, List<Waypoint>> routes = JSONLoader.loadRoutesFromDefaultLocation();

            String routeNames = routes.keySet().stream()
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.joining(", "));

            source.sendFeedback(routeNames.isBlank()
                    ? Message.warning("No routes found.")
                    : Message.info("Routes: " + routeNames));

            return 1;
        } catch (Exception e) {
            source.sendError(Message.error("Failed to load routes: " + e.getMessage()));
            return 0;
        }
    }

    private static int checkRoute(CommandContext<FabricClientCommandSource> context, int range) {
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
                    RouteValidatorHelper.validateWaypoints(source.getWorld(), waypoints, range);

            source.sendFeedback(RouteValidatorHelper.formatResult(result));

            String json = RouteExportHelper.toValidatedWaypointJson(result.validWaypoints());
            source.sendFeedback(Message.copyButton("Click to copy validated JSON", json));

            return 1;
        } catch (Exception e) {
            source.sendError(Message.error("Route validation failed: " + e.getMessage()));
            return 0;
        }
    }
}