package com.ericgonzalesevans.healthlog.storage;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Model representing an item of the HealthLogUserData table in DynamoDB for the HealthLog
 * skill.
 */
@DynamoDBTable(tableName = "HealthLogUserData")
public class HealthLogUserDataItem {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String customerId;

    private HealthLogMetricData metricData;

    @DynamoDBHashKey(attributeName = "CustomerId")
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @DynamoDBAttribute(attributeName = "Data")
    @DynamoDBMarshalling(marshallerClass = HealthLoggerMetricDataMarshaller.class)
    public HealthLogMetricData getMetricData() {
        return metricData;
    }

    public void setMetricData(HealthLogMetricData metricData) {
        this.metricData = metricData;
    }

    /**
     * A {@link DynamoDBMarshaller} that provides marshalling and unmarshalling logic for
     * {@link HealthLogMetricData} values so that they can be persisted in the database as String.
     */
    public static class HealthLoggerMetricDataMarshaller implements
            DynamoDBMarshaller<HealthLogMetricData> {

        @Override
        public String marshall(HealthLogMetricData metricData) {
            try {
                return OBJECT_MAPPER.writeValueAsString(metricData);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unable to marshall metric data", e);
            }
        }

        @Override
        public HealthLogMetricData unmarshall(Class<HealthLogMetricData> clazz, String value) {
            try {
                return OBJECT_MAPPER.readValue(value, new TypeReference<HealthLogMetricData>() {
                });
            } catch (Exception e) {
                throw new IllegalStateException("Unable to unmarshall metric data value", e);
            }
        }
    }
}
