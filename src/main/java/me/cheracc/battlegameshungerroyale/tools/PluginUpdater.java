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
    private final boolean useSnapshotBuilds;
    private boolean isSnapshotBuild = false;
    private boolean notified = false;

    public PluginUpdater(BGHR plugin) {
        this.plugin = plugin;
        useSnapshotBuilds = plugin.getConfig().getBoolean("use snapshot builds", false);
        new BukkitRunnable() {
            @Override
            public void run() {
                getCurrentBuildNumber();
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

    private void getCurrentBuildNumber() {
        if (currentBuildNumber <= 0) {
            YamlConfiguration pluginYml = new YamlConfiguration();
            InputStream input = plugin.getResource("plugin.yml");

            try {
                pluginYml.load(new InputStreamReader(input));
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }


            apiVersion = Double.toString(pluginYml.getDouble("api-version"));
            currentBuildNumber = pluginYml.getInt("build-number", 0);
            isSnapshotBuild = pluginYml.getString("version", "").contains("SNAPSHOT");
            if (isSnapshotBuild)
                Logr.info("Using snapshot builds - expect errors or bugs!");
        }
    }

    private void getLatestVersionInfo() {
        if (!plugin.getConfig().getBoolean("auto-update", true))
            return;
        try {
            String projectName = useSnapshotBuilds ? "BGHR-SNAPSHOT" : "BattleGamesHungerRoyale";
            URL url = new URL("https://jenkins.cheracc.me/job/" + projectName + "/api/json?tree=lastStableBuild[number,url]");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

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
        if (isSnapshotBuild != useSnapshotBuilds)
            return false;
        if (mostRecentBuildAvailable > 0 && urlToMostRecentBuild != null && urlToMostRecentBuild.length() > 0) {
            if (!notified)
                Logr.info(String.format("Updater found no new version. (snapshots:%s, current:%s, newest:%s)", useSnapshotBuilds, currentBuildNumber, mostRecentBuildAvailable));
            notified = true;
            return mostRecentBuildAvailable <= currentBuildNumber;
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
                toSave.getParentFile().mkdirs();

                Logr.info(String.format("Updater found a new update (%s build #%s): preparing to download", useSnapshotBuilds ? "SNAPSHOT" : "" ,mostRecentBuildAvailable));

                try {
                    toSave.createNewFile();
                    URL url = new URL(urlString);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    OutputStream out = new FileOutputStream(toSave);
                    con.setRequestProperty("User-Agent", "Mozilla/5.0");

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
                        Logr.info("Updater finished downloading plugin update. it will be installed on restart");
                        cancel();
                    }
                    else {
                        Logr.info("Updater failed to download update!");
                        downloadStatus = null;
                        File failure = new File(Bukkit.getUpdateFolderFile(), plugin.getJarFilename());
                        if (failure.exists())
                            failure.delete();
                    }
                    return;
                }
                if (System.currentTimeMillis() - last > 1000*60*15 && !isLatestVersion()) {
                    downloadStatus = downloadLatest();
                }
                last = System.currentTimeMillis();
            }
        };
        return task.runTaskTimer(plugin, 20*60, 20*30);
    }
}
