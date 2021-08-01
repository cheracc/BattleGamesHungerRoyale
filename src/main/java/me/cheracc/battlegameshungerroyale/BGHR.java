package me.cheracc.battlegameshungerroyale;

import me.cheracc.battlegameshungerroyale.managers.Logr;
import me.cheracc.battlegameshungerroyale.tools.PluginUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class BGHR extends JavaPlugin implements Listener {
    private PluginUpdater updater;
    private BghrApi api;

    @Override
    public void onDisable() {
        api.shutdown();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        saveDefaultConfig();
        api = new BghrApi(this);
        Metrics metrics = new Metrics(this, 12102);
    }

    public String getJarFilename() {
        return super.getFile().getName();
    }

    public Logr getLogr() {
        return api.logr();
    }

    public BghrApi getApi() {
        return api;
    }
}
