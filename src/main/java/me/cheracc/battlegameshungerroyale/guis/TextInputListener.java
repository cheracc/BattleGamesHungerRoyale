package me.cheracc.battlegameshungerroyale.guis;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.guis.interfaces.GetTextInput;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TextInputListener implements Listener {
    private static TextInputListener singleton = null;
    private BukkitTask cleanupTask;
    private final ConcurrentHashMap<UUID, GetTextInput> watchers;
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

    public void getNextInputFrom(Player p, GetTextInput callback) {
        watchers.put(p.getUniqueId(), callback);
        if (cleanupTask == null)
            cleanupTask = startCleanupTask();
    }

    @EventHandler
    public void watchChat(AsyncChatEvent event) {
        Set<Player> savingMessagesFor = new HashSet<>();
        if (watchers.containsKey(event.getPlayer().getUniqueId())) {

            Player p = event.getPlayer();
            String text = PlainComponentSerializer.plain().serialize(event.originalMessage());
            if (text == null) return;

            event.setCancelled(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    watchers.get(p.getUniqueId()).textCallback(text);
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
                    if (!Bukkit.getPlayer(uuid).isOnline()) {
                        watchers.remove(uuid);
                        if (watchers.isEmpty())
                            cancel();
                        break;
                    }
                }
            }
        }.runTaskTimer(BGHR.getPlugin(), 20L, 200L);
    }
}
