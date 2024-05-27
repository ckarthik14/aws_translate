package org.connectverse;

import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import org.json.JSONObject;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;

public class WebSocketStreamer {

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
        List<String> connectionIds = getConnectionIds();
        System.out.println("Connection IDs: " + connectionIds);

        for (String connectionId : connectionIds) {
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
    }

    private List<String> getConnectionIds() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .build();
        var scanResponse = dynamoDbClient.scan(scanRequest);
        return scanResponse.items().stream().map(item -> item.get("connectionId").s()).toList();
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
