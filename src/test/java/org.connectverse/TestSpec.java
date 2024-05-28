package org.connectverse;

import org.junit.jupiter.api.Test;

public class TestSpec {
    @Test
    public void test() {
        TranscriptionRequest request = new TranscriptionRequest();

        request.setSaveCallRecording(false);
        request.setStreamARN("arn:aws:kinesis:us-east-1:471112798145:stream/ICS_Showcase_from_customer_audio/");
        request.setTranscribeLanguageCode("en-US");
        request.setTranslateFromLanguageCode("en");
        request.setTranslateToLanguageCode("es");
        request.setPollyLanguageCode("es-ES");
        request.setPollyVoiceId("Lucia");

        new KDSTranslateLambda().handleRequest(request, null);
    }
}
