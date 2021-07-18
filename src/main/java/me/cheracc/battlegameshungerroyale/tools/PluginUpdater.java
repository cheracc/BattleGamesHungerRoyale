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
    private BukkitTask updateChecker = null;
    private CompletableFuture<Boolean> downloadStatus = null;

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
        if (currentBuildNumber > 0)
            return currentBuildNumber;
        else {
            YamlConfiguration pluginYml = new YamlConfiguration();
            InputStream input = plugin.getResource("plugin.yml");

            try {
                pluginYml.load(new InputStreamReader(input));
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }


            apiVersion = pluginYml.getString("api-version");
            currentBuildNumber = pluginYml.getInt("build-number", 0);
            return currentBuildNumber;
        }
    }

    private void getLatestVersionInfo() {
        try {
            URL url = new URL("http://jenkins.cheracc.me/job/BattleGamesHungerRoyale/api/json?tree=lastStableBuild[number,url]");
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

            Bukkit.getLogger().info(String.format("this build:%s, latest available:%s (%s)", currentBuildNumber, mostRecentBuildAvailable, urlToMostRecentBuild));
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
                String urlString = "https://jenkins.cheracc.me/job/BattleGamesHungerRoyale" + mostRecentBuildAvailable +
                        "/artifact/target/BattleGamesHungerRoyale-" + apiVersion + ".jar";
                File toSave = new File(Bukkit.getUpdateFolderFile(), plugin.getJarFilename());

                if ((toSave.exists() && toSave.delete()) || toSave.getParentFile().mkdirs()) {
                    Bukkit.getLogger().info(String.format("update found (build #%s): preparing to download", mostRecentBuildAvailable));
                }

                try {
                    URL url = new URL(urlString);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    OutputStream out = new FileOutputStream(toSave);
                    con.setDefaultUseCaches(false);

                    InputStream input = con.getInputStream();
                    input.transferTo(out);
                    future.complete(true);
                } catch (IOException e) {
                    future.complete(false);
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    private BukkitTask runUpdateChecker() {
        BukkitRunnable task = new BukkitRunnable() {
            long last = System.currentTimeMillis();
            @Override
            public void run() {
                if (downloadStatus != null && downloadStatus.isDone()) {
                    Bukkit.getLogger().info("finished downloading plugin update. it will be installed on next restart");
                    cancel();
                    return;
                }
                if (System.currentTimeMillis() - last > 1000*60*30 && !isLatestVersion()) {
                    downloadStatus = downloadLatest();
                }
                last = System.currentTimeMillis();
            }
        };
        return task.runTaskTimer(plugin, 20*60, 20*30);
    }
}
