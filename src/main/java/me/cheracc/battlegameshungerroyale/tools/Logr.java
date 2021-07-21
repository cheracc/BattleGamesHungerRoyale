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
            throw new InstantiationError("this must be initialized first using Logr.initialize(plugin)");
        return singleton;
    }

    public Logger getLogger() {
        return logger;
    }

    public static void info(String message) {
        log().getLogger().info(message);
    }

    public static void warn(String message) {
        log().getLogger().warning(message);
    }
}
