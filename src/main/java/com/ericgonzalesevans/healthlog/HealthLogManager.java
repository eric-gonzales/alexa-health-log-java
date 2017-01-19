package com.ericgonzalesevans.healthlog;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.ericgonzalesevans.healthlog.storage.HealthLogDao;
import com.ericgonzalesevans.healthlog.storage.HealthLogDynamoDbClient;
import com.ericgonzalesevans.healthlog.storage.HealthLogMetricData;
import com.ericgonzalesevans.healthlog.storage.HealthLogMetrics;

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

/**
 * The {@link HealthLogManager} receives various events and intents and manages the flow.
 */
public class HealthLogManager {
    /**
     * Intent slot for user name.
     */
    private static final String SLOT_USER_NAME = "UserName";

    /**
     * Intent slot for user weight (in pounds).
     */
    private static final String SLOT_WEIGHT_NUMBER = "WeightNumber";

    /**
     * Intent slot for user height (in inches).
     */
    private static final String SLOT_HEIGHT_NUMBER = "HeightNumber";

    /**
     * Maximum number of users for which weights must be announced while adding a weight.
     */
    private static final int MAX_USERS_FOR_SPEECH = 3;

    private final HealthLogDao healthLogDao;

    public HealthLogManager(final AmazonDynamoDBClient amazonDynamoDbClient) {
        HealthLogDynamoDbClient dynamoDbClient =
                new HealthLogDynamoDbClient(amazonDynamoDbClient);
        healthLogDao = new HealthLogDao(dynamoDbClient);
    }

    /**
     * Creates and returns response for Launch request.
     *
     * @param request
     *            {@link LaunchRequest} for this request
     * @param session
     *            Speechlet {@link Session} for this request
     * @return response for launch request
     */
    public SpeechletResponse getLaunchResponse(LaunchRequest request, Session session) {
        // Speak welcome message and ask user questions
        // based on whether there are users or not.
        String speechText, repromptText;
        HealthLogMetrics logMetrics = healthLogDao.getHealthLogMetrics(session);

        if (logMetrics == null || !logMetrics.hasUsers()) {
            speechText = "HealthLog, Let's start your metrics. Who's your first user?";
            repromptText = "Please tell me who is your first user?";
        } else if (!logMetrics.hasWeights()) {
            speechText =
                    "HealthLog, you have " + logMetrics.getNumberOfUsers()
                            + (logMetrics.getNumberOfUsers() == 1 ? " user" : " users")
                            + " in the log. You can give a user metrics, add another user,"
                            + " reset all user data or exit. Which would you like?";
            repromptText = HealthLogTextUtil.COMPLETE_HELP;
        } else {
            speechText = "HealthLog, What can I do for you?";
            repromptText = HealthLogTextUtil.NEXT_HELP;
        }

        return getAskSpeechletResponse(speechText, repromptText);
    }

