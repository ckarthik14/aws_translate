package org.connectverse;

/*
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

import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;

import java.util.Optional;

public class TranscriptionRequest {

    String streamARN = null;
    String inputFileName = null;
    String connectContactId = null;
    Optional<String> transcribeLanguageCode = Optional.empty();

    Optional<String> translateFromLanguageCode = Optional.empty();

    Optional<String> translateToLanguageCode = Optional.empty();
    Optional<String> pollyLanguageCode = Optional.empty();
    Optional<String> pollyVoiceId = Optional.empty();
    boolean transcriptionEnabled = false;
    Optional<Boolean> saveCallRecording = Optional.empty();
    boolean streamAudioFromCustomer = true;
    boolean streamAudioToCustomer = false;

    public String getStreamARN() {

        return this.streamARN;
    }

    public void setStreamARN(String streamARN) {

        this.streamARN = streamARN;
    }

    public String getInputFileName() {

        return this.inputFileName;
    }

    public void setInputFileName(String inputFileName) {

        this.inputFileName = inputFileName;
    }

    public String getConnectContactId() {

        return this.connectContactId;
    }

    public void setConnectContactId(String connectContactId) {

        this.connectContactId = connectContactId;
    }

    public Optional<String> getTranscribeLanguageCode() {
        return this.transcribeLanguageCode;
    }

    public void setTranscribeLanguageCode(String transcribeLanguageCode) {
        if ((transcribeLanguageCode != null) && (transcribeLanguageCode.length() > 0)) {

            this.transcribeLanguageCode = Optional.of(transcribeLanguageCode);
        }
    }

    public String getTranslateFromLanguageCode() {
        return translateFromLanguageCode.orElse("en");
    }

    public void setTranslateFromLanguageCode(String translateFromLanguageCode) {
        if ((translateFromLanguageCode != null) && (translateFromLanguageCode.length() > 0)) {

            this.translateFromLanguageCode = Optional.of(translateFromLanguageCode);
        }
    }

    public String getTranslateToLanguageCode() {
        return translateToLanguageCode.orElse("en");
    }

    public void setTranslateToLanguageCode(String translateToLanguageCode) {
        if ((translateToLanguageCode != null) && (translateToLanguageCode.length() > 0)) {
            this.translateToLanguageCode = Optional.of(translateToLanguageCode);
        }
    }

    public String getPollyLanguageCode() {
        return pollyLanguageCode.orElse("en-US");
    }

    public void setPollyLanguageCode(String pollyLanguageCode) {
        if ((pollyLanguageCode != null) && (pollyLanguageCode.length() > 0)) {
            this.pollyLanguageCode = Optional.of(pollyLanguageCode);
        }
    }

    public String getPollyVoiceId() {
        return pollyVoiceId.orElse("Danielle");
    }

    public void setPollyVoiceId(String pollyVoiceId) {
        if ((pollyVoiceId != null) && (pollyVoiceId.length() > 0)) {
            this.pollyVoiceId = Optional.of(pollyVoiceId);
        }
    }

    public void setTranscriptionEnabled(boolean enabled) {
        transcriptionEnabled = enabled;
    }

    public boolean isTranscriptionEnabled() {
        return  transcriptionEnabled;
    }

    public void setStreamAudioFromCustomer(boolean enabled) {
        streamAudioFromCustomer = enabled;
    }

    public boolean isStreamAudioFromCustomer() {
        return  streamAudioFromCustomer;
    }

    public void setStreamAudioToCustomer(boolean enabled) {
        streamAudioToCustomer = enabled;
    }

    public boolean isStreamAudioToCustomer() {
        return  streamAudioToCustomer;
    }

    public void setSaveCallRecording(boolean shouldSaveCallRecording) {

        saveCallRecording = Optional.of(shouldSaveCallRecording);
    }

    public Optional<Boolean> getSaveCallRecording() {
        return saveCallRecording;
    }

    public boolean isSaveCallRecordingEnabled() {

        return (saveCallRecording.isPresent() ? saveCallRecording.get() : false);
    }

    @Override
    public String toString() {
        return "TranscriptionRequest{" +
                "streamARN='" + streamARN + '\'' +
                ", inputFileName='" + inputFileName + '\'' +
                ", connectContactId='" + connectContactId + '\'' +
                ", transcribeLanguageCode=" + transcribeLanguageCode +
                ", translateFromLanguageCode=" + translateFromLanguageCode +
                ", translateToLanguageCode=" + translateToLanguageCode +
                ", pollyLanguageCode=" + pollyLanguageCode +
                ", pollyVoiceId=" + pollyVoiceId +
                ", transcriptionEnabled=" + transcriptionEnabled +
                ", saveCallRecording=" + saveCallRecording +
                ", streamAudioFromCustomer=" + streamAudioFromCustomer +
                ", streamAudioToCustomer=" + streamAudioToCustomer +
                '}';
    }

    public void validate() throws IllegalArgumentException {

        // complain if both are provided
        if ((getStreamARN() != null) && (getInputFileName() != null))
            throw new IllegalArgumentException("At most one of streamARN or inputFileName must be provided");
        // complain if none are provided
        if ((getStreamARN() == null) && (getInputFileName() == null))
            throw new IllegalArgumentException("One of streamARN or inputFileName must be provided");

        // language code is optional; if provided, it should be one of the values accepted by
        // https://docs.aws.amazon.com/transcribe/latest/dg/API_streaming_StartStreamTranscription.html#API_streaming_StartStreamTranscription_RequestParameters
        if (transcribeLanguageCode.isPresent()) {
            if (!LanguageCode.knownValues().contains(LanguageCode.fromValue(transcribeLanguageCode.get()))) {
                throw new IllegalArgumentException("Incorrect language code");
            }
        }
    }

}
