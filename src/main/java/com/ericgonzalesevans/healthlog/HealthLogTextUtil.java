package com.ericgonzalesevans.healthlog;

import java.util.Arrays;
import java.util.List;

public final class HealthLogTextUtil {

    private HealthLogTextUtil() {
    }

    /**
     * List of player names blacklisted for this app.
     */
    private static final List<String> NAME_BLACKLIST = Arrays.asList("player", "players");

    /**
     * Text of complete help.
     */
    public static final String COMPLETE_HELP =
            "Here's some things you can say. Add user, reset user data, and exit.";

    /**
     * Text of next help.
     */
    public static final String NEXT_HELP = "You can track a user's weight, add a player, or say help." +
            " What would you like?";

    /**
     * Cleans up the player name, and sanitizes it against the blacklist.
     *
     * @param recognizedName
     * @return
     */
    public static String getUserName(String recognizedName) {
        if (recognizedName == null || recognizedName.isEmpty()) {
            return null;
        }

        String cleanedName;
        if (recognizedName.contains(" ")) {
            // the name should only contain a first name, so ignore the second part if any
            cleanedName = recognizedName.substring(recognizedName.indexOf(" "));
        } else {
            cleanedName = recognizedName;
        }

        // if the name is on our blacklist, it must be mis-recognition
        if (NAME_BLACKLIST.contains(cleanedName)) {
            return null;
        }

        return cleanedName;
    }
}
