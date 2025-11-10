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
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class AdminGui implements Listener {

    private static final String TITLE = ChatColor.DARK_AQUA + "LuckySky Admin";
    private static final int SIZE = 27;

    // === SLOTS (neu sortiert) ===
    private static final int SLOT_PASTE_SCHEM = 0;          // STRUCTURE_BLOCK
    private static final int SLOT_TOGGLE_WITHER = 6;        // WITHER_SKULL
    private static final int SLOT_RESET_WORLD = 7;          // TNT
    private static final int SLOT_SPAWN_WITHER = 8;         // WITHER TEST-SPAWN
    private static final int SLOT_SPAWN_LUCKY = 9;          // LUCKY-BLOCK TEST-SPAWN
    private static final int SLOT_LOAD_SCHEM = 26;          // PAPER

    private static final String LUCKY_WORLD_NAME = "LuckySky";

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

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        switch (slot) {
            case SLOT_LOAD_SCHEM:
                loadSchematic(p);
                break;
            case SLOT_PASTE_SCHEM:
                pasteSchematic(p);
                break;
            case SLOT_RESET_WORLD:
                resetWorld(p);
                break;
            case SLOT_TOGGLE_WITHER:
                toggleWither(p);
                break;

            // NEU: Test-Spawns
            case SLOT_SPAWN_WITHER:
                spawnWitherTest(p);
                break;
            case SLOT_SPAWN_LUCKY:
                spawnLuckyBlockTest(p);
                break;

            // Bestehende Buttons (klassisch mit breaks)
            case 10:
                plugin.game().start(); Msg.to(p, "&aCountdown gestartet.");
                break;
            case 11:
                plugin.game().stop(); Msg.to(p, "&eGame gestoppt & Lobby.");
                break;
            case 12:
                plugin.game().setDurationMinutes(5); Msg.to(p, "&aDauer: 5 Min.");
                break;
            case 13:
                plugin.game().setDurationMinutes(20); Msg.to(p, "&aDauer: 20 Min.");
                break;
            case 14:
                plugin.game().setDurationMinutes(60); Msg.to(p, "&aDauer: 60 Min.");
                break;
            case 15:
                toggleTaunts(p);
                break;
            case 17:
                plugin.scoreboard().setEnabled(!plugin.scoreboard().isEnabled());
                break;
            case 18:
                plugin.scoreboard().setTimerVisible(!plugin.scoreboard().isTimerVisible());
                break;
            case 19:
                cycleLuckyVariant(p);
                break;
            case 20:
                Msg.to(p, "&7Soft-Wipe: &f" + plugin.game().softClear());
                break;
            case 21:
                Msg.to(p, "&7Hard-Wipe: &f" + plugin.game().hardClear());
                break;
            case 22:
                plugin.game().bindAll(); Msg.to(p, "&bAlle gebunden.");
                break;
            case 23:
                plugin.game().placePlatform(); Msg.to(p, "&bPlattform gesetzt.");
                break;
            case 24:
                teleportToSpawn(p);
                break;
            case 25:
                plugin.configs().saveAll(); plugin.reloadSettings(); Msg.to(p, "&aConfig gespeichert.");
                break;
            default:
                break;
        }

        Bukkit.getScheduler().runTask(plugin, () -> open(p));
    }

    private void populate(Inventory inv) {
        GameConfig game = plugin.configs().game();
        TrapsConfig traps = plugin.configs().traps();
        boolean running = plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING;

        // Schematic-Buttons
        inv.setItem(SLOT_LOAD_SCHEM, GuiItems.adminLoadSchem());
        inv.setItem(SLOT_PASTE_SCHEM, GuiItems.adminPasteSchem());

        // Reset Welt
        inv.setItem(SLOT_RESET_WORLD, GuiItems.adminResetWorld());

        // Wither Toggle
        boolean witherEnabled = traps.withers().enabled();
        inv.setItem(SLOT_TOGGLE_WITHER, GuiItems.adminToggleWither(witherEnabled));

        // NEU: Test-Spawns
        inv.setItem(SLOT_SPAWN_WITHER, GuiItems.adminSpawnWither());
        inv.setItem(SLOT_SPAWN_LUCKY, GuiItems.adminSpawnLuckyBlock());

        // Laufende Game-Controls
        inv.setItem(10, GuiItems.adminStart(running));
        inv.setItem(11, GuiItems.adminStop());
        inv.setItem(12, GuiItems.adminDuration(5));
        inv.setItem(13, GuiItems.adminDuration(20));
        inv.setItem(14, GuiItems.adminDuration(60));

        boolean tauntsEnabled = traps.withers().taunts().enabled();
        inv.setItem(15, GuiItems.adminTaunts(tauntsEnabled));

        boolean scoreboardEnabled = plugin.scoreboard().isEnabled();
        boolean timerVisible = plugin.scoreboard().isTimerVisible();
        inv.setItem(17, GuiItems.adminScoreboard(scoreboardEnabled));
        inv.setItem(18, GuiItems.adminTimer(timerVisible));
        inv.setItem(19, GuiItems.adminVariant(game.lucky().variant()));
        inv.setItem(20, GuiItems.button(Material.FEATHER, "&bSoft-Wipe",
                java.util.Arrays.asList("&7Entfernt Effekte um Lucky.")));
        inv.setItem(21, GuiItems.button(Material.NETHERITE_SWORD, "&cHard-Wipe",
                java.util.Arrays.asList("&7Entfernt Entities großflächig.")));
        inv.setItem(22, GuiItems.adminBind());
        inv.setItem(23, GuiItems.adminPlatform());
        inv.setItem(24, GuiItems.adminTeleport());
        inv.setItem(25, GuiItems.adminSaveConfig());
    }

    // ===================================================================
    // === MULTIVERSE: WELT RESET =======================================
    // ===================================================================
    private void resetWorld(Player p) {
        if (!hasMV()) { Msg.to(p, "&cMultiverse-Core fehlt!"); return; }

        World world = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (world == null) { Msg.to(p, "&cWelt 'LuckySky' nicht gefunden!"); return; }

        final long seed = world.getSeed();
        Msg.to(p, "&eWelt wird resettet... Spieler werden teleportiert.");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override public void run() {
                // 1) Spieler raus
                sync(new Runnable() { @Override public void run() {
                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        World lobby = Bukkit.getWorlds().get(0);
                        pl.teleport(lobby.getSpawnLocation());
                    }
                }});

                // 2) Delete & confirm
                sync(new Runnable() { @Override public void run() {
                    dispatch("mv delete " + LUCKY_WORLD_NAME);
                    dispatch("mv confirm");
                }});

                waitForWorldUnload(LUCKY_WORLD_NAME);

                // 3) Neu erstellen (flat, gleicher Seed)
                sync(new Runnable() { @Override public void run() {
                    dispatch("mv create " + LUCKY_WORLD_NAME + " NORMAL -t FLAT -s " + seed);
                }});

                // 4) kurze Pause + Plattform bauen
                sleep(1500);
                sync(new Runnable() { @Override public void run() {
                    World nw = Bukkit.getWorld(LUCKY_WORLD_NAME);
                    if (nw != null) {
                        nw.setSpawnLocation(0, 101, 0);
                        plugin.game().placePlatform();
                        dispatchIn(nw, "tellraw @a {\"text\":\"Welt wurde resettet & Plattform neu gebaut!\",\"color\":\"green\"}");
                        Msg.to(p, "&aWelt erfolgreich resettet!");
                    } else {
                        Msg.to(p, "&cFehler: Welt nach Create nicht geladen.");
                    }
                }});
            }
        });
    }

    // ===================================================================
    // === NEU: TEST-SPAWNS ==============================================
    // ===================================================================
    private void spawnWitherTest(Player p) {
        if (!isInLuckyWorld(p)) { Msg.to(p, "&cIn LuckySky sein!"); return; }
        Location at = p.getLocation();
        at.getWorld().spawnEntity(at, EntityType.WITHER);
        Msg.to(p, "&5Wither gespawnt &7(Test).");
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
    }

    private void spawnLuckyBlockTest(Player p) {
        if (!isInLuckyWorld(p)) { Msg.to(p, "&cIn LuckySky sein!"); return; }
        Location at = p.getLocation().toBlockLocation();
        at.getBlock().setType(Material.GOLD_BLOCK); // Platzhalter für ntdLuckyBlock
        Msg.to(p, "&6Lucky-Block (Test) platziert &7(Goldblock).");
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1f, 1.1f);
    }

    // ===================================================================
    // === HELPER ========================================================
    // ===================================================================
    private void dispatch(String cmd) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private void dispatchIn(World world, String cmd) {
        // Fallback ohne World#getKey (API-kompatibel):
        final String worldIdent = (world != null) ? world.getName() : LUCKY_WORLD_NAME;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + worldIdent + " run " + cmd);
    }

    private void sync(Runnable r) { Bukkit.getScheduler().runTask(plugin, r); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private void waitForWorldUnload(String worldName) {
        for (int i = 0; i < 20; i++) {
            if (Bukkit.getWorld(worldName) == null) return;
            sleep(500);
        }
    }

    private boolean hasMV() { return Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null; }
    private boolean isInLuckyWorld(Player p) { return p.getWorld().getName().equalsIgnoreCase(LUCKY_WORLD_NAME); }

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
        Msg.to(p, "&bLucky-Variante: &f" + next);
    }

    private void teleportToSpawn(Player p) {
        WorldsConfig.LuckyWorld cfg = plugin.configs().worlds().luckySky();
        World w = Worlds.require(cfg.worldName());
        WorldsConfig.Spawn spawn = cfg.spawn();
        p.teleport(new Location(w, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
        Msg.to(p, "&dTeleportiert.");
    }
}
