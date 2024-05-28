package org.connectverse;

import org.apache.commons.lang3.Validate;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This Subscription converts audio bytes received from the KVS stream into AudioEvents
 * that can be sent to the Transcribe service. It implements a simple demand system that will read chunks of bytes
 * from a KVS stream using the KVS parser library
 *
 * <p>Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.</p>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class KDSByteToAudioEventSubscription implements Subscription {

    private static final int CHUNK_SIZE_IN_BYTES = 1024;
    private final String streamName;
    private final ExecutorService executor = Executors.newFixedThreadPool(1); // Change nThreads here!! used in SubmissionPublisher not subscription
    private final AtomicLong demand = new AtomicLong(0); // state container
    private final Subscriber<? super AudioStream> subscriber;

    private final KinesisClient kinesisClient;
    private final GetShardIteratorRequest shardIteratorRequest;
    private final GetShardIteratorResponse shardIteratorResponse;
    private String shardIterator;

    public KDSByteToAudioEventSubscription(Subscriber<? super AudioStream> s, String streamName) {
        this.subscriber = Validate.notNull(s);
        this.streamName = streamName;

        System.out.println("Stream Name: " + streamName);

        String shardId = "shardId-000000000000"; // Replace with your shard ID
        Region region = Region.US_EAST_1; // Replace with your region

        // Create a Kinesis client
        kinesisClient = KinesisClient.builder().region(region).build();

        // Get an initial shard iterator
        shardIteratorRequest = GetShardIteratorRequest.builder()
                .streamName(streamName)
                .shardId(shardId)
                .shardIteratorType(ShardIteratorType.LATEST)
                .build();

        shardIteratorResponse = kinesisClient.getShardIterator(shardIteratorRequest);
        shardIterator = shardIteratorResponse.shardIterator();
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException("Demand must be positive"));
        }

        demand.getAndAdd(n);

//        System.out.println("Number of records demanded: " + n);

        //We need to invoke this in a separate thread because the call to subscriber.onNext(...) is recursive
        executor.submit(() -> {
            try {
                while (demand.get() > 0) {
                    GetRecordsRequest recordsRequest = GetRecordsRequest.builder()
                            .shardIterator(shardIterator)
                            .limit((int) n)
                            .build();

                    GetRecordsResponse recordsResponse = kinesisClient.getRecords(recordsRequest);

                    List<Record> records = recordsResponse.records();
                    System.out.println("Number of records received: " + records.size());

                    for (Record record : records) {
                        ByteBuffer encodedBytes = record.data().asByteBuffer();
                        ByteBuffer decodedBytes = Base64.getDecoder().decode(record.data().asByteBuffer());

                        CharBuffer b64Buf = StandardCharsets.UTF_8.decode(encodedBytes);
                        System.out.println("Base64 audio: " + b64Buf);

                        // Process the byte buffer as needed
                        System.out.println("Received record with data of size: " + b64Buf.length());

                        AudioEvent audioEvent = audioEventFromBuffer(decodedBytes);
                        subscriber.onNext(audioEvent);
                        demand.decrementAndGet();
                    }

                    shardIterator = recordsResponse.nextShardIterator();

//                if (records.size() < n) {
//                    subscriber.onComplete();
//                }
                }
            } catch (Exception e) {
                System.out.println("Got an exception while sending for transcription: ");
                e.printStackTrace();
                subscriber.onError(e);
            }
        });
    }

    @Override
    public void cancel() {
        executor.shutdown();
    }

    private AudioEvent audioEventFromBuffer(ByteBuffer sdkBytes) {
        return AudioEvent.builder()
                .audioChunk(SdkBytes.fromByteBuffer(sdkBytes))
                .build();
    }
}
