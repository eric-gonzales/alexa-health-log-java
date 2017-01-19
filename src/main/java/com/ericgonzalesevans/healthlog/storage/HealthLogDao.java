package com.ericgonzalesevans.healthlog.storage;

import com.amazon.speech.speechlet.Session;

/**
 * Contains the methods to interact with the persistence layer for HealthLog in DynamoDB.
 */
public class HealthLogDao {
    private final HealthLogDynamoDbClient dynamoDbClient;

    public HealthLogDao(HealthLogDynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Reads and returns the {@link HealthLogMetrics} using user information from the session.
     * <p>
     * Returns null if the item could not be found in the database.
     * 
     * @param session
     * @return
     */
    public HealthLogMetrics getHealthLogMetrics(Session session) {
        HealthLogUserDataItem item = new HealthLogUserDataItem();
        item.setCustomerId(session.getUser().getUserId());

        item = dynamoDbClient.loadItem(item);

        if (item == null) {
            return null;
        }

        return HealthLogMetrics.newInstance(session, item.getMetricData());
    }

    /**
     * Saves the {@link HealthLogMetrics} into the database.
     * 
     * @param metrics
     */
    public void saveHealthLogMetrics(HealthLogMetrics metrics) {
        HealthLogUserDataItem item = new HealthLogUserDataItem();
        item.setCustomerId(metrics.getSession().getUser().getUserId());
        item.setMetricData(metrics.getMetricData());

        dynamoDbClient.saveItem(item);
    }
}
