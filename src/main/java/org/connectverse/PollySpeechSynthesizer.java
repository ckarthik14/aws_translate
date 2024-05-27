package org.connectverse;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;

public class PollySpeechSynthesizer {
    private final String languageCode;
    private final String voiceId;
    private final AmazonPolly pollyClient;

    public PollySpeechSynthesizer(String languageCode, String voiceId) {
        this.languageCode = languageCode;
        this.voiceId = voiceId;
        this.pollyClient = AmazonPollyClientBuilder.standard()
                .withRegion("us-east-1")
                .build();
    }

    public SynthesizeSpeechResult synthesizeSpeech(String text) {
        SynthesizeSpeechRequest request = new SynthesizeSpeechRequest()
                .withText(text)
                .withVoiceId(voiceId)
                .withLanguageCode(languageCode)
                .withOutputFormat(OutputFormat.Mp3);
        return pollyClient.synthesizeSpeech(request);
    }
}

