package com.ericgonzalesevans.healthlog;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

import java.util.HashSet;
import java.util.Set;

public final class HealthLogSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {
    private static final Set<String> supportedApplicationIds;

    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds = new HashSet<String>();
        supportedApplicationIds.add("amzn1.ask.skill.[unique-id-here]");
    }

    public HealthLogSpeechletRequestStreamHandler() {
        super(new HealthLogSpeechlet(), supportedApplicationIds);
    }
}
