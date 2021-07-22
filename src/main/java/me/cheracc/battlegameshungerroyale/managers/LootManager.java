package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Logr;
import me.cheracc.battlegameshungerroyale.types.Game;
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
import org.bukkit.loot.LootTables;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class LootManager implements Listener {
    private final Game game;
    private final BukkitTask asyncChunkScanner;
    private final BukkitTask updateChests;
    private final BukkitTask chestRecycler;

    private final boolean generateChests;
    private final boolean loadPrePlacedChests;
    private final int maxChestsPerChunk;
    private final boolean loosenChestSearchRestrictions;
    private final long startTime;

    private final LootTable lootTable;
    private final List<Location> unusedChestLocations = new ArrayList<>();
    private final Set<Location> usedChestLocations = new HashSet<>();
    private final Queue<ChunkSnapshot> toSearch = new ConcurrentLinkedQueue<>();

    public LootManager(Game game) {
        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
        this.generateChests = game.getOptions().isGenerateChests();
        this.loadPrePlacedChests = game.getOptions().isFillAllChests();
        this.maxChestsPerChunk = game.getOptions().getMaxChestsPerChunk();
        this.loosenChestSearchRestrictions = game.getOptions().isLoosenSearchRestrictions();
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.asyncChunkScanner = asyncChunkScanner();
        this.updateChests = updateChests();
        this.chestRecycler = chestRecycler();
        if (game.getOptions().getLootTable() == null)
            this.lootTable = getDefaultLootTable();
        else
            this.lootTable = game.getOptions().getLootTable();
        Logr.info("Searching for loot chest locations...");
    }

    public void close() {
        asyncChunkScanner.cancel();
        updateChests.cancel();
        chestRecycler.cancel();
        ChunkLoadEvent.getHandlerList().unregister(this);
    }

    public void placeLootChests(int amount) {
        Set<Location> selected = new HashSet<>();
        Random rand = ThreadLocalRandom.current();

        if (!generateChests)
            return;

        if (amount >= unusedChestLocations.size()) {
            selected.addAll(unusedChestLocations);
        }
        else {
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

    private BukkitTask updateChests() {
        BukkitRunnable addShinies = new BukkitRunnable() {
            @Override
            public void run() {
                Set<Location> toRemove = new HashSet<>();
                for (Location l : usedChestLocations) {
                    l.getBlock();
                    if (l.getBlock().getType() != Material.CHEST) {
                        toRemove.add(l);
                        continue;
                    }
                    Chest chest = (Chest) l.getBlock().getState();
                    if (!chest.hasBeenFilled())
                        l.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, l.clone().add(0.5,0.5,0.5), 5, 0.5, 0.5, 0.5);
                }
                usedChestLocations.removeAll(toRemove);
                unusedChestLocations.addAll(toRemove);
                toRemove.clear();
            }
        };
        return addShinies.runTaskTimer(BGHR.getPlugin(), 10L, 8L);
    }

    private BukkitTask asyncChunkScanner() {
        BukkitRunnable asyncScanner = new BukkitRunnable() {
            long last = System.currentTimeMillis();
            int scanned = 0;
            @Override
            public void run() {
                if (!game.getPhase().equalsIgnoreCase("pregame") || (scanned > 10 && toSearch.size() == 0 && System.currentTimeMillis() - startTime > 1000*10)) {
                    cancel();
                    Logr.info(String.format("Finished searching, found %s loot chest locations.",
                            unusedChestLocations.size() + usedChestLocations.size()));
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
        };
        return asyncScanner.runTaskTimerAsynchronously(BGHR.getPlugin(), 20L, 2L);
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
                    if (chunk.isSectionEmpty(y/16)) {
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
                            unusedChestLocations.add(center.getLocation()); };
                        count++;
                        game.getWorld().getChunkAtAsync(chunk.getX(), chunk.getZ(), addThisLocation);
                    }
                    else {
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
                                unusedChestLocations.add(center.getLocation()); };
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
                        unusedChestLocations.add(center.getLocation()); };

                    tries++;
                    if (checkIfGoodForChest(cluster, 1)) {
                        count++;
                        game.getWorld().getChunkAtAsync(chunk.getX(), chunk.getZ(), addThisLocation);
                    }
                }
            }
        }.runTaskAsynchronously(BGHR.getPlugin());

    }

    private BukkitTask chestRecycler() {
        BukkitRunnable recycler = new BukkitRunnable() {
            @Override
            public void run() {
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
        };
        return recycler.runTaskTimer(BGHR.getPlugin(), 200L, 100L);
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
        LootTable table = Bukkit.getLootTable(new NamespacedKey(BGHR.getPlugin(), lootTable.getKey().getKey()));
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


    private final Set<Long> loadedChunkKeys = new HashSet<>();
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

    private final static Collection<LootTable> LOOT_TABLES = new HashSet<>();

    private static void loadLootTables() {
        BGHR plugin = BGHR.getPlugin();

        for (String s : MapManager.getInstance().getLootTableNames()) {
            LootTable t = Bukkit.getLootTable(new NamespacedKey(plugin, s));
            if (t != null)
                LOOT_TABLES.add(t);
        }

        if (!LOOT_TABLES.isEmpty()) {
            StringBuilder names = new StringBuilder(" ");
            for (LootTable t : LOOT_TABLES) {
                names.append(t.getKey().getKey());
                names.append(" ");
            }
            Logr.info(String.format("Loaded custom loot tables: [%s]", names));
        }
        else {
            // default loot tables
            for (LootTables t : LootTables.values()) {
                if (t.getKey().getKey().contains("chest"))
                    LOOT_TABLES.add(t.getLootTable());
            }
        }
    }

    public static List<LootTable> getLootTables() {
        if (LOOT_TABLES.isEmpty())
            loadLootTables();
        return new ArrayList<>(LOOT_TABLES);
    }

    public static LootTable getDefaultLootTable() {
        LootTable lt = Bukkit.getLootTable(new NamespacedKey(BGHR.getPlugin(), "default"));
        if (lt == null) {
            Bukkit.getLogger().warning("Could not find default loot table");
        }
        return lt;
    }

    public static LootTable getLootTableFromKey(String key) {
        if (LOOT_TABLES.isEmpty())
            loadLootTables();
        for (LootTable lt : LOOT_TABLES) {
            if (lt.getKey().getKey().equalsIgnoreCase(key))
                return lt;
        }
        return null;
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
        if (b.getState() instanceof Chest)  {
            Chest chest = (Chest) b.getState();
            if (!chest.hasBeenFilled() && !chest.hasLootTable()) {
                chest.setLootTable(lootTable);
                chest.update(true);
                usedChestLocations.add(chest.getLocation());
            }
        }
    }

}
