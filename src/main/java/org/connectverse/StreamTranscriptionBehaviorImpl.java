package org.connectverse;

import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of StreamTranscriptionBehavior to define how a stream response is handled.
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
public class StreamTranscriptionBehaviorImpl implements StreamTranscriptionBehavior {

    private static final Logger logger = LoggerFactory.getLogger(StreamTranscriptionBehaviorImpl.class);
    private final TranscriptionRequest request;
    private final TranslateText translateText;
    private final PollySpeechSynthesizer synthesizer;
    private final WebSocketStreamer streamer;

    public StreamTranscriptionBehaviorImpl(TranscriptionRequest request) {
        this.request = request;

        translateText = new TranslateText(request.getTranslateFromLanguageCode(), request.getTranslateToLanguageCode());
        synthesizer = new PollySpeechSynthesizer(request.getPollyLanguageCode(), request.getPollyVoiceId());
        streamer = new WebSocketStreamer("https://qv1241nc27.execute-api.us-east-1.amazonaws.com/dev/", "WebSocketConnections");
    }

    @Override
    public void onError(Throwable e) {
        logger.error("Error in middle of stream: ", e);
    }

    @Override
    public void onStream(TranscriptResultStream e) {
        // EventResultStream has other fields related to the timestamp of the transcripts in it.
        // Please refer to the javadoc of TranscriptResultStream for more details
        TranscriptEvent event = (TranscriptEvent) e;

        String transcript = getTranscript(event);

        if (!transcript.isEmpty()) {
            String translatedText = translateText.translate(transcript);
            logger.info("Translated text: '" + translatedText + "'");

            logger.info("Finished synthesizing speech for: " + translatedText);
            SynthesizeSpeechResult speechResult = synthesizer.synthesizeSpeech(translatedText);

            streamer.streamAudioToConnections(speechResult);
            logger.info("Finished streaming to socket for: " + translatedText);
        }
    }

    String getTranscript(TranscriptEvent transcriptEvent) {
        List<Result> results = transcriptEvent.transcript().results();
        String transcript = "";

        if (results.size() > 0) {

            Result result = results.get(0);

            if (!result.isPartial()) {
                try {
                    if (result.alternatives().size() > 0) {
                        if (!result.alternatives().get(0).transcript().isEmpty()) {
                            transcript = result.alternatives().get(0).transcript();
                        }
                    }

                    logger.info("Transcript: " + result.alternatives().get(0).transcript());
                    logger.info("Processed transcript at: " + Instant.now().getEpochSecond());

                } catch (Exception e) {
                    logger.error("Could not process transcript: ", e);
                }
            }
        }

        return transcript;
    }

    @Override
    public void onResponse(StartStreamTranscriptionResponse r) {
        logger.info(String.format("%d Received Initial response from Transcribe. Request Id: %s",
                System.currentTimeMillis(), r.requestId()));
    }

    @Override
    public void onComplete() {
        logger.info("Transcribe stream completed");
    }
}

