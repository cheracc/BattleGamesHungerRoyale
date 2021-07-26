package me.cheracc.battlegameshungerroyale.tools;

import me.cheracc.battlegameshungerroyale.BGHR;

import java.util.logging.Logger;

public class Logr {
    private static Logr singleton = null;
    private final Logger logger;

    private Logr(BGHR plugin) {
        this.logger = plugin.getLogger();
    }

    public static void initialize(BGHR plugin) {
        singleton = new Logr(plugin);
    }

    public static Logr log() {
        if (singleton == null)
            throw new InstantiationError(Trans.late("this must be initialized first using Logr.initialize(plugin)"));
        return singleton;
    }

    public Logger getLogger() {
        return logger;
    }

    public static void info(String message) {
        log().getLogger().info(Trans.late(message));
    }

    public static void info(String message, Object... args) {
        log().getLogger().info(String.format(Trans.late(message), args));
    }

    public static void warn(String message) {
        log().getLogger().warning(Trans.late(message));
    }

    public static void warn(String message, Object args) {
        log().getLogger().warning(String.format(Trans.late(message), args));
    }
}
