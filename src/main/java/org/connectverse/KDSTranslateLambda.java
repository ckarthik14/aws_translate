package org.connectverse;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrate Amazon Connect's real-time transcription feature using AWS Kinesis Video Streams and AWS Transcribe.
 * The data flow is :
 * <p>
 * Amazon Connect => AWS KVS => AWS Transcribe => AWS DynamoDB
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
public class KDSTranslateLambda implements RequestHandler<TranscriptionRequest, String> {
    private static final Regions TRANSCRIBE_REGION = Regions.fromName("us-east-1");
    private static final String TRANSCRIBE_ENDPOINT = "https://transcribestreaming." + TRANSCRIBE_REGION.getName() + ".amazonaws.com";

    private static final Logger logger = LoggerFactory.getLogger(KDSTranslateLambda.class);

    /**
     * Handler function for the Lambda
     *
     * @param request
     * @param context
     * @return
     */
    @Override
    public String handleRequest(TranscriptionRequest request, Context context) {

        logger.info("received request : " + request.toString());

        String streamARN = request.getStreamARN();
        String streamName = streamARN.substring(streamARN.indexOf("/") + 1, streamARN.lastIndexOf("/"));

        try {
            // validate the request
            request.validate();

            try (TranscribeStreamingRetryClient client = new TranscribeStreamingRetryClient(getTranscribeCredentials(),
                    TRANSCRIBE_ENDPOINT, TRANSCRIBE_REGION)) {

                logger.info("Calling Transcribe service..");
                CompletableFuture<Void> result = null;

                result = getStartStreamingTranscriptionFuture(client, streamName, request);

                // Synchronous wait for stream to close, and close client connection
                // Timeout of 890 seconds because the Lambda function can be run for at most 15 mins (~890 secs)
                if (null != result) {
                    result.get(890, TimeUnit.SECONDS);
                }

            } catch (TimeoutException e) {
                logger.debug("Timing out KVS to Transcribe Streaming after 890 sec");

            } catch (Exception e) {
                logger.error("Error during streaming: ", e);
                throw e;
            }

            return "{ \"result\": \"Success\" }";

        } catch (Exception e) {
            logger.error("KVS to Transcribe Streaming failed with: ", e);
            return "{ \"result\": \"Failed\" }";
        }
    }


    private CompletableFuture<Void> getStartStreamingTranscriptionFuture(TranscribeStreamingRetryClient client, String streamName, TranscriptionRequest request) {
        return client.startStreamTranscription(
                // since we're definitely working with telephony audio, we know that's 8 kHz
                getRequest(16000, request.transcribeLanguageCode),
                new KDSAudioStreamPublisher(streamName),
                new StreamTranscriptionBehaviorImpl(request)
        );
    }

    /**
     * @return AWS credentials to be used to connect to Transcribe service. This example uses the default credentials
     * provider, which looks for environment variables (AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY) or a credentials
     * file on the system running this program.
     */
    private static AwsCredentialsProvider getTranscribeCredentials() {
        return DefaultCredentialsProvider.create();
    }

    /**
     * Build StartStreamTranscriptionRequestObject containing required parameters to open a streaming transcription
     * request, such as audio sample rate and language spoken in audio
     *
     * @param mediaSampleRateHertz sample rate of the audio to be streamed to the service in Hertz
     * @param languageCode the language code to be used for Transcription (optional; see https://docs.aws.amazon.com/transcribe/latest/dg/API_streaming_StartStreamTranscription.html#API_streaming_StartStreamTranscription_RequestParameters )
     * @return StartStreamTranscriptionRequest to be used to open a stream to transcription service
     */
    private static StartStreamTranscriptionRequest getRequest(Integer mediaSampleRateHertz, Optional <String> languageCode) {

        return StartStreamTranscriptionRequest.builder()
                .languageCode(languageCode.isPresent() ? languageCode.get() : LanguageCode.EN_US.toString())
                .numberOfChannels(1)
                .mediaEncoding(MediaEncoding.PCM)
                .mediaSampleRateHertz(mediaSampleRateHertz)
                .build();
    }

    /**
     * KVSAudioStreamPublisher implements audio stream publisher.
     * It emits audio events from a KVS stream asynchronously in a separate thread
     */
    private static class KDSAudioStreamPublisher implements Publisher<AudioStream> {
        private String streamName;

        private KDSAudioStreamPublisher(String streamName) {
            this.streamName = streamName;
        }

        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {
            s.onSubscribe(new KDSByteToAudioEventSubscription(s, streamName));
        }
    }

}
