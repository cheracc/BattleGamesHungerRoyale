package me.cheracc.battlegameshungerroyale.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.cheracc.battlegameshungerroyale.BGHR;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.h2.store.fs.FileUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PluginUpdater {
    private final BGHR plugin;
    private int currentBuildNumber = 0;
    private int mostRecentBuildAvailable = 0;
    private String urlToMostRecentBuild = null;
    private String apiVersion = null;
    private final BukkitTask updateChecker;
    private CompletableFuture<Boolean> downloadStatus = null;
    private final boolean useSnapshotBuilds = true;

    public PluginUpdater(BGHR plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                getLatestVersionInfo();
            }
        }.runTaskAsynchronously(plugin);
        updateChecker = runUpdateChecker();
    }

    public void disable() {
        if (updateChecker != null && !updateChecker.isCancelled())
            updateChecker.cancel();
        if (downloadStatus != null && !downloadStatus.isDone()) {
            // there is a download in progress, going to hold the thread...
            try {
                downloadStatus.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                //failed, delete the  file so it doesn't bork the next restart
                FileUtils.delete(Bukkit.getUpdateFolder() + "/" + plugin.getJarFilename());
            }
        }
    }

    private int currentBuildNumber() {
        if (currentBuildNumber <= 0) {
            YamlConfiguration pluginYml = new YamlConfiguration();
            InputStream input = plugin.getResource("plugin.yml");

            try {
                pluginYml.load(new InputStreamReader(input));
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }


            apiVersion = pluginYml.getString("api-version");
            currentBuildNumber = pluginYml.getInt("build-number", 0);
        }
        return currentBuildNumber;
    }

    private void getLatestVersionInfo() {
        if (!plugin.getConfig().getBoolean("auto-update", true))
            return;
        try {
            String projectName = useSnapshotBuilds ? "BGHR-SNAPSHOT" : "BattleGamesHungerRoyale";
            URL url = new URL("https://jenkins.cheracc.me/job/" + projectName + "/api/json?tree=lastStableBuild[number,url]");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDefaultUseCaches(false);

            InputStream input = con.getInputStream();
            String json = new BufferedReader(new InputStreamReader(input)).lines().collect(Collectors.joining("\n"));

            JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();

            if (jsonObject == null) {
                Bukkit.getLogger().warning("could not read json string");
                return;
            }
            JsonObject buildInfo = jsonObject.getAsJsonObject("lastStableBuild");
            if (buildInfo == null) {
                Bukkit.getLogger().warning("json object did not contain lastStableBuild");
                return;
            }
            if (buildInfo.has("number"))
                mostRecentBuildAvailable = buildInfo.get("number").getAsInt();

            if (buildInfo.has("url"))
                urlToMostRecentBuild = buildInfo.get("url").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isLatestVersion() {
        if (mostRecentBuildAvailable > 0 && urlToMostRecentBuild != null && urlToMostRecentBuild.length() > 0) {
            return mostRecentBuildAvailable <= currentBuildNumber();
        }
        return true;
    }

    private CompletableFuture<Boolean> downloadLatest() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (urlToMostRecentBuild == null || urlToMostRecentBuild.length() == 0) {
            future.complete(false);
            return future;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                String projectName = useSnapshotBuilds ? "BGHR-SNAPSHOT" : "BattleGamesHungerRoyale";
                String urlString = String.format("https://jenkins.cheracc.me/job/%s/%s/artifact/target/BattleGamesHungerRoyale-%s.jar",
                        projectName, mostRecentBuildAvailable, apiVersion);
                File toSave = new File(Bukkit.getUpdateFolderFile(), plugin.getJarFilename());

                if (toSave.exists())
                    toSave.delete();

                Logr.info(String.format("(Updater) Found a new update (build #%s): preparing to download", mostRecentBuildAvailable));

                try {
                    URL url = new URL(urlString);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    OutputStream out = new FileOutputStream(toSave);
                    con.setDefaultUseCaches(false);

                    InputStream input = con.getInputStream();
                    Tools.copyStreams(input, out);
                    input.close();
                    out.close();
                    future.complete(true);
                } catch (IOException e) {
                    toSave.delete();
                    future.complete(false);
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    private BukkitTask runUpdateChecker() {
        BukkitRunnable task = new BukkitRunnable() {
            long last = 0;
            @Override
            public void run() {
                if (downloadStatus != null && downloadStatus.isDone()) {
                    if (downloadStatus.getNow(false)) {
                        Logr.info("(Updater) Finished downloading plugin update. it will be installed on restart");
                        cancel();
                    }
                    else {
                        Logr.info("(Updater) Failed to download update!");
                        downloadStatus = null;
                    }
                    return;
                }
                if (System.currentTimeMillis() - last > 1000*60*15 && !isLatestVersion()) {
                    downloadStatus = downloadLatest();
                    last = System.currentTimeMillis();
                }
            }
        };
        return task.runTaskTimer(plugin, 20*60, 20*30);
    }
}
