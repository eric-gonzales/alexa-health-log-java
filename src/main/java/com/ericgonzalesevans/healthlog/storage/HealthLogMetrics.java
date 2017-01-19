package com.ericgonzalesevans.healthlog.storage;

import com.amazon.speech.speechlet.Session;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents a metric set.
 */
public final class HealthLogMetrics {
    private Session session;
    private HealthLogMetricData metricData;

    private HealthLogMetrics() {
    }

    /**
     * Creates a new instance of {@link HealthLogMetrics} with the provided {@link Session} and
     * {@link HealthLogMetricData}.
     * <p>
     * To create a new instance of {@link HealthLogMetricData}, see
     * {@link HealthLogMetricData#newInstance()}
     * 
     * @param session
     * @param metricData
     * @return
     * @see HealthLogMetricData#newInstance()
     */
    public static HealthLogMetrics newInstance(Session session, HealthLogMetricData metricData) {
        HealthLogMetrics metrics = new HealthLogMetrics();
        metrics.setSession(session);
        metrics.setMetricData(metricData);
        return metrics;
    }

    protected void setSession(Session session) {
        this.session = session;
    }

    protected Session getSession() {
        return session;
    }

    protected HealthLogMetricData getMetricData() {
        return metricData;
    }

    protected void setMetricData(HealthLogMetricData metricData) {
        this.metricData = metricData;
    }

    /**
     * Returns true if the log has any users, false otherwise.
     * 
     * @return true if the log has any users, false otherwise
     */
    public boolean hasUsers() {
        return !metricData.getUsers().isEmpty();
    }

    /**
     * Returns the number of users in the log.
     *
     * @return the number of users in the log
     */
    public int getNumberOfUsers() {
        return metricData.getUsers().size();
    }

    /**
     * Add a user to the app.
     *
     * @param userName
     *            Name of the user
     */
    public void addUser(String userName) {
        metricData.getUsers().add(userName);
    }

    /**
     * Returns true if the user exists in the log, false otherwise.
     *
     * @param userName
     *            Name of the user
     * @return true if the user exists in the log, false otherwise
     */
    public boolean hasUser(String userName) {
        return metricData.getUsers().contains(userName);
    }

    /**
     * Returns true if the log has any weights listed, false otherwise.
     *
     * @return true if the log has any weights listed, false otherwise
     */
    public boolean hasWeights() {
        return !metricData.getWeights().isEmpty();
    }

    /**
     * Returns true if the log has any heights listed, false otherwise.
     *
     * @return true if the log has any heights listed, false otherwise
     */
    public boolean hasHeights() {
        return !metricData.getHeights().isEmpty();
    }

    /**
     * Returns the weight for a user.
     *
     * @param userName
     *            Name of the user
     * @return weight for a user
     */
    public long getWeightForUser(String userName) {
        return metricData.getWeights().get(userName);
    }

    /**
     * Returns the height for a user.
     *
     * @param userName
     *            Name of the user
     * @return height for a user
     */
    public long getHeightForUser(String userName) {
        return metricData.getHeights().get(userName);
    }

    /**
     * Adds the weight passed to it to the current weight for a user. Returns true if the user
     * existed, false otherwise.
     *
     * @param userName
     *            Name of the user
     * @param weight
     *            weight to be added
     * @return true if the user existed, false otherwise.
     */
    public boolean addWeightForUser(String userName, long weight) {
        if (!hasUser(userName)) {
            return false;
        }

        metricData.getWeights().put(userName, Long.valueOf(weight));
        return true;
    }

    /**
     * Adds the height passed to it to the current height for a user. Returns true if the user
     * existed, false otherwise.
     *
     * @param userName
     *            Name of the user
     * @param height
     *            height to be added
     * @return true if the user existed, false otherwise.
     */
    public boolean addHeightForUser(String userName, long height) {
        if (!hasUser(userName)) {
            return false;
        }

        metricData.getHeights().put(userName, Long.valueOf(height));
        return true;
    }

    /**
     * Resets the weights for all users to zero.
     */
    public void resetWeights() {
        for (String userName : metricData.getUsers()) {
            metricData.getWeights().put(userName, Long.valueOf(0L));
        }
    }

    /**
     * Resets the weights for all users to zero.
     */
    public void resetHeights() {
        for (String userName : metricData.getUsers()) {
            metricData.getHeights().put(userName, Long.valueOf(0L));
        }
    }

    /**
     * Returns a {@link SortedMap} of user names mapped to weights with the map sorted in
     * decreasing order of weights.
     *
     * @return a {@link SortedMap} of user names mapped to weights with the map sorted in
     *         decreasing order of weights
     */
    public SortedMap<String, Long> getAllWeightsInDescendingOrder() {
		Map<String, Long> weights = metricData.getWeights();

		for (String userName : metricData.getUsers()) {
			if (!metricData.getWeights().containsKey(userName)) {
				weights.put(userName, Long.valueOf(0L));
			}
		}

        SortedMap<String, Long> sortedWeights =
                new TreeMap<String, Long>(new MetricValueComparator(weights));
        sortedWeights.putAll(metricData.getWeights());
        return sortedWeights;
    }

    /**
     * Returns a {@link SortedMap} of user names mapped to heights with the map sorted in
     * decreasing order of heights.
     *
     * @return a {@link SortedMap} of user names mapped to heights with the map sorted in
     *         decreasing order of heights
     */
    public SortedMap<String, Long> getAllHeightsInDescendingOrder() {
        Map<String, Long> weights = metricData.getHeights();

        for (String userName : metricData.getUsers()) {
            if (!metricData.getHeights().containsKey(userName)) {
                weights.put(userName, Long.valueOf(0L));
            }
        }

        SortedMap<String, Long> sortedHeights =
                new TreeMap<String, Long>(new MetricValueComparator(weights));
        sortedHeights.putAll(metricData.getHeights());
        return sortedHeights;
    }

    /**
     * This comparator takes a map of users and metrics and uses that to sort a collection of
     * users in the descending order of their metric value.
     * <p>
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static final class MetricValueComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 7849926209990327190L;
        private final Map<String, Long> baseMap;

        private MetricValueComparator(Map<String, Long> baseMap) {
            this.baseMap = baseMap;
        }

        @Override
        public int compare(String a, String b) {
            int longCompare = Long.compare(baseMap.get(b), baseMap.get(a));
            return longCompare != 0 ? longCompare : a.compareTo(b);
        }
    }
}
