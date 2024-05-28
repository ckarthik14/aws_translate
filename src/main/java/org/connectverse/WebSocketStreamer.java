package org.connectverse;

import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import org.json.JSONObject;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;

public class WebSocketStreamer {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF = 1000;

    public static final String COMMUNICATOR = "communicator";
    public static final String AGENT_RECEIVER = "AGENT_RECEIVER";
    private final ApiGatewayManagementApiClient apiClient;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public WebSocketStreamer(String apiGatewayEndpointUrl, String tableName) {
        this.apiClient = ApiGatewayManagementApiClient.builder()
                .endpointOverride(URI.create(apiGatewayEndpointUrl))
                .build();
        this.dynamoDbClient = DynamoDbClient.create();
        this.tableName = tableName;
    }

    public void streamAudioToConnections(SynthesizeSpeechResult speechResult) {
        String connectionId = getConnectionId();

        System.out.println("Posting to connection: " + connectionId);
        try (InputStream audioStream = speechResult.getAudioStream()) {
            String audio = encodeToBase64(audioStream);

            JSONObject json = new JSONObject();
            json.put("audio_data", audio);
            String jsonData = json.toString();

            // Convert String data to SdkBytes
            SdkBytes dataBytes = SdkBytes.fromUtf8String(jsonData);
            PostToConnectionRequest postRequest = PostToConnectionRequest.builder()
                    .connectionId(connectionId)
                    .data(dataBytes)
                    .build();
            apiClient.postToConnection(postRequest);
        } catch (Exception e) {
            System.err.println("Error posting to WebSocket connection " + connectionId + ": " + e.getMessage());
            // Optionally handle connection cleanup if the connection is gone
        }
    }

    private String getConnectionId() {
        DynamoDBHelper dynamoDBHelper = new DynamoDBHelper();

        try {
            String connectionId = retryQuery(dynamoDBHelper, tableName);
            System.out.println("Got connection ID to stream translated audio to: " + connectionId);
            return connectionId;
        } catch (Exception e) {
            System.err.println("Failed to retrieve connection ID after retries: " + e.getMessage());
        } finally {
            dynamoDBHelper.close();
        }

        throw new RuntimeException("FATAL: Could not get connection ID");
    }

    private static String retryQuery(DynamoDBHelper dynamoDBHelper, String tableName) throws Exception {
        int retries = 0;
        while (true) {
            try {
                QueryResponse response = dynamoDBHelper.queryByCommunicator(tableName, "communicator-index", AGENT_RECEIVER);
                if (!response.items().isEmpty() && response.items().get(0).containsKey("connectionId")) {
                    return response.items().get(0).get("connectionId").s();
                } else {
                    throw new Exception("Connection ID not found in the response");
                }
            } catch (DynamoDbException e) {
                if (++retries > MAX_RETRIES) {
                    throw new Exception("Maximum retry limit reached", e);
                }
                System.err.println("Query failed, retrying... Attempt: " + retries);
                Thread.sleep((long) (INITIAL_BACKOFF * Math.pow(2, retries - 1)));
            }
        }
    }

    public static String encodeToBase64(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        // Ensure all data is written to the buffer
        buffer.flush();

        // Get the byte array from the output stream
        byte[] byteArray = buffer.toByteArray();

        // Encode byte array to Base64 string
        return Base64.getEncoder().encodeToString(byteArray);
    }
}
