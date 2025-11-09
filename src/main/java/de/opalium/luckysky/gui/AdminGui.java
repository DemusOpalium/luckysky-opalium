package de.opalium.luckysky.gui;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AdminGui implements Listener {

    private static final String TITLE = ChatColor.DARK_AQUA + "LuckySky Admin";
    private static final int SIZE = 27;

    // Reset-Button Slots
    private static final int SLOT_CLEAR_Y101 = 8;   // TNT
    private static final int SLOT_CLEAR_FULL = 9;   // GUNPOWDER

    private final LuckySkyPlugin plugin;

    public AdminGui(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        populate(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        switch (slot) {
            case SLOT_CLEAR_Y101 -> clearY101(player);
            case SLOT_CLEAR_FULL -> clearFullField(player);
            case 10 -> startGame(player);
            case 11 -> stopToLobby(player);
            case 12 -> setDuration(player, 5);
            case 13 -> setDuration(player, 20);
            case 14 -> setDuration(player, 60);
            case 15 -> toggleTaunts(player);
            case 16 -> toggleWither(player);
            case 17 -> toggleScoreboard(player);
            case 18 -> toggleTimer(player);
            case 19 -> cycleLuckyVariant(player);
            case 20 -> softWipe(player);
            case 21 -> hardWipe(player);
            case 22 -> bindAll(player);
            case 23 -> placePlatform(player);
            case 24 -> teleportToSpawn(player);
            case 25 -> saveConfig(player);
        }
        Bukkit.getScheduler().runTask(plugin, () -> open(player));
    }

    private void populate(Inventory inv) {
        GameConfig game = plugin.configs().game();
        TrapsConfig traps = plugin.configs().traps();
        boolean running = plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING;

        inv.setItem(SLOT_CLEAR_Y101, GuiItems.tntClearPlaneY101());
        inv.setItem(SLOT_CLEAR_FULL, GuiItems.fullClear0to319());

        inv.setItem(10, GuiItems.button(Material.LIME_DYE, "&aStart Countdown",
                List.of("&7Startet das Spiel und teleportiert zur Plattform."), running));
        inv.setItem(11, GuiItems.button(Material.BARRIER, "&cStop & Lobby",
                List.of("&7Stoppt das Spiel und sendet alle zur Lobby."), false));
        inv.setItem(12, GuiItems.button(Material.CLOCK, "&eMode 5", List.of("&7Setzt Dauer auf 5 Minuten."), false));
        inv.setItem(13, GuiItems.button(Material.CLOCK, "&eMode 20", List.of("&7Setzt Dauer auf 20 Minuten."), false));
        inv.setItem(14, GuiItems.button(Material.CLOCK, "&eMode 60", List.of("&7Setzt Dauer auf 60 Minuten."), false));

        boolean taunts = traps.withers().taunts().enabled();
        boolean wither = traps.withers().enabled();
        inv.setItem(15, GuiItems.button(Material.GOAT_HORN, taunts ? "&aTaunts AN" : "&cTaunts AUS",
                List.of("&7Schaltet Wither-Taunts um."), taunts));
        inv.setItem(16, GuiItems.button(Material.WITHER_SKELETON_SKULL, wither ? "&aWither AN" : "&cWither AUS",
                List.of("&7Aktiviert/Deaktiviert Wither-Spawns."), wither));

        boolean sb = plugin.scoreboard().isEnabled();
        boolean timer = plugin.scoreboard().isTimerVisible();
        inv.setItem(17, GuiItems.button(Material.OAK_SIGN, sb ? "&aScoreboard AN" : "&cScoreboard AUS",
                List.of("&7Schaltet das LuckySky-Scoreboard."), sb));
        inv.setItem(18, GuiItems.button(Material.COMPASS, timer ? "&aTimer sichtbar" : "&cTimer versteckt",
                List.of("&7Blendt den Timer im Scoreboard ein/aus."), timer));

        inv.setItem(19, GuiItems.button(Material.SPONGE, "&bLucky-Variante",
                List.of("&7Aktuell: &f" + game.lucky().variant()), false));
        inv.setItem(20, GuiItems.button(Material.FEATHER, "&bSoft-Wipe", List.of("&7Entfernt Effekte um Lucky."), false));
        inv.setItem(21, GuiItems.button(Material.NETHERITE_SWORD, "&cHard-Wipe", List.of("&7Entfernt Entities großflächig."), false));
        inv.setItem(22, GuiItems.button(Material.RESPAWN_ANCHOR, "&bBind", List.of("&7Setzt Spawn für alle."), false));
        inv.setItem(23, GuiItems.button(Material.PRISMARINE_BRICKS, "&bPlattform", List.of("&7Baut Safe-Plattform."), false));
        inv.setItem(24, GuiItems.button(Material.ENDER_PEARL, "&dTeleport", List.of("&7Teleportiert dich zum Spawn."), false));
        inv.setItem(25, GuiItems.button(Material.NAME_TAG, "&aSave Config", List.of("&7Speichert & läd Config neu."), false));
    }

    // ====== CLEAR Y=101 (±300) – NUR GELADENE CHUNKS ======
    private void clearY101(Player p) {
        World w = Bukkit.getWorld("LuckySky");
        if (w == null) {
            Msg.to(p, "&cWelt 'LuckySky' nicht gefunden!");
            return;
        }

        Msg.to(p, "&aLösche y=101 (±300) in geladenen Chunks...");
        tellAll("Ebene y=101 wird bereinigt...", "yellow");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int radius = 300;
            int size = 32; // 2 Chunks
            int processed = 0;

            for (int x = -radius; x <= radius; x += size) {
                for (int z = -radius; z <= radius; z += size) {
                    int x1 = x, x2 = Math.min(x + size - 1, radius);
                    int z1 = z, z2 = Math.min(z + size - 1, radius);

                    // Nur wenn Chunk geladen ist
                    if (isChunkLoaded(w, x1 >> 4, z1 >> 4)) {
                        String cmd = String.format("fill %d 101 %d %d 101 %d air", x1, z1, x2, z2);
                        runCommand(cmd);
                        processed++;
                        sleep(30);
                    }
                }
            }

            runSync(() -> {
                rebuildPlatform();
                tellAll("Ebene y=101 sauber! (" + processed + " Blöcke)", "green");
                Msg.to(p, "&aFertig!");
            });
        });
    }

    // ====== CLEAR 0–319 (±300) – NUR GELADENE CHUNKS ======
    private void clearFullField(Player p) {
        World w = Bukkit.getWorld("LuckySky");
        if (w == null) {
            Msg.to(p, "&cWelt 'LuckySky' nicht gefunden!");
            return;
        }

        Msg.to(p, "&aStarte Vollbereinigung 0–319 in geladenen Chunks...");
        tellAll("Vollbereinigung läuft...", "red");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int radius = 300;
            int size = 32, height = 16;
            int total = 0, done = 0;

            // Zähle mögliche Chunks
            for (int x = -radius; x <= radius; x += size)
                for (int z = -radius; z <= radius; z += size)
                    for (int y = 0; y <= 319; y += height)
                        if (isChunkLoaded(w, x >> 4, z >> 4)) total++;

            for (int x = -radius; x <= radius; x += size) {
                for (int z = -radius; z <= radius; z += size) {
                    int cx = x >> 4, cz = z >> 4;
                    if (!isChunkLoaded(w, cx, cz)) continue;

                    for (int y = 0; y <= 319; y += height) {
                        int x1 = x, x2 = Math.min(x + size - 1, radius);
                        int z1 = z, z2 = Math.min(z + size - 1, radius);
                        int y1 = y, y2 = Math.min(y + height - 1, 319);
                        String cmd = String.format("fill %d %d %d %d %d %d air", x1, y1, z1, x2, y2, z2);
                        runCommand(cmd);
                        done++;
                        if (done % 30 == 0) {
                            int pct = total > 0 ? done * 100 / total : 100;
                            runSync(() -> tellAll("Wipe: " + pct + "%", "gold"));
                        }
                        sleep(50);
                    }
                }
            }

            runSync(() -> {
                rebuildPlatform();
                tellAll("Spielfeld 100% sauber!", "dark_green");
                Msg.to(p, "&aVollbereinigung abgeschlossen!");
            });
        });
    }

    // ====== POD EST WIEDERHERSTELLEN (0 101 0) ======
    private void rebuildPlatform() {
        String[] blocks = {
            "setblock 0 100 -1 prismarine_stairs[facing=south,half=bottom,shape=straight]",
            "setblock 0 100 0 prismarine_stairs[facing=south,half=bottom,shape=straight]",
            "setblock 0 100 1 prismarine_bricks",
            "setblock 0 100 2 prismarine_bricks"
        };
        for (String cmd : blocks) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    // ====== HELFER-METHODEN ======
    private boolean isChunkLoaded(World world, int chunkX, int chunkZ) {
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    private void runCommand(String cmd) {
        Bukkit.getScheduler().runCommand(plugin, Bukkit.getConsoleSender(), cmd);
    }

    private void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void tellAll(String text, String color) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
            "tellraw @a {\"text\":\"" + text + "\",\"color\":\"" + color + "\"}"
        );
    }

    // ====== BESTEHENDE FUNKTIONEN (unverändert) ======
    private void startGame(Player p) { plugin.game().start(); Msg.to(p, "&aCountdown gestartet."); }
    private void stopToLobby(Player p) { plugin.game().stop(); Msg.to(p, "&eGame gestoppt & Lobby."); }
    private void setDuration(Player p, int m) { plugin.game().setDurationMinutes(m); Msg.to(p, "&aDauer: " + m + " Min."); }
    private void toggleTaunts(Player p) {
        TrapsConfig t = plugin.configs().traps();
        boolean v = !t.withers().taunts().enabled();
        plugin.configs().updateTraps(t.withTauntsEnabled(v));
        plugin.reloadSettings(); plugin.game().setTauntsEnabled(v);
        Msg.to(p, v ? "&aTaunts AN" : "&cTaunts AUS");
    }
    private void toggleWither(Player p) {
        TrapsConfig t = plugin.configs().traps();
        boolean v = !t.withers().enabled();
        plugin.configs().updateTraps(t.withWithersEnabled(v));
        plugin.reloadSettings(); plugin.game().setWitherEnabled(v);
        Msg.to(p, v ? "&aWither AN" : "&cWither AUS");
    }
    private void cycleLuckyVariant(Player p) {
        GameConfig g = plugin.configs().game();
        List<String> vars = g.lucky().variantsAvailable();
        String cur = g.lucky().variant();
        int i = vars.indexOf(cur), nextI = vars.isEmpty() ? 0 : (i + 1) % vars.size();
        String next = vars.isEmpty() ? cur : vars.get(nextI);
        plugin.configs().updateGame(g.withLuckyVariant(next));
        plugin.reloadSettings();
        Msg.to(p, "&bLucky-Variante: &f" + next);
    }
    private void toggleScoreboard(Player p) {
        boolean v = !plugin.scoreboard().isEnabled();
        plugin.scoreboard().setEnabled(v);
        Msg.to(p, v ? "&aScoreboard AN" : "&cScoreboard AUS");
    }
    private void toggleTimer(Player p) {
        boolean v = !plugin.scoreboard().isTimerVisible();
        plugin.scoreboard().setTimerVisible(v);
        Msg.to(p, v ? "&aTimer sichtbar" : "&cTimer versteckt");
    }
    private void softWipe(Player p) { Msg.to(p, "&7Soft-Wipe: &f" + plugin.game().softClear()); }
    private void hardWipe(Player p) { Msg.to(p, "&7Hard-Wipe: &f" + plugin.game().hardClear()); }
    private void bindAll(Player p) { plugin.game().bindAll(); Msg.to(p, "&bAlle gebunden."); }
    private void placePlatform(Player p) { plugin.game().placePlatform(); Msg.to(p, "&bPlattform gesetzt."); }
    private void teleportToSpawn(Player p) {
        WorldsConfig.LuckyWorld cfg = plugin.configs().worlds().luckySky();
        World w = Worlds.require(cfg.worldName());
        WorldsConfig.Spawn spawn = cfg.spawn();
        p.teleport(new Location(w, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
        Msg.to(p, "&dTeleportiert.");
    }
    private void saveConfig(Player p) {
        plugin.configs().saveAll();
        plugin.reloadSettings();
        Msg.to(p, "&aConfig gespeichert.");
    }
}
