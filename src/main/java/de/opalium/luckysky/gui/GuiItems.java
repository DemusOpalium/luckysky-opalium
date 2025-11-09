package de.opalium.luckysky.gui;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class AdminGui implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "LuckySky Admin";
    private static final int SIZE = 27;

    private static final int SLOT_CLEAR_PLANE_Y101 = 8;  // TNT
    private static final int SLOT_CLEAR_FIELD_FULL = 9;  // GUNPOWDER
    private static final int SLOT_LOAD_SCHEM = 26;       // Load Schematic
    private static final int SLOT_PASTE_SCHEM = 0;       // Paste Schematic

    private static final String LUCKY_WORLD_NAME = "LuckySky";
    private static final int STEP_XZ = 48;
    private static final int STEP_Y = 14;

    private final LuckySkyPlugin plugin;

    public AdminGui(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, SIZE, TITLE);
        populate(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        switch (slot) {
            case SLOT_CLEAR_PLANE_Y101 -> handleClearPlaneY101(p);
            case SLOT_CLEAR_FIELD_FULL -> handleClearField300(p);
            case SLOT_LOAD_SCHEM -> loadSchematic(p);
            case SLOT_PASTE_SCHEM -> pasteSchematic(p);

            case 10 -> {
                plugin.game().start();
                Msg.to(p, "&aCountdown gestartet.");
            }
            case 11 -> {
                plugin.game().stop();
                Msg.to(p, "&eGame gestoppt & Lobby.");
            }
            case 12 -> {
                plugin.game().setDurationMinutes(5);
                Msg.to(p, "&aDauer: 5 Min.");
            }
            case 13 -> {
                plugin.game().setDurationMinutes(20);
                Msg.to(p, "&aDauer: 20 Min.");
            }
            case 14 -> {
                plugin.game().setDurationMinutes(60);
                Msg.to(p, "&aDauer: 60 Min.");
            }

            case 15 -> toggleTaunts(p);
            case 16 -> toggleWither(p);
            case 17 -> plugin.scoreboard().setEnabled(!plugin.scoreboard().isEnabled());
            case 18 -> plugin.scoreboard().setTimerVisible(!plugin.scoreboard().isTimerVisible());

            case 19 -> cycleLuckyVariant(p);
            case 20 -> Msg.to(p, "&7Soft-Wipe: &f" + plugin.game().softClear());
            case 21 -> Msg.to(p, "&7Hard-Wipe: &f" + plugin.game().hardClear());
            case 22 -> {
                plugin.game().bindAll();
                Msg.to(p, "&bAlle gebunden.");
            }
            case 23 -> {
                plugin.game().placePlatform();
                Msg.to(p, "&bPlattform gesetzt.");
            }
            case 24 -> teleportToSpawn(p);
            case 25 -> {
                plugin.configs().saveAll();
                plugin.reloadSettings();
                Msg.to(p, "&aConfig gespeichert.");
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> open(p));
    }

    private void populate(Inventory inv) {
        GameConfig game = plugin.configs().game();
        TrapsConfig traps = plugin.configs().traps();

        boolean running = plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING;
        boolean taunts = traps.withers().taunts().enabled();
        boolean wither = traps.withers().enabled();
        boolean sb = plugin.scoreboard().isEnabled();
        boolean timer = plugin.scoreboard().isTimerVisible();

        inv.setItem(SLOT_CLEAR_PLANE_Y101, GuiItems.tntClearPlaneY101());
        inv.setItem(SLOT_CLEAR_FIELD_FULL, GuiItems.fullClear0to319());
        inv.setItem(SLOT_LOAD_SCHEM, GuiItems.loadSchematic());
        inv.setItem(SLOT_PASTE_SCHEM, GuiItems.pasteSchematic());

        inv.setItem(10, GuiItems.b(
                Material.LIME_DYE, "&aStart Countdown",
                List.of("&7Startet das Spiel und teleportiert zur Plattform."), running));
        inv.setItem(11, GuiItems.b(
                Material.BARRIER, "&cStop & Lobby",
                List.of("&7Stoppt das Spiel und sendet alle zur Lobby.")));
        inv.setItem(12, GuiItems.b(Material.CLOCK, "&eMode 5", List.of("&7Setzt Dauer auf 5 Minuten.")));
        inv.setItem(13, GuiItems.b(Material.CLOCK, "&eMode 20", List.of("&7Setzt Dauer auf 20 Minuten.")));
        inv.setItem(14, GuiItems.b(Material.CLOCK, "&eMode 60", List.of("&7Setzt Dauer auf 60 Minuten.")));

        inv.setItem(15, GuiItems.t(Material.LIME_WOOL, Material.RED_WOOL, taunts,
                "Taunts AN", "Taunts AUS", List.of("&7Schaltet Wither-Taunts um.")));
        inv.setItem(16, GuiItems.t(Material.LIME_WOOL, Material.RED_WOOL, wither,
                "Wither AN", "Wither AUS", List.of("&7Aktiviert/Deaktiviert Wither-Spawns.")));
        inv.setItem(17, GuiItems.t(Material.OAK_SIGN, Material.SPRUCE_SIGN, sb,
                "Scoreboard AN", "Scoreboard AUS", List.of("&7Schaltet das LuckySky-Scoreboard.")));
        inv.setItem(18, GuiItems.t(Material.COMPASS, Material.CLOCK, timer,
                "Timer sichtbar", "Timer versteckt", List.of("&7Blendet den Timer im Scoreboard ein/aus.")));

        inv.setItem(19, GuiItems.b(Material.SPONGE, "&bLucky-Variante",
                List.of("&7Aktuell: &f" + game.lucky().variant())));
        inv.setItem(20, GuiItems.b(Material.FEATHER, "&bSoft-Wipe",
                List.of("&7Entfernt Effekte um Lucky.")));
        inv.setItem(21, GuiItems.b(Material.NETHERITE_SWORD, "&cHard-Wipe",
                List.of("&7Entfernt Entities großflächig."), true));
        inv.setItem(22, GuiItems.b(Material.RESPAWN_ANCHOR, "&bBind",
                List.of("&7Setzt Spawn für alle.")));
        inv.setItem(23, GuiItems.b(Material.PRISMARINE_BRICKS, "&bPlattform",
                List.of("&7Baut Safe-Plattform.")));
        inv.setItem(24, GuiItems.b(Material.ENDER_PEARL, "&dTeleport",
                List.of("&7Teleportiert dich zum Spawn.")));
        inv.setItem(25, GuiItems.b(Material.NAME_TAG, "&aSave Config",
                List.of("&7Speichert & lädt Config neu.")));
    }

    // ====== CLEAR Y=101 (±300) ======
    private void handleClearPlaneY101(Player p) {
        World world = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (world == null) { Msg.to(p, "&cWelt 'LuckySky' nicht gefunden!"); return; }

        // Chunks laden + forceload setzen (damit //fill nicht „position not loaded“ wirft)
        loadChunksForArea(world, -300, -300, 300, 300);
        addForceLoad(world, -300, -300, 300, 300);

        Msg.to(p, "&aBereinige Ebene y=101 (±300)...");
        dispatchIn(world, "tellraw @a {\"text\":\"LuckySky: Ebene y=101 wird gereinigt...\",\"color\":\"yellow\"}");
        disableAdminLogs(world);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final int y = 101;
            for (int x = -300; x <= 300; x += STEP_XZ) {
                for (int z = -300; z <= 300; z += STEP_XZ) {
                    int x1 = x, x2 = Math.min(x + STEP_XZ - 1, 300);
                    int z1 = z, z2 = Math.min(z + STEP_XZ - 1, 300);
                    String cmd = String.format("fill %d %d %d %d %d %d air", x1, y, z1, x2, y, z2);
                    Bukkit.getScheduler().runTask(plugin, () -> dispatchIn(world, cmd));
                    try { Thread.sleep(25); } catch (InterruptedException ignored) {}
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                restorePlatform(world);
                dispatchIn(world, "tellraw @a {\"text\":\"✔ Ebene y=101 gereinigt & Podest wiederhergestellt.\",\"color\":\"green\"}");
                Msg.to(p, "&aFertig.");
                enableAdminLogs(world);
                // Forceloads wieder entfernen (nur in dieser Dimension)
                dispatchIn(world, "forceload remove all");
            });
        });
    }

    // ====== CLEAR 0–319 (±300) ======
    private void handleClearField300(Player p) {
        World world = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (world == null) { Msg.to(p, "&cWelt 'LuckySky' nicht gefunden!"); return; }

        loadChunksForArea(world, -300, -300, 300, 300);
        addForceLoad(world, -300, -300, 300, 300);

        Msg.to(p, "&aVollbereinigung 0–319 (±300) gestartet...");
        dispatchIn(world, "tellraw @a {\"text\":\"LuckySky: Vollbereinigung läuft...\",\"color\":\"red\"}");
        disableAdminLogs(world);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int total = ((600 / STEP_XZ) + 1) * ((600 / STEP_XZ) + 1) * ((319 / STEP_Y) + 1);
            int done = 0;

            for (int x = -300; x <= 300; x += STEP_XZ) {
                for (int z = -300; z <= 300; z += STEP_XZ) {
                    for (int y = 0; y <= 319; y += STEP_Y) {
                        int x1 = x, x2 = Math.min(x + STEP_XZ - 1, 300);
                        int z1 = z, z2 = Math.min(z + STEP_XZ - 1, 300);
                        int y1 = y, y2 = Math.min(y + STEP_Y - 1, 319);
                        String cmd = String.format("fill %d %d %d %d %d %d air", x1, y1, z1, x2, y2, z2);
                        Bukkit.getScheduler().runTask(plugin, () -> dispatchIn(world, cmd));
                        done++;
                        if (done % 20 == 0) {
                            int pct = Math.min(100, done * 100 / total);
                            final String bar = "tellraw @a [\"\",{\"text\":\"Wipe: \"},{\"text\":\"" + pct + "%\",\"color\":\"gold\"}]";
                            Bukkit.getScheduler().runTask(plugin, () -> dispatchIn(world, bar));
                        }
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    }
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                restorePlatform(world);
                dispatchIn(world, "tellraw @a {\"text\":\"✔ Spielfeld 0–319 gereinigt.\",\"color\":\"dark_green\"}");
                Msg.to(p, "&aVollbereinigung abgeschlossen!");
                enableAdminLogs(world);
                dispatchIn(world, "forceload remove all");
            });
        });
    }

    // ====== SCHEM LOAD/PASTE ======
    private void loadSchematic(Player p) {
        if (!isInLuckyWorld(p)) { Msg.to(p, "&cBitte in LuckySky stehen!"); return; }
        if (!isWorldEditPresent()) { Msg.to(p, "&cWorldEdit/FAWE nicht gefunden – Ladebefehl nicht verfügbar."); return; }
        p.performCommand("//schem load platform");
        Msg.to(p, "&aSchematic 'platform' geladen.");
    }

    private void pasteSchematic(Player p) {
        if (!isInLuckyWorld(p)) { Msg.to(p, "&cBitte in LuckySky stehen!"); return; }
        if (!isWorldEditPresent()) { Msg.to(p, "&cWorldEdit/FAWE nicht gefunden – Paste nicht verfügbar."); return; }
        p.performCommand("//paste");
        Msg.to(p, "&aSchematic gepastet.");
    }

    // ====== HELPERS ======
    private void loadChunksForArea(World world, int minX, int minZ, int maxX, int maxZ) {
        int minChunkX = floorDiv(minX, 16);
        int maxChunkX = floorDiv(maxX, 16);
        int minChunkZ = floorDiv(minZ, 16);
        int maxChunkZ = floorDiv(maxZ, 16);
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                // robustes Laden; true = generieren falls nötig
                world.loadChunk(cx, cz, true);
            }
        }
    }

    private void addForceLoad(World world, int minX, int minZ, int maxX, int maxZ) {
        int minChunkX = floorDiv(minX, 16);
        int maxChunkX = floorDiv(maxX, 16);
        int minChunkZ = floorDiv(minZ, 16);
        int maxChunkZ = floorDiv(maxZ, 16);
        String cmd = String.format("forceload add %d %d %d %d", minChunkX, minChunkZ, maxChunkX, maxChunkZ);
        dispatchIn(world, cmd);
    }

    private int floorDiv(int a, int b) {
        int r = a / b;
        if ((a ^ b) < 0 && (r * b != a)) r--;
        return r;
    }

    private boolean isInLuckyWorld(Player p) {
        return p.getWorld().getName().equalsIgnoreCase(LUCKY_WORLD_NAME);
    }

    private boolean isWorldEditPresent() {
        Plugin we = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (we == null || !we.isEnabled()) {
            we = Bukkit.getPluginManager().getPlugin("WorldEdit");
        }
        return we != null && we.isEnabled();
    }

    private void restorePlatform(World world) {
        String[] cmds = {
                "setblock 0 100 -1 prismarine_stairs[facing=south,half=bottom,shape=straight]",
                "setblock 0 100 0 prismarine_stairs[facing=south,half=bottom,shape=straight]",
                "setblock 0 100 1 prismarine_bricks",
                "setblock 0 100 2 prismarine_bricks"
        };
        for (String cmd : cmds) dispatchIn(world, cmd);
    }

    private void disableAdminLogs(World world) { dispatchIn(world, "gamerule logAdminCommands false"); }
    private void enableAdminLogs(World world)  { dispatchIn(world, "gamerule logAdminCommands true"); }

    private void dispatch(String cmd) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private void dispatchIn(World world, String cmd) {
        // führt den Befehl garantiert in LuckySky aus
        String worldKey = world.getKey().toString(); // z.B. "minecraft:luckysky"
        dispatch("execute in " + worldKey + " run " + cmd);
    }

    private void toggleTaunts(Player p) {
        TrapsConfig t = plugin.configs().traps();
        boolean v = !t.withers().taunts().enabled();
        plugin.configs().updateTraps(t.withTauntsEnabled(v));
        plugin.reloadSettings();
        plugin.game().setTauntsEnabled(v);
        Msg.to(p, v ? "&aTaunts aktiviert." : "&cTaunts deaktiviert.");
    }

    private void toggleWither(Player p) {
        TrapsConfig t = plugin.configs().traps();
        boolean v = !t.withers().enabled();
        plugin.configs().updateTraps(t.withWithersEnabled(v));
        plugin.reloadSettings();
        plugin.game().setWitherEnabled(v);
        Msg.to(p, v ? "&aWither aktiviert." : "&cWither deaktiviert.");
    }

    private void cycleLuckyVariant(Player p) {
        GameConfig g = plugin.configs().game();
        List<String> vars = g.lucky().variantsAvailable();
        String cur = g.lucky().variant();
        int i = vars.indexOf(cur);
        String next = vars.get((i + 1) % vars.size());
        plugin.configs().updateGame(g.withLuckyVariant(next));
        plugin.reloadSettings();
        Msg.to(p, "&bLucky-Variante jetzt: &f" + next);
    }

    private void teleportToSpawn(Player p) {
        WorldsConfig.LuckyWorld cfg = plugin.configs().worlds().luckySky();
        World w = Worlds.require(cfg.worldName());
        WorldsConfig.Spawn s = cfg.spawn();
        p.teleport(new Location(w, s.x(), s.y(), s.z(), s.yaw(), s.pitch()));
        Msg.to(p, "&dTeleportiert.");
    }
}
