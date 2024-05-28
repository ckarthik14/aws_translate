package org.connectverse;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Map;

public class DynamoDBHelper {
    private final DynamoDbClient dbClient;

    public DynamoDBHelper() {
        // Initialize DynamoDB client with the default credentials and specified region
        this.dbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    public QueryResponse queryByCommunicator(String tableName, String indexName, String communicatorValue) {
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName(indexName)
                    .keyConditionExpression("communicator = :v_communicator")
                    .expressionAttributeValues(Map.of(":v_communicator", AttributeValue.builder().s(communicatorValue).build()))
                    .build();

            return dbClient.query(queryRequest);
        } catch (DynamoDbException e) {
            System.err.println("Error querying DynamoDB: " + e.getMessage());
            throw e;  // Re-throw the exception after logging
        }
    }

    public void close() {
        // Close the DynamoDB client connection
        dbClient.close();
    }
}