    /**
     * Creates and returns response for the add user intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            Speechlet {@link Session} for this request
     * @param skillContext
     * @return response for the add user intent.
     */
    public SpeechletResponse getAddUserIntentResponse(Intent intent, Session session,
            SkillContext skillContext) {
        // add a user to the current log,
        // terminate or continue the conversation based on whether the intent
        // is from a one shot command or not.
        String newUserName =
                HealthLogTextUtil.getUserName(intent.getSlot(SLOT_USER_NAME).getValue());
        if (newUserName == null) {
            String speechText = "OK. Who do you want to add?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        // Load the previous log
        HealthLogMetrics metrics = healthLogDao.getHealthLogMetrics(session);
        if (metrics == null) {
            metrics = HealthLogMetrics.newInstance(session, HealthLogMetricData.newInstance());
        }

        metrics.addUser(newUserName);

        // Save the updated metrics
        healthLogDao.saveHealthLogMetrics(metrics);

        String speechText = newUserName + " has been added your log. You can now keep track of their health metrics!";
        String repromptText = null;

        if (skillContext.needsMoreHelp()) {
            if (metrics.getNumberOfUsers() == 1) {
                speechText += "You can say, I am done adding users. Now who's your next user?";

            } else {
                speechText += "Who is your next user?";
            }
            repromptText = HealthLogTextUtil.NEXT_HELP;
        }

        if (repromptText != null) {
            return getAskSpeechletResponse(speechText, repromptText);
        } else {
            return getTellSpeechletResponse(speechText);
        }
    }

    /**
     * Creates and returns response for the set weight intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the set weight intent
     */
    public SpeechletResponse getSetWeightIntentResponse(Intent intent, Session session,
                                                        SkillContext skillContext) {
        String userName =
                HealthLogTextUtil.getUserName(intent.getSlot(SLOT_USER_NAME).getValue());
        if (userName == null) {
            String speechText = "Sorry, I did not hear the user name. Please say again?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        int weight = 0;
        try {
            weight = Integer.parseInt(intent.getSlot(SLOT_WEIGHT_NUMBER).getValue());
        } catch (NumberFormatException e) {
            String speechText = "Sorry, I did not hear the weight. Please say again?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        HealthLogMetrics metrics = healthLogDao.getHealthLogMetrics(session);
        if (metrics == null) {
            return getTellSpeechletResponse("A health log has not been started.");
        }

        if (metrics.getNumberOfUsers() == 0) {
            String speechText = "Sorry, no users are on the health log. Try adding a user?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        // Update weight
        if (!metrics.addWeightForUser(userName, weight)) {
            String speechText = "Sorry, " + userName + " is not on this log. What else?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        // Save metrics
        healthLogDao.saveHealthLogMetrics(metrics);

        // Prepare speech text
        String speechText = weight + " pounds for " + userName + ". ";
        if (metrics.getNumberOfUsers() > MAX_USERS_FOR_SPEECH) {
            speechText += userName + " is " + metrics.getWeightForUser(userName) + " pounds in weight.";
        } else {
            speechText += getAllWeightsAsSpeechText(metrics.getAllWeightsInDescendingOrder());
        }

        return getTellSpeechletResponse(speechText);
    }

    /**
     * Creates and returns response for the set height intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the set height intent
     */
    public SpeechletResponse getSetHeightIntentResponse(Intent intent, Session session,
                                                        SkillContext skillContext) {
        String userName =
                HealthLogTextUtil.getUserName(intent.getSlot(SLOT_USER_NAME).getValue());
        if (userName == null) {
            String speechText = "Sorry, I did not hear the user name. Please say again?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        int height = 0;
        try {
            height = Integer.parseInt(intent.getSlot(SLOT_HEIGHT_NUMBER).getValue());
        } catch (NumberFormatException e) {
            String speechText = "Sorry, I did not hear the height. Please say again?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        HealthLogMetrics metrics = healthLogDao.getHealthLogMetrics(session);
        if (metrics == null) {
            return getTellSpeechletResponse("A health log has not been started.");
        }

        if (metrics.getNumberOfUsers() == 0) {
            String speechText = "Sorry, no users are on the health log. Try adding a user?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        // Update height
        if (!metrics.addHeightForUser(userName, height)) {
            String speechText = "Sorry, " + userName + " is not on this log. What else?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        // Save metrics
        healthLogDao.saveHealthLogMetrics(metrics);

        // Prepare speech text
        String speechText = height + " inches for " + userName + ". ";
        if (metrics.getNumberOfUsers() > MAX_USERS_FOR_SPEECH) {
            speechText += userName + " is " + metrics.getHeightForUser(userName) + " inches tall.";
        } else {
            speechText += getAllHeightsAsSpeechText(metrics.getAllHeightsInDescendingOrder());
        }

        return getTellSpeechletResponse(speechText);
    }

    /**
     * Creates and returns response for the tell weights intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @return response for the tell weights intent
     */
    public SpeechletResponse getTellWeightIntentResponse(Intent intent, Session session) {
        // tells all stored weights and send the result in card.
        HealthLogMetrics metrics = healthLogDao.getHealthLogMetrics(session);

        if (metrics == null || !metrics.hasUsers()) {
            return getTellSpeechletResponse("Nobody is on the health log. Try adding a user first.");
        }

        SortedMap<String, Long> weightsInDescendingOrder = metrics.getAllWeightsInDescendingOrder();
        String speechText = getAllWeightsAsSpeechText(weightsInDescendingOrder);
        Card leaderboardMetricsCard = getMetricsCard(weightsInDescendingOrder);

        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return SpeechletResponse.newTellResponse(speech, leaderboardMetricsCard);
    }

    /**
     * Creates and returns response for the tell heights intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @return response for the tell heights intent
     */
    public SpeechletResponse getTellHeightIntentResponse(Intent intent, Session session) {
        // tells all stored heights and send the result in card.
        HealthLogMetrics metrics = healthLogDao.getHealthLogMetrics(session);

        if (metrics == null || !metrics.hasUsers()) {
            return getTellSpeechletResponse("Nobody is on the health log. Try adding a user first.");
        }

        SortedMap<String, Long> heights = metrics.getAllHeightsInDescendingOrder();
        String speechText = getAllHeightsAsSpeechText(heights);
        Card leaderboardMetricsCard = getMetricsCard(heights);

        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return SpeechletResponse.newTellResponse(speech, leaderboardMetricsCard);
    }

    /**
     * Creates and returns response for the reset users intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @return response for the reset users intent
     */
    public SpeechletResponse getResetUsersIntent(Intent intent, Session session) {
        // Remove all users
        HealthLogMetrics metrics =
                HealthLogMetrics.newInstance(session, HealthLogMetricData.newInstance());
        healthLogDao.saveHealthLogMetrics(metrics);

        String speechText = "New health log started without users. Who do you want to add first?";
        return getAskSpeechletResponse(speechText, speechText);
    }

    /**
     * Creates and returns response for the help intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the help intent
     */
    public SpeechletResponse getHelpIntentResponse(Intent intent, Session session,
            SkillContext skillContext) {
        return skillContext.needsMoreHelp() ? getAskSpeechletResponse(
                HealthLogTextUtil.COMPLETE_HELP + " So, how can I help?",
                HealthLogTextUtil.NEXT_HELP)
                : getTellSpeechletResponse(HealthLogTextUtil.COMPLETE_HELP);
    }

    /**
     * Creates and returns response for the exit intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the exit intent
     */
    public SpeechletResponse getExitIntentResponse(Intent intent, Session session,
            SkillContext skillContext) {
        return skillContext.needsMoreHelp() ? getTellSpeechletResponse("Okay. Whenever you're "
                + "ready, you can start tracking your weight using health log.")
                : getTellSpeechletResponse("");
    }

    /**
     * Returns an ask Speechlet response for a speech and reprompt text.
     *
     * @param speechText
     *            Text for speech output
     * @param repromptText
     *            Text for reprompt output
     * @return ask Speechlet response for a speech and reprompt text
     */
    private SpeechletResponse getAskSpeechletResponse(String speechText, String repromptText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Session");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
        repromptSpeech.setText(repromptText);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptSpeech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    /**
     * Returns a tell Speechlet response for a speech and reprompt text.
     *
     * @param speechText
     *            Text for speech output
     * @return a tell Speechlet response for a speech and reprompt text
     */
    private SpeechletResponse getTellSpeechletResponse(String speechText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Session");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Converts a {@link Map} of weights into text for speech. The order of the entries in the text
     * is determined by the order of entries in {@link Map#entrySet()}.
     *
     * @param weights
     *            A {@link Map} of weights
     * @return a speech ready text containing weights
     */
    private String getAllWeightsAsSpeechText(Map<String, Long> weights) {
        StringBuilder speechText = new StringBuilder();
        int index = 0;
        for (Entry<String, Long> entry : weights.entrySet()) {
            if (weights.size() > 1 && index == weights.size() - 1) {
                speechText.append(" and ");
            }
            String singularOrPluralPoints = entry.getValue() == 1 ? " pound, " : " pounds, ";
            speechText
                    .append(entry.getKey())
                    .append(" weighs ")
                    .append(entry.getValue())
                    .append(singularOrPluralPoints);
            index++;
        }

        return speechText.toString();
    }

    /**
     * Converts a {@link Map} of heights into text for speech. The order of the entries in the text
     * is determined by the order of entries in {@link Map#entrySet()}.
     *
     * @param heights
     *            A {@link Map} of heights
     * @return a speech ready text containing heights
     */
    private String getAllHeightsAsSpeechText(Map<String, Long> heights) {
        StringBuilder speechText = new StringBuilder();
        int index = 0;
        for (Entry<String, Long> entry : heights.entrySet()) {
            if (heights.size() > 1 && index == heights.size() - 1) {
                speechText.append(" and ");
            }
            String singularOrPluralPoints = entry.getValue() == 1 ? " inch, " : " inches, ";
            speechText
                    .append(entry.getKey())
                    .append(" is ")
                    .append(entry.getValue())
                    .append(singularOrPluralPoints);
            index++;
        }

        return speechText.toString();
    }

    /**
     * Creates and returns a {@link Card} with a formatted text containing all metrics in the log.
     * The order of the entries in the text is determined by the order of entries in
     * {@link Map#entrySet()}.
     *
     * @param metrics
     *            A {@link Map} of metrics
     * @return leaderboard text containing all metrics in the log
     */
    private Card getMetricsCard(Map<String, Long> metrics) {
        StringBuilder cardText = new StringBuilder();
        int index = 0;
        for (Entry<String, Long> entry : metrics.entrySet()) {
            index++;
            cardText
                    .append("No. ")
                    .append(index)
                    .append(" - ")
                    .append(entry.getKey())
                    .append(" : ")
                    .append(entry.getValue())
                    .append("\n");
        }

        SimpleCard card = new SimpleCard();
        card.setTitle("Health Metrics");
        card.setContent(cardText.toString());
        return card;
    }
}
