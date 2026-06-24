package net.pickle.rv.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public final class RouteExportHelper {

    private RouteExportHelper() {
    }

    public static String toValidatedWaypointJson(List<Waypoint> waypoints) {
        JsonArray array = new JsonArray();
        int index = 1;

        for (Waypoint waypoint : waypoints) {
            RouteValidatorHelper.BlockXYZ xyz = RouteValidatorHelper.parseLocation(waypoint.location);

            JsonObject obj = new JsonObject();
            obj.addProperty("x", xyz.x());
            obj.addProperty("y", xyz.y());
            obj.addProperty("z", xyz.z());
            obj.addProperty("r", 0);
            obj.addProperty("g", 1);
            obj.addProperty("b", 0);

            JsonObject options = new JsonObject();
            options.addProperty("name", index++);
            obj.add("options", options);

            array.add(obj);
        }

        return array.toString();
    }
}