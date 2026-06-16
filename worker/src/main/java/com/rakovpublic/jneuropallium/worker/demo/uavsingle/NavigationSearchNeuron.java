package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.ArrayList;
import java.util.List;

public class NavigationSearchNeuron {
    private final SearchCoverageNeuron coverageNeuron = new SearchCoverageNeuron();

    public List<SearchWaypointSignal> plan(SearchArea area, UavSingleConfig config, long tick) {
        List<SearchWaypointSignal> signals = new ArrayList<>();
        List<UavPose> waypoints = coverageNeuron.plan(area, config);
        for (int i = 0; i < waypoints.size(); i++) {
            signals.add(new SearchWaypointSignal(config.missionId, config.uavId, tick, area.areaId, i, waypoints.get(i)));
        }
        return signals;
    }
}
