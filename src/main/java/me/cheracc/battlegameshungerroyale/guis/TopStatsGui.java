package me.cheracc.battlegameshungerroyale.guis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.builder.item.SkullBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import org.bukkit.entity.HumanEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TopStatsGui extends Gui {
    private final BghrApi api;
    private final List<CompletableFuture<TopTenCategory>> uncompletedFutures = new ArrayList<>();
    private final LinkedHashMap<String, TopTenCategory> finishedCategories = new LinkedHashMap<>();
    String[] columns = new String[]{"kills", "killstreak", "deaths", "wins", "secondplaces", "totaltime", "quits", "damagedealt", "damagetaken", "activeabilities", "chests", "itemslooted", "arrowsshot", "monsterskilled", "animalskilled", "foodeaten"};
    String[] prettyNames = new String[]{"Kills", "Highest Kill Streak", "Deaths", "Wins", "Second Place Finishes", "Total Time Played", "Games Quit", "Damage Dealt", "Damage Taken", "Abilities Used", "Chests Opened", "Items Looted", "Arrows Shot", "Monsters Killed", "Animals Killed", "Food Eaten"};

    public TopStatsGui(BghrApi api) {
        super(2, "Top Players", InteractionModifier.VALUES);
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());
        this.api = api;
        BukkitTask completionChecker = watchForCompleted();

        for (int i = 0; i < columns.length; i++) {
            uncompletedFutures.add(new TopTenCategory(columns[i], prettyNames[i]).loadTopTen());
        }
    }

    public void send(HumanEntity player) {
        for (int i = 0; i < columns.length; i++) {
            setItem(i, categoryIcon(finishedCategories.get(columns[i])));
        }
        open(player);
    }

    private GuiItem categoryIcon(TopTenCategory statistic) {
        SkullBuilder icon = ItemBuilder.skull().texture(statistic.getTopTexture());
        icon.name(Trans.lateToComponent("&bTop 10: &f&l%s", statistic.getPrettyName()));
        List<String> lore = new ArrayList<>(statistic.getOutput());
        lore.add(0, "&8========================");
        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem();
    }

    private BukkitTask watchForCompleted() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                List<CompletableFuture<TopTenCategory>> toRemove = new ArrayList<>();
                for (CompletableFuture<TopTenCategory> f : uncompletedFutures) {
                    if (f.isDone()) {
                        try {
                            TopTenCategory category = f.get();
                            finishedCategories.put(category.getColumnName(), category);
                            toRemove.add(f);
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
                uncompletedFutures.removeAll(toRemove);
                if (uncompletedFutures.isEmpty()) {
                    api.logr().info("Finished loading TopStats");
                    cancel();
                }
            }
        };
        return task.runTaskTimer(api.getPlugin(), 40, 5L);
    }

    public TopTenCategory getTopTen(String columnName) {
        return finishedCategories.get(columnName);
    }

    public class TopTenCategory {
        private final String prettyName;
        private final String columnName;
        LinkedHashMap<String, Integer> topScores = new LinkedHashMap<>();
        private String topTexture;

        public TopTenCategory(String columnName, String prettyName) {
            this.columnName = columnName;
            this.prettyName = prettyName;

            try (Connection con = api.getDatabaseManager().getConnection();
                 PreparedStatement stmt = con.prepareStatement("SELECT uuid," + columnName + " FROM player_stats ORDER BY " + columnName + " DESC LIMIT 10");
                 ResultSet result = stmt.executeQuery()) {
                boolean first = true;
                try {
                    while (result.next()) {
                        String uuid = result.getString("uuid");
                        String name;
                        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
                        URLConnection request = new URL(url).openConnection();
                        request.connect();

                        JsonParser parser = new JsonParser();
                        JsonElement root = parser.parse(new InputStreamReader((InputStream) request.getContent()));
                        JsonObject rootObj = root.getAsJsonObject();

                        name = rootObj.get("name").getAsString();
                        topScores.put(name, result.getInt(columnName));

                        JsonArray properties = rootObj.getAsJsonArray("properties");
                        String base64Textures = null;
                        for (JsonElement ele : properties) {
                            if (ele.isJsonObject()) {
                                JsonObject obj = ele.getAsJsonObject();
                                if (obj.get("name").getAsString().equals("textures")) {
                                    base64Textures = obj.get("value").getAsString();
                                }
                            }
                        }
                        if (first && base64Textures != null) {
                            topTexture = base64Textures;
                            first = false;
                        }
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public String getTopTexture() {
            return topTexture;
        }

        public List<String> getOutput() {
            List<String> output = new ArrayList<>();

            int i = 1;
            for (Map.Entry<String, Integer> e : topScores.entrySet()) {
                output.add(String.format("#%-4s %s%s &8(&7%s&8)", i + ":", i == 1 ? "&e" : i == 2 ? "&6" : i == 3 ? "&6" : "&9", e.getKey(), e.getValue()));
                i++;
            }

            return output;
        }

        public String getPlaceName(int place) {
            List<String> list = new ArrayList<>(topScores.keySet());
            if (list.size() > place - 1)
                return list.get(place - 1);
            return "";
        }

        public int getPlaceValue(int place) {
            List<Integer> list = new ArrayList<>(topScores.values());
            if (list.size() > place - 1)
                return list.get(place - 1);
            return 0;
        }

        public String getPrettyName() {
            return prettyName;
        }

        public String getColumnName() {
            return columnName;
        }

        public CompletableFuture<TopTenCategory> loadTopTen() {
            CompletableFuture<TopTenCategory> future = new CompletableFuture<>();
            new BukkitRunnable() {
                @Override
                public void run() {
                    future.complete(new TopTenCategory(columnName, prettyName));
                }
            }.runTaskAsynchronously(api.getPlugin());
            return future;
        }
    }
}
