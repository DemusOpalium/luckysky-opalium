package de.opalium.luckysky.gui;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.List;
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

public class AdminGui implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "LuckySky Admin";
    private static final int SIZE = 27;
    
    private static final int SLOT_CLEAR_PLANE_Y101 = 8;   // TNT
    private static final int SLOT_CLEAR_FIELD_FULL = 9;   // GUNPOWDER
    
    private static final String LUCKY_DIM = "minecraft:luckysky";
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
            case 10 -> plugin.game().start(); Msg.to(p, "&aCountdown gestartet.");
            case 11 -> plugin.game().stop(); Msg.to(p, "&eGame gestoppt & Lobby.");
            case 12 -> plugin.game().setDurationMinutes(5); Msg.to(p, "&aDauer: 5 Min.");
            case 13 -> plugin.game().setDurationMinutes(20); Msg.to(p, "&aDauer: 20 Min.");
            case 14 -> plugin.game().setDurationMinutes(60); Msg.to(p, "&aDauer: 60 Min.");
            case 15 -> toggleTaunts(p);
            case 16 -> toggleWither(p);
            case 17 -> plugin.scoreboard().setEnabled(!plugin.scoreboard().isEnabled());
            case 18 -> plugin.scoreboard().setTimerVisible(!plugin.scoreboard().isTimerVisible());
            case 19 -> cycleLuckyVariant(p);
            case 20 -> Msg.to(p, "&7Soft-Wipe: &f" + plugin.game().softClear());
            case 21 -> Msg.to(p, "&7Hard-Wipe: &f" + plugin.game().hardClear());
            case 22 -> plugin.game().bindAll(); Msg.to(p, "&bAlle gebunden.");
            case 23 -> plugin.game().placePlatform(); Msg.to(p, "&bPlattform gesetzt.");
            case 24 -> teleportToSpawn(p);
            case 25 -> plugin.configs().saveAll(); plugin.reloadSettings(); Msg.to(p, "&aConfig gespeichert.");
        }
        Bukkit.getScheduler().runTask(plugin, () -> open(p));
    }

    private void populate(Inventory inv) {
        GameConfig game = plugin.configs().game();
        TrapsConfig traps = plugin.configs().traps();
        boolean running = plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING;

        inv.setItem(SLOT_CLEAR_PLANE_Y101, GuiItems.tntClearPlaneY101());
        inv.setItem(SLOT_CLEAR_FIELD_FULL, GuiItems.fullClear0to319());

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

    // ====== CLEAR Y=101 (±300) – MIT execute in + FAWE + NO SPAM ======
    private void handleClearPlaneY101(Player player) {
        Msg.to(player, "&aBereinige Ebene &ey=101 &7(±300) in &bLuckySky&7...");
        dispatch("tellraw @a {\"text\":\"LuckySky: Ebene y=101 wird gereinigt...\",\"color\":\"yellow\"}");
        disableAdminLogs();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final int y = 101;
            for (int x = -300; x <= 300; x += STEP_XZ) {
                for (int z = -300; z <= 300; z += STEP_XZ) {
                    int x1 = x, x2 = Math.min(x + STEP_XZ - 1, 300);
                    int z1 = z, z2 = Math.min(z + STEP_XZ - 1, 300);
                    runFillInLuckySky(y, x1, z1, y, x2, z2);
                    sleep(25);
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.game().placePlatform();
                dispatch("tellraw @a {\"text\":\"LuckySky: Ebene y=101 gereinigt & Plattform gesetzt.\",\"color\":\"green\"}");
                Msg.to(player, "&aFertig.");
                enableAdminLogs();
            });
        });
    }

    // ====== CLEAR 0–319 (±300) – MIT execute in + FAWE + NO SPAM ======
    private void handleClearField300(Player player) {
        Msg.to(player, "&aVollbereinigung &70–319 &7(±300) in &bLuckySky&7 gestartet...");
        dispatch("tellraw @a {\"text\":\"LuckySky: Vollbereinigung läuft...\",\"color\":\"red\"}");
        disableAdminLogs();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int done = 0;
            int total = ((600 / STEP_XZ) + 1) * ((600 / STEP_XZ) + 1) * ((319 / STEP_Y) + 1);

            for (int x = -300; x <= 300; x += STEP_XZ) {
                for (int z = -300; z <= 300; z += STEP_XZ) {
                    for (int y = 0; y <= 319; y += STEP_Y) {
                        int x1 = x, x2 = Math.min(x + STEP_XZ - 1, 300);
                        int z1 = z, z2 = Math.min(z + STEP_XZ - 1, 300);
                        int y1 = y, y2 = Math.min(y + STEP_Y - 1, 319);
                        runFillInLuckySky(y1, x1, z1, y2, x2, z2);
                        done++;
                        if (done % 20 == 0) {
                            int pct = Math.min(100, done * 100 / total);
                            Bukkit.getScheduler().runTask(plugin, () ->
                                dispatch("tellraw @a [\"\",{\"text\":\"Wipe LuckySky: \"},{\"text\":\"" + pct + "%\",\"color\":\"gold\"}]"));
                        }
                        sleep(50);
                    }
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.game().placePlatform();
                dispatch("tellraw @a {\"text\":\"LuckySky: Spielfeld 0–319 vollständig gereinigt.\",\"color\":\"dark_green\"}");
                Msg.to(player, "&aVollbereinigung abgeschlossen!");
                enableAdminLogs();
            });
        });
    }

    // ====== HELPER: execute in minecraft:luckysky run fill ======
    private void runFillInLuckySky(int y1, int x1, int z1, int y2, int x2, int z2) {
        String cmd = String.format(
            "execute in %s run fill %d %d %d %d %d %d minecraft:air",
            LUCKY_DIM, x1, y1, z1, x2, y2, z2
        );
        Bukkit.getScheduler().runTask(plugin, () -> dispatch(cmd));
    }

    // ====== NO SPAM: logAdminCommands false ======
    private void disableAdminLogs() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule logAdminCommands false");
    }

    private void enableAdminLogs() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule logAdminCommands true");
    }

    private void dispatch(String cmd) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ====== BESTEHENDE FUNKTIONEN (gekürzt) ======
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
        int i = vars.indexOf(cur), nextI = (i + 1) % vars.size();
        String next = vars.get(nextI);
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
