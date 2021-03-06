package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class LootManager implements Listener {
    private final Game game;
    private final Logr logr;
    private final BGHR plugin;
    private final BukkitTask asyncChunkScanner;
    private final boolean generateChests;
    private final boolean loadPrePlacedChests;
    private final int maxChestsPerChunk;
    private final boolean loosenChestSearchRestrictions;
    private final long startTime;
    private final LootTable lootTable;
    private final List<Location> unusedChestLocations = new ArrayList<>();
    private final Set<Location> usedChestLocations = new HashSet<>();
    private final Queue<ChunkSnapshot> toSearch = new ConcurrentLinkedQueue<>();
    private final Set<Long> loadedChunkKeys = new HashSet<>();
    private long lastChestRespawn;
    private int tickCounter = 0;

    // TODO make a ChestScanner class and move all associated stuff into that

    public LootManager(Game game, BGHR plugin, Logr logr, GameManager gameManager) {
        this.plugin = plugin;
        this.logr = logr;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.generateChests = game.getOptions().isGenerateChests();
        this.loadPrePlacedChests = game.getOptions().isFillAllChests();
        this.maxChestsPerChunk = game.getOptions().getMaxChestsPerChunk();
        this.loosenChestSearchRestrictions = game.getOptions().isLoosenSearchRestrictions();
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.asyncChunkScanner = asyncChunkScanner();
        lastChestRespawn = System.currentTimeMillis();
        if (game.getOptions().getLootTable() == null)
            this.lootTable = gameManager.getDefaultLootTable();
        else
            this.lootTable = game.getOptions().getLootTable();
        logr.info("Searching for loot chest locations...");
    }

    public void close() {
        plugin.getLogr().debug("Stopping async chunk scanner");
        asyncChunkScanner.cancel();
        ChunkLoadEvent.getHandlerList().unregister(this);
    }

    public void tick() {
        tickCounter++;
        if (tickCounter > 9)
            tickCounter = 0;

        if ((System.currentTimeMillis() - lastChestRespawn) / 1000 / 60 >= game.getOptions().getChestRespawnTime()) {
            // TODO add the ability to modify this 'density' number for loot chests
            placeLootChests((int) (game.getActivePlayers().size() * 5 * Math.sqrt(game.getMap().getBorderRadius())));
            lastChestRespawn = System.currentTimeMillis();
        }
        updateChests();
        if (tickCounter == 0)
            recycleChests();
    }

    public void placeLootChests(int amount) {
        Set<Location> selected = new HashSet<>();
        Random rand = ThreadLocalRandom.current();

        if (!generateChests)
            return;

        if (amount >= unusedChestLocations.size()) {
            selected.addAll(unusedChestLocations);
        } else {
            Collections.shuffle(unusedChestLocations);
            while (selected.size() < amount) {
                int index = rand.nextInt(unusedChestLocations.size() - 1);
                selected.add(unusedChestLocations.get(index));
            }
        }

        for (Location l : selected) {
            if (makeLootChestAt(l)) {
                usedChestLocations.add(l);
            }
            unusedChestLocations.remove(l);
        }
    }

    private void updateChests() {
        Set<Location> toRemove = new HashSet<>();
        for (Location l : usedChestLocations) {
            l.getBlock();
            if (l.getBlock().getType() != Material.CHEST) {
                toRemove.add(l);
                continue;
            }
            Chest chest = (Chest) l.getBlock().getState();
            if (!chest.hasBeenFilled())
                l.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, l.clone().add(0.5, 0.5, 0.5), 5, 0.5, 0.5, 0.5);
        }
        usedChestLocations.removeAll(toRemove);
        unusedChestLocations.addAll(toRemove);
        toRemove.clear();
    }

    private BukkitTask asyncChunkScanner() {
        BukkitRunnable asyncScanner = new BukkitRunnable() {
            long last = System.currentTimeMillis();
            int scanned = 0;

            @Override
            public void run() {
                if (generateChests) {
                    if (!game.getPhase().equalsIgnoreCase("pregame") || (scanned > 10 && toSearch.size() == 0 && System.currentTimeMillis() - startTime > 1000 * 10)) {
                        cancel();
                        logr.info("Finished searching, found %s loot chest locations.",
                                  unusedChestLocations.size() + usedChestLocations.size());
                        return;
                    }

                    ChunkSnapshot chunk = toSearch.poll();
                    if (chunk != null) {
                        searchChunkForChestLocations(chunk, maxChestsPerChunk);
                        scanned++;
                    }

                    if (System.currentTimeMillis() - last > 5000) {
                        last = System.currentTimeMillis();
                    }
                }
            }
        };
        return asyncScanner.runTaskTimerAsynchronously(plugin, 20L, 2L);
    }

    private void searchChunkForChestLocations(ChunkSnapshot chunk, int maxChestsPerChunk) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Random rand = ThreadLocalRandom.current();

                int tries = 0;
                int count = 0;
                boolean skipSecondChance = false;

                while (count < maxChestsPerChunk && tries <= 1000 && game.getWorld() != null) {
                    int x = rand.nextInt(15);
                    int z = rand.nextInt(15);
                    int yTop = chunk.getHighestBlockYAt(x, z);
                    if (yTop <= 10 || yTop >= 255) {
                        tries += 100; // this is probably an empty chunk, lets speed it up
                        skipSecondChance = true;
                        continue;
                    }
                    int y = rand.nextInt(yTop - 10) + 10;
                    if (chunk.isSectionEmpty(y / 16)) {
                        tries += 10;
                        skipSecondChance = true;
                        continue;
                    }

                    BlockDataCluster cluster = new BlockDataCluster(chunk, x, y, z);
                    int needed = 2;
                    if (loosenChestSearchRestrictions)
                        needed--;
                    if (chunk.getBlockSkyLight(x, y, z) <= 8)
                        needed--;
                    tries++;
                    if (checkIfGoodForChest(cluster, needed)) {
                        Consumer<Chunk> addThisLocation = ch -> {
                            Block center = ch.getBlock(x, y, z);
                            unusedChestLocations.add(center.getLocation());
                        };
                        count++;
                        game.getWorld().getChunkAtAsync(chunk.getX(), chunk.getZ(), addThisLocation);
                    } else {
                        tries++;
                        needed = 3;
                        if (loosenChestSearchRestrictions)
                            needed--;
                        if (chunk.getBlockSkyLight(x, yTop, z) <= 8)
                            needed = 1;
                        cluster = new BlockDataCluster(chunk, x, yTop, z);
                        if (checkIfGoodForChest(cluster, needed)) {
                            Consumer<Chunk> addThisLocation = ch -> {
                                Block center = ch.getBlock(x, yTop, z);
                                unusedChestLocations.add(center.getLocation());
                            };
                            count++;
                            game.getWorld().getChunkAtAsync(chunk.getX(), chunk.getZ(), addThisLocation);
                        }
                    }
                }
                while (count < 5 && !skipSecondChance && tries < 1200) {
                    int x = rand.nextInt(15);
                    int z = rand.nextInt(15);
                    int yTop = chunk.getHighestBlockYAt(x, z);
                    BlockDataCluster cluster = new BlockDataCluster(chunk, x, yTop, z);
                    Consumer<Chunk> addThisLocation = ch -> {
                        Block center = ch.getBlock(x, yTop, z);
                        unusedChestLocations.add(center.getLocation());
                    };

                    tries++;
                    if (checkIfGoodForChest(cluster, 1)) {
                        count++;
                        game.getWorld().getChunkAtAsync(chunk.getX(), chunk.getZ(), addThisLocation);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void recycleChests() {
        for (Location l : usedChestLocations) {
            if (l.getBlock().getType() == Material.CHEST) {
                Chest chest = (Chest) l.getBlock().getState();
                if (chest.hasBeenFilled() && l.getNearbyPlayers(5).isEmpty()) {
                    recycleChestAt(l);
                    return;
                }
            }
        }
    }

    private void recycleChestAt(Location location) {
        if (location.getBlock().getType() != Material.CHEST)
            return;

        Chest chest = (Chest) location.getBlock().getState();
        chest.clearLootTable();
        chest.getBlockInventory().clear();
        chest.update(true);
        ((InventoryHolder) chest.getBlock().getState()).getInventory().clear();
        chest.getBlock().setType(Material.AIR);
        location.getWorld().playEffect(location, Effect.LAVA_CONVERTS_BLOCK, null);

        usedChestLocations.remove(location);
        unusedChestLocations.add(location);
    }

    public boolean makeLootChestAt(Location location) {
        Block b = location.getBlock();
        // look for adjacent chests and cancel if found
        for (BlockFace face : BlockFace.values()) {
            if (b.getRelative(face).getType() == Material.CHEST)
                return false;
        }
        b.setType(Material.CHEST);
        Chest chest = (Chest) b.getState();
        LootTable table = Bukkit.getLootTable(new NamespacedKey(plugin, lootTable.getKey().getKey()));
        chest.setLootTable(table);
        chest.update(true);
        Directional data = (Directional) chest.getBlockData();
        data.setFacing(getGoodFacing(b));
        chest.setBlockData(data);
        location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, null);
        return true;
    }

    private BlockFace getGoodFacing(Block block) {
        BlockFace[] sides = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        List<BlockFace> sidesList = new ArrayList<>(Arrays.asList(sides));

        Collections.shuffle(sidesList);
        for (BlockFace face : sidesList) {
            if (!block.getRelative(face).isSolid() || block.getRelative(face).isLiquid())
                return face;
        }
        return BlockFace.NORTH;
    }

    private boolean checkIfGoodForChest(BlockDataCluster cluster, int adjacentBlockNeeded) {
        if (cluster.above == null || cluster.below == null)
            return false;

        if (cluster.center.getMaterial().isSolid()) // center must not be solid
            return false;

        if (cluster.above.getMaterial().isOccluding()) // block above must be not-solid or water
            return false;

        if (!cluster.below.getMaterial().isSolid()) // block below must be solid
            return false;

        int count = 0;
        for (BlockData d : cluster.sides) { // look for at least x adjacent non-air blocks
            if (!d.getMaterial().isAir())
                count++;
        }
        return count >= adjacentBlockNeeded;
    }

    @EventHandler
    public void grabChunksAsTheyAreLoaded(ChunkLoadEvent event) {
        if (game == null || game.getWorld() == null || loadedChunkKeys.contains(event.getChunk().getChunkKey()))
            return;
        if (event.getWorld().equals(game.getWorld()) && game.getPhase().equalsIgnoreCase("pregame")) {
            Location center = game.getMap().getBorderCenter(game.getWorld());
            Location chunkCenter = event.getChunk().getBlock(8, (int) center.getY(), 8).getLocation();

            loadedChunkKeys.add(event.getChunk().getChunkKey());
            if (center.distance(chunkCenter) > game.getMap().getBorderRadius() * 0.9)
                return;

            ChunkSnapshot chunk = event.getChunk().getChunkSnapshot();
            if (!toSearch.contains(chunk)) {
                toSearch.add(chunk);
            }
        }
    }

    @EventHandler
    public void populatePrePlacedChests(InventoryOpenEvent event) {
        if (!event.getPlayer().getWorld().equals(game.getWorld()) || !loadPrePlacedChests)
            return;

        if (game.getPhase().equalsIgnoreCase("pregame") || game.getPhase().equalsIgnoreCase("postgame"))
            return;

        Inventory inv = event.getInventory();
        Location loc = inv.getLocation();

        if (loc == null)
            return;

        Block b = loc.getBlock();
        if (b.getState() instanceof Chest) {
            Chest chest = (Chest) b.getState();
            if (!chest.hasBeenFilled() && !chest.hasLootTable()) {
                chest.setLootTable(lootTable);
                chest.update(true);
                usedChestLocations.add(chest.getLocation());
            }
        }
    }

    private static class BlockDataCluster {
        BlockData center;
        BlockData above = null;
        BlockData below = null;
        Set<BlockData> sides = new HashSet<>();

        // south = +z, east = +x
        public BlockDataCluster(ChunkSnapshot chunk, int centerX, int centerY, int centerZ) {
            center = chunk.getBlockData(centerX, centerY, centerZ);
            if (centerY - 1 >= 0)
                above = chunk.getBlockData(centerX, centerY + 1, centerZ);
            if (centerY - 1 <= 256 && centerY >= 0)
                below = chunk.getBlockData(centerX, centerY - 1, centerZ);
            if (centerZ - 1 >= 0)
                sides.add(chunk.getBlockData(centerX, centerY, centerZ - 1));
            if (centerZ + 1 <= 15)
                sides.add(chunk.getBlockData(centerX, centerY, centerZ + 1));
            if (centerX + 1 <= 15)
                sides.add(chunk.getBlockData(centerX + 1, centerY, centerZ));
            if (centerX - 1 >= 0)
                sides.add(chunk.getBlockData(centerX - 1, centerY, centerZ));
        }
    }
}
