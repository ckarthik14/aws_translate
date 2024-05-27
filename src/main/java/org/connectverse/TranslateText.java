package org.connectverse;

import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClientBuilder;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;

public class TranslateText {
    String source, target;

    public TranslateText(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String translate(String transcript) {
        // Create an AmazonTranslate client
        AmazonTranslate translate = AmazonTranslateClientBuilder.standard()
                .withRegion("us-east-1") // specify the region you configured
                .build();

        // Create request
        TranslateTextRequest request = new TranslateTextRequest()
                .withText(transcript)
                .withSourceLanguageCode(source)
                .withTargetLanguageCode(target);

        // Translate the text
        TranslateTextResult result = translate.translateText(request);

        return result.getTranslatedText();
    }
}
