package org.connectverse;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.services.kinesis.model.Record;

import java.nio.ByteBuffer;
import java.util.List;

public class KinesisStreamConsumer {
    public static void main(String[] args) {
        String streamName = "YourStreamName"; // Replace with your stream name
        String shardId = "shardId-000000000000"; // Replace with your shard ID
        Region region = Region.US_EAST_1; // Replace with your region

        // Create a Kinesis client
        KinesisClient kinesisClient = KinesisClient.builder()
                .region(region)
                .build();

        // Get an initial shard iterator
        GetShardIteratorRequest shardIteratorRequest = GetShardIteratorRequest.builder()
                .streamName(streamName)
                .shardId(shardId)
                .shardIteratorType(ShardIteratorType.LATEST)
                .build();

        GetShardIteratorResponse shardIteratorResponse = kinesisClient.getShardIterator(shardIteratorRequest);
        String shardIterator = shardIteratorResponse.shardIterator();

        // Continuously read records from the shard
        while (true) {
            // Fetch records based on the shard iterator
            GetRecordsRequest recordsRequest = GetRecordsRequest.builder()
                    .shardIterator(shardIterator)
                    .limit(100)
                    .build();

            GetRecordsResponse recordsResponse = kinesisClient.getRecords(recordsRequest);

            List<Record> records = recordsResponse.records();
            for (Record record : records) {
                SdkBytes byteBuffer = record.data();
                // Process the byte buffer as needed
                System.out.println("Received record with data: " + byteBuffer.toString());
            }

            // Update the shard iterator
            shardIterator = recordsResponse.nextShardIterator();

            // Handle the case where the shard has been closed
            if (shardIterator == null) break;

            // Throttling to avoid hitting the limit
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        kinesisClient.close();
    }
}

