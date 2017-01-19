package com.ericgonzalesevans.healthlog;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthLogSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(HealthLogSpeechlet.class);

    private AmazonDynamoDBClient amazonDynamoDBClient;

    private HealthLogManager healthLogManager;

    private SkillContext skillContext;

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        initializeComponents();

        // if user said a one shot command that triggered an intent event,
        // it will start a new session, and then we should avoid speaking too many words.
        skillContext.setNeedsMoreHelp(false);
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        skillContext.setNeedsMoreHelp(true);
        return healthLogManager.getLaunchResponse(request, session);
    }

    @Override
    public SpeechletResponse onIntent(IntentRequest request, Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        initializeComponents();

        Intent intent = request.getIntent();
        if ("AddUserIntent".equals(intent.getName())) {
            return healthLogManager.getAddUserIntentResponse(intent, session, skillContext);

        } else if ("SetWeightIntent".equals(intent.getName())) {
            return healthLogManager.getSetWeightIntentResponse(intent, session, skillContext);

        } else if ("SetHeightIntent".equals(intent.getName())) {
        return healthLogManager.getSetHeightIntentResponse(intent, session, skillContext);

        }else if ("TellWeightIntent".equals(intent.getName())) {
            return healthLogManager.getTellWeightIntentResponse(intent, session);

        }else if ("TellHeightIntent".equals(intent.getName())) {
            return healthLogManager.getTellHeightIntentResponse(intent, session);

        } else if ("ResetUsersIntent".equals(intent.getName())) {
            return healthLogManager.getResetUsersIntent(intent, session);

        } else if ("AMAZON.HelpIntent".equals(intent.getName())) {
            return healthLogManager.getHelpIntentResponse(intent, session, skillContext);

        } else if ("AMAZON.CancelIntent".equals(intent.getName())) {
            return healthLogManager.getExitIntentResponse(intent, session, skillContext);

        } else if ("AMAZON.StopIntent".equals(intent.getName())) {
            return healthLogManager.getExitIntentResponse(intent, session, skillContext);

        } else {
            throw new IllegalArgumentException("Unrecognized intent: " + intent.getName());
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any cleanup logic goes here
    }

    /**
     * Initializes the instance components if needed.
     */
    private void initializeComponents() {
        if (amazonDynamoDBClient == null) {
            amazonDynamoDBClient = new AmazonDynamoDBClient();
            healthLogManager = new HealthLogManager(amazonDynamoDBClient);
            skillContext = new SkillContext();
        }
    }
}
