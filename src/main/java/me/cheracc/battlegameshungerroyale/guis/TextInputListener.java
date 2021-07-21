package me.cheracc.battlegameshungerroyale.guis;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Logr;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TextInputListener implements Listener {
    private static TextInputListener singleton = null;
    private BukkitTask cleanupTask;
    private final ConcurrentHashMap<UUID, Consumer<String>> watchers;
    private final ConcurrentHashMap<UUID, List<Component>> missedMessages;

    private TextInputListener() {
        watchers = new ConcurrentHashMap<>();
        missedMessages = new ConcurrentHashMap<>();
    }

    public static TextInputListener getInstance() {
        if (singleton == null)
            singleton = new TextInputListener();

        return singleton;
    }

    public void getNextInputFrom(Player p, Consumer<String> callback) {
        watchers.put(p.getUniqueId(), callback);
        if (cleanupTask == null)
            cleanupTask = startCleanupTask();
    }

    @EventHandler
    public void watchChat(AsyncChatEvent event) {
        Set<Player> savingMessagesFor = new HashSet<>();
        if (watchers.containsKey(event.getPlayer().getUniqueId())) {

            Player p = event.getPlayer();
            String text = LegacyComponentSerializer.legacyAmpersand().serialize(event.originalMessage());

            event.setCancelled(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    watchers.get(p.getUniqueId()).accept(text);
                    watchers.remove(p.getUniqueId());
                    sendMissedChatMessages(p);
                }
            }.runTask(BGHR.getPlugin());

        } else if (!event.viewers().isEmpty()) {
            for (Audience a : event.viewers()) {
                if (a instanceof Player && watchers.containsKey(((Player) a).getUniqueId())) {
                    missedMessages.computeIfAbsent(((Player) a).getUniqueId(), k -> new ArrayList<>());
                    missedMessages.get(((Player) a).getUniqueId()).add(event.renderer()
                            .render(event.getPlayer(), event.getPlayer().displayName(), event.message(), a));
                    savingMessagesFor.add((Player) a);
                }
            }
        }
        for (Player p : savingMessagesFor) {
            event.viewers().remove(p);
        }
    }

    private void sendMissedChatMessages(Player p) {
        if (missedMessages.containsKey(p.getUniqueId())) {
            p.sendMessage(Tools.componentalize("These came in while you were typing:"));
            for (Component c : missedMessages.get(p.getUniqueId())) {
                p.sendMessage(c);
            }
            missedMessages.remove(p.getUniqueId());
        }
    }

    private BukkitTask startCleanupTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new ArrayList<>(watchers.keySet())) {
                    if (Bukkit.getPlayer(uuid) == null || !Bukkit.getPlayer(uuid).isOnline()) {
                        watchers.remove(uuid);
                        if (watchers.isEmpty())
                            cancel();
                        break;
                    }
                }
            }
        }.runTaskTimer(BGHR.getPlugin(), 20L, 200L);
    }

    @EventHandler
    public void bookInputListener(PlayerEditBookEvent event) {
        if (event.isSigning()) {
            BookMeta meta = event.getNewBookMeta();
            Player p = event.getPlayer();
            if (p != null && meta.getPersistentDataContainer().has(Tools.PLUGIN_KEY, PersistentDataType.STRING)) {
                if (!meta.getPersistentDataContainer().get(Tools.PLUGIN_KEY, PersistentDataType.STRING).equalsIgnoreCase("mysql")) {
                    return;
                }
                Component page = meta.page(1);
                String hostname = null, port = null, database = null, user = null, pass = null;

                for (String plain : Tools.decomponentalize(page).split("\\r?\\n")) {
                    plain = ChatColor.stripColor(plain);
                    String[] split = plain.toLowerCase().replace(" ", "").split(":");
                    if (split.length < 2)
                        continue;
                    Logr.info(split[0]);
                    if (split[0].contains("adr")) {
                        hostname = split[1];
                        continue;
                    }
                    if (split[0].contains("prt")) {
                        port = split[1];
                        continue;
                    }
                    if (split[0].contains("db")) {
                        database = split[1];
                        continue;
                    }
                    if (split[0].contains("usr")) {
                        user = split[1];
                        continue;
                    }
                    if (split[0].contains("pwd")) {
                        pass = split[1];
                    }

                }
                if (hostname == null || port == null || database == null || user == null || pass == null) {
                    p.sendMessage(Tools.componentalize("Missing one or more required values. Could not connect. Please try again."));

                    event.setCancelled(true);
                    return;
                }
                String connectString = String.format("jdbc:mysql://%s:%s/%s", hostname, port, database);
                Logr.info(connectString);
                try (Connection con = DriverManager.getConnection(connectString, user, pass);
                     PreparedStatement stmt = con.prepareStatement("CREATE TABLE IF NOT EXISTS db_version (lockcol CHAR(1) PRIMARY KEY, version TINYINT)");
                     ResultSet result = stmt.executeQuery())   {
                    Logr.info(result.getCursorName());
                } catch (SQLException e) {
                    p.sendMessage(Tools.componentalize("Could not connect. Error: &c" + e.getMessage()));
                    p.sendMessage(Tools.componentalize("Re-open the book and try again."));
                    event.setCancelled(true);
                    return;
                }
                BGHR plugin = BGHR.getPlugin();
                FileConfiguration config = plugin.getConfig();

                config.set("mysql.hostname", hostname);
                config.set("mysql.port", port);
                config.set("mysql.database", database);
                config.set("mysql.user", user);
                config.set("mysql.pass", pass);
                config.set("mysql.arguments", " ");
                plugin.saveConfig();

                p.sendMessage(Tools.componentalize("Connection Successful! You will need to restart for this change to take effect."));
                new AdminGui(p).sendPluginAdminGui(p);
            }
        }
    }
}
