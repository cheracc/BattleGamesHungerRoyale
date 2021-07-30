package me.cheracc.battlegameshungerroyale.managers;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Trans;

import java.util.logging.Logger;

public class Logr {
    private final Logger logger;
    private final boolean showDebugMessages;

    public Logr(BGHR plugin) {
        this.logger = plugin.getLogger();
        showDebugMessages = plugin.getConfig().getBoolean("show debug messages in console", false);
    }

    public void info(String message) {
        logger.info(Trans.late(message));
    }

    public void info(String message, Object... args) {
        logger.info(String.format(Trans.late(message), args));
    }

    public void warn(String message) {
        logger.warning(Trans.late(message));
    }

    public void warn(String message, Object... args) {
        logger.warning(String.format(Trans.late(message), args));
    }

    public void debug(String message) {
        if (showDebugMessages)
            logger.info("Debug> " + Trans.late(message));
    }

    public void debug(String message, Object... args) {
        if (showDebugMessages)
            logger.info("Debug> " + String.format(Trans.late(message), args));
    }
}
