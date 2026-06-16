package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.ArrayList;
import java.util.List;

public class SearchCoverageNeuron {
    public List<UavPose> plan(SearchArea area, UavSingleConfig config) {
        area.validate(config);
        List<UavPose> waypoints = new ArrayList<>();
        int rows = Math.max(1, (int) Math.ceil((area.maxY - area.minY) / area.spacingMeters) + 1);
        int columns = Math.max(1, (int) Math.ceil((area.maxX - area.minX) / area.spacingMeters) + 1);
        for (int row = 0; row < rows; row++) {
            double y = Math.min(area.maxY, area.minY + row * area.spacingMeters);
            boolean reverse = row % 2 == 1;
            for (int column = 0; column < columns; column++) {
                int effectiveColumn = reverse ? columns - 1 - column : column;
                double x = Math.min(area.maxX, area.minX + effectiveColumn * area.spacingMeters);
                if (isAllowedSearchPoint(x, y, config)) {
                    waypoints.add(new UavPose(x, y, area.altitudeMeters, 0.0, 1.0, 1.0));
                }
            }
        }
        return waypoints;
    }

    private static boolean isAllowedSearchPoint(double x, double y, UavSingleConfig config) {
        if (!config.geofence.contains(x, y)) {
            return false;
        }
        for (NoGoZone zone : config.noGoZones) {
            if (zone.contains(x, y)) {
                return false;
            }
        }
        return true;
    }
}
