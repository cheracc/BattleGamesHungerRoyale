package me.cheracc.battlegameshungerroyale.listeners;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TextInputListener implements Listener {
    private final BGHR plugin;
    private BukkitTask cleanupTask;
    private final ConcurrentHashMap<UUID, Consumer<String>> watchers;
    private final ConcurrentHashMap<UUID, List<Component>> missedMessages;

    public TextInputListener(BGHR plugin) {
        this.plugin = plugin;
        watchers = new ConcurrentHashMap<>();
        missedMessages = new ConcurrentHashMap<>();
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
            }.runTask(plugin);

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
            p.sendMessage(Trans.lateToComponent("These came in while you were typing:"));
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
        }.runTaskTimer(plugin, 20L, 200L);
    }
}
