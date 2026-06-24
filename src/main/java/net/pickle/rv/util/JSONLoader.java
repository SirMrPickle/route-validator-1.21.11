package net.pickle.rv.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JSONLoader {

    private static final Gson GSON = new Gson();
    private static final Path DEBUG_PATH = Path.of("/Users/evanspezzano/Desktop/routes.json");
    private static final Path DEFAULT_PATH = FabricLoader.getInstance().getConfigDir().resolve("skyhanni").resolve("routes.json");
    private static final Type WAYPOINT_LIST_TYPE = new TypeToken<List<Waypoint>>() {}.getType();

    private JSONLoader() {
    }

    public static Map<String, List<Waypoint>> loadRoutesFromDefaultLocation() throws IOException {
        return loadRoutes(DEFAULT_PATH);
    }

    public static Map<String, List<Waypoint>> loadRoutes(Path filePath) throws IOException {
        String raw = Files.readString(filePath);

        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
        JsonObject routesObj = root.getAsJsonObject("routes");

        Map<String, List<Waypoint>> routes = new LinkedHashMap<>();

        if (routesObj == null) {
            return routes;
        }

        for (Map.Entry<String, JsonElement> entry : routesObj.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isJsonArray()) {
                List<Waypoint> waypoints = GSON.fromJson(entry.getValue(), WAYPOINT_LIST_TYPE);
                routes.put(entry.getKey(), waypoints);
            }
        }

        return routes;
    }
}