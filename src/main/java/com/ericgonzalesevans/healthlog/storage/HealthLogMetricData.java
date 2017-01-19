package com.ericgonzalesevans.healthlog.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains user and measurement data to represent metric data.
 */
public class HealthLogMetricData {
    private List<String> users;
    private Map<String, Long> weights;
    private Map<String, Long> heights;

    public HealthLogMetricData() {
        // public no-arg constructor required for DynamoDBMapper marshalling
    }

    /**
     * Creates a new instance of {@link HealthLogMetricData} with initialized but empty user and
     * measurement information.
     * 
     * @return
     */
    public static HealthLogMetricData newInstance() {
        HealthLogMetricData newInstance = new HealthLogMetricData();
        newInstance.setUsers(new ArrayList<String>());
        newInstance.setWeights(new HashMap<String, Long>());
        newInstance.setHeights(new HashMap<String, Long>());
        return newInstance;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public Map<String, Long> getWeights() {
        return weights;
    }

    public void setWeights(Map<String, Long> weights) {
        this.weights = weights;
    }

    public Map<String, Long> getHeights() {
        return heights;
    }

    public void setHeights(Map<String, Long> heights) {
        this.heights = heights;
    }

    @Override
    public String toString() {
        return "[HealthLogMetricData users: " + users + "] weights: " + weights + "] heights: " + heights + "]";
    }
}
