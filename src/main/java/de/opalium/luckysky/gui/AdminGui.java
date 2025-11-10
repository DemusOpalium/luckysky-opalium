package de.opalium.luckysky.gui;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.game.GameState;
import de.opalium.luckysky.game.PortalService;
import de.opalium.luckysky.game.WitherService;
import de.opalium.luckysky.gui.layout.AdminGuiLayout;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;

public class AdminGui implements Listener {
    private static final String LUCKY_WORLD_NAME = "LuckySky";

    private final LuckySkyPlugin plugin;
    private final Map<UUID, String> openMenus = new ConcurrentHashMap<>();
    private AdminGuiLayout layout;

    public AdminGui(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.layout = AdminGuiLayout.load(plugin);
    }

    public void open(Player player) {
        open(player, layout != null ? layout.defaultMenu() : "main");
    }

    public void open(Player player, String menuId) {
        if (layout == null) {
            reload();
        }
        AdminGuiLayout.Menu menu = layout.menu(menuId).orElse(null);
        if (menu == null) {
            Msg.to(player, "&cMenü nicht gefunden: &f" + menuId);
            return;
        }
        if (menu.permission() != null
                && !player.hasPermission(menu.permission())
                && !player.hasPermission("opalium.luckysky.admin")) {
            Msg.to(player, "&cDazu fehlt dir die Berechtigung.");
            return;
        }
        Inventory inv = Bukkit.createInventory(player, menu.size(), color(menu.title()));
        ItemStack filler = createItem(menu.filler(), Map.of(), false);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler.clone());
        }
        boolean running = plugin.game().state() == GameState.RUNNING;
        for (Map.Entry<Integer, String> entry : menu.slotMapping().entrySet()) {
            AdminGuiLayout.Button template = layout.button(entry.getValue());
            if (template == null || template.display() == null) {
                continue;
            }
            if (template.onlyWhenRunning() && !running) {
                continue;
            }
            if (template.onlyWhenIdle() && running) {
                continue;
            }
            boolean locked = template.lockWhenRunning() && running;
            if (locked && template.hideWhenLocked()) {
                continue;
            }
            Map<String, String> placeholders = placeholdersFor(player, template);
            ItemStack item = createItem(template.display(), placeholders, locked);
            inv.setItem(entry.getKey(), item);
        }
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), menu.id());
    }

    public List<String> menuIds() {
        if (layout == null) {
            reload();
        }
        return new ArrayList<>(layout.menuIds());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String menuId = openMenus.get(player.getUniqueId());
        if (menuId == null) {
            return;
        }
        AdminGuiLayout.Menu menu = layout.menu(menuId).orElse(null);
        if (menu == null) {
            return;
        }
        if (!Objects.equals(color(menu.title()), event.getView().getTitle())) {
            return;
        }
        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        String buttonId = menu.slotMapping().get(slot);
        if (buttonId == null) {
            return;
        }
        AdminGuiLayout.Button template = layout.button(buttonId);
        if (template == null) {
            return;
        }
        if (template.permission() != null
                && !player.hasPermission(template.permission())
                && !player.hasPermission("opalium.luckysky.admin")) {
            Msg.to(player, "&cKeine Berechtigung für diesen Button.");
            return;
        }
        boolean running = plugin.game().state() == GameState.RUNNING;
        if (template.onlyWhenRunning() && !running) {
            Msg.to(player, "&7Nur während eines laufenden Spiels verfügbar.");
            return;
        }
        if (template.onlyWhenIdle() && running) {
            Msg.to(player, "&7Nur im Leerlauf verfügbar.");
            return;
        }
        if (template.lockWhenRunning() && running) {
            Msg.to(player, "&8Während eines Spiels gesperrt.");
            return;
        }
        switch (template.action()) {
            case AdminGuiLayout.Action.BuiltinAction builtin -> {
                executeBuiltin(player, builtin.builtin(), builtin.argument());
            }
            case AdminGuiLayout.Action.CommandAction command -> {
                executeCommands(player, command);
            }
            case AdminGuiLayout.Action.OpenMenuAction open -> {
                if (open.targetMenu() == null) {
                    Msg.to(player, "&cZiel-Menü nicht definiert.");
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> open(player, open.targetMenu()));
                return;
            }
        }
        if (template.closeAfterClick()) {
            player.closeInventory();
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> open(player, menuId));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openMenus.remove(event.getPlayer().getUniqueId());
    }

    // =====================================================================
    // === BUILTIN ACTIONS =================================================
    // =====================================================================

    private void executeBuiltin(Player player, AdminGuiLayout.Builtin builtin, String argument) {
        switch (builtin) {
            case SAVE_BASE -> saveBaseSnapshot(player);
            case LOAD_BASE -> restoreBaseSnapshot(player);
            case LOAD_SCHEMATIC -> loadSchematic(player);
            case PASTE_SCHEMATIC -> pasteSchematic(player);
            case TOGGLE_WITHER -> toggleWither(player);
            case TOGGLE_TAUNTS -> toggleTaunts(player);
            case START_DURATION -> startWithDuration(player, parseMinutes(argument));
            case STOP_GAME -> stopGame(player);
            case TOGGLE_SCOREBOARD -> toggleScoreboard(player);
            case TOGGLE_TIMER -> toggleTimer(player);
            case SOFT_WIPE -> softWipe(player);
            case HARD_WIPE -> hardWipe(player);
            case BIND_ALL -> bindAll(player);
            case PLACE_PLATFORM -> placePlatform(player, false);
            case PLACE_PLATFORM_EXTENDED -> placePlatform(player, true);
            case TELEPORT_TO_SPAWN -> teleportToSpawn(player);
            case SAVE_CONFIG -> saveConfig(player);
            case CYCLE_VARIANT -> cycleVariant(player);
            case SPAWN_WITHER_NOW -> spawnWither(player);
            case RELOAD_PLUGIN -> reloadPlugin(player);
            case TRIGGER_REWARD_WIN -> triggerRewardWin(player);
            case TRIGGER_REWARD_FAIL -> triggerRewardFail(player);
            case OPEN_PORTAL -> openPortal(player);
            case CLOSE_PORTAL -> closePortal(player);
            default -> Msg.to(player, "&cAktion nicht implementiert.");
        }
    }

    private void stopGame(Player player) {
        plugin.game().stop();
        Msg.to(player, "&eGame gestoppt & Lobby.");
    }

    private void toggleScoreboard(Player player) {
        boolean enabled = !plugin.scoreboard().isEnabled();
        plugin.scoreboard().setEnabled(enabled);
        plugin.scoreboard().refresh();
        Msg.to(player, enabled ? "&aScoreboard aktiviert." : "&cScoreboard deaktiviert.");
    }

    private void toggleTimer(Player player) {
        boolean visible = !plugin.scoreboard().isTimerVisible();
        plugin.scoreboard().setTimerVisible(visible);
        plugin.scoreboard().refresh();
        Msg.to(player, visible ? "&aTimer eingeblendet." : "&cTimer ausgeblendet.");
    }

    private void softWipe(Player player) {
        int count = plugin.game().softClear();
        Msg.to(player, "&7Soft-Wipe entfernt &f" + count + " &7Entities.");
    }

    private void hardWipe(Player player) {
        int count = plugin.game().hardClear();
        Msg.to(player, "&7Hard-Wipe entfernt &f" + count + " &7Entities.");
    }

    private void bindAll(CommandSender sender) {
        plugin.game().bindAll(sender);
    }

    private void placePlatform(Player player, boolean extended) {
        if (extended) {
            plugin.game().placePlatformExtended();
            Msg.to(player, "&bPlattform 3x3 angewendet.");
        } else {
            plugin.game().placePlatform();
            Msg.to(player, "&bPlattform gesetzt.");
        }
    }

    private void teleportToSpawn(Player player) {
        WorldsConfig.LuckyWorld cfg = plugin.configs().worlds().luckySky();
        WorldsConfig.Spawn spawn = cfg.spawn();
        if (spawn == null) {
            Msg.to(player, "&cKein Spawn definiert.");
            return;
        }
        World world = Worlds.require(cfg.worldName());
        player.teleport(new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
        Msg.to(player, "&dTeleportiert.");
    }

    private void saveConfig(Player player) {
        plugin.configs().saveAll();
        plugin.reloadSettings();
        Msg.to(player, "&aConfig gespeichert & neu geladen.");
    }

    private void cycleVariant(Player player) {
        GameConfig config = plugin.configs().game();
        List<String> variants = config.lucky().variantsAvailable();
        if (variants.isEmpty()) {
            Msg.to(player, "&cKeine Varianten verfügbar.");
            return;
        }
        String current = config.lucky().variant();
        int index = variants.indexOf(current);
        String next = variants.get((index + 1) % variants.size());
        plugin.configs().updateGame(config.withLuckyVariant(next));
        plugin.reloadSettings();
        Msg.to(player, "&bLucky-Variante: &f" + next);
    }

    private void spawnWither(Player player) {
        WitherService.SpawnRequestResult result = plugin.game().spawnWitherNow();
        switch (result) {
            case ACCEPTED -> Msg.to(player, "&dWither-Spawn ausgelöst.");
            case WITHER_DISABLED -> Msg.to(player, "&cWither-Spawns sind deaktiviert.");
            case GAME_NOT_RUNNING -> Msg.to(player, "&eLuckySky läuft derzeit nicht.");
            case FAILED -> Msg.to(player, "&cWither-Spawn fehlgeschlagen (Welt/Regeln prüfen).");
        }
    }

    private void reloadPlugin(Player player) {
        plugin.reloadSettings();
        Msg.to(player, "&aLuckySky neu geladen.");
    }

    private void triggerRewardWin(Player player) {
        plugin.game().triggerRewardsWin(player);
        Msg.to(player, "&aReward (Win) ausgeführt.");
    }

    private void triggerRewardFail(Player player) {
        plugin.game().triggerRewardsFail();
        Msg.to(player, "&cReward (Fail) ausgeführt.");
    }

    private void openPortal(Player player) {
        PortalService.openBackspawn(player);
        Msg.to(player, "&bPortal geöffnet.");
    }

    private void closePortal(Player player) {
        PortalService.closeBackspawn(player);
        Msg.to(player, "&cPortal entfernt.");
    }

    private void startWithDuration(Player player, int minutes) {
        plugin.game().setDurationMinutes(minutes);
        plugin.game().start();
        Msg.to(player, "&aStart mit Dauer: &f" + minutes + " Minuten.");
    }

    private int parseMinutes(String argument) {
        try {
            return Math.max(1, Integer.parseInt(argument));
        } catch (NumberFormatException ex) {
            return plugin.configs().game().durations().minutesDefault();
        }
    }

    private void toggleTaunts(Player player) {
        TrapsConfig traps = plugin.configs().traps();
        boolean enabled = !traps.withers().taunts().enabled();
        plugin.configs().updateTraps(traps.withTauntsEnabled(enabled));
        plugin.reloadSettings();
        plugin.game().setTauntsEnabled(enabled);
        Msg.to(player, enabled ? "&aTaunts aktiviert." : "&cTaunts deaktiviert.");
    }

    private void toggleWither(Player player) {
        TrapsConfig traps = plugin.configs().traps();
        boolean enabled = !traps.withers().enabled();
        plugin.configs().updateTraps(traps.withWithersEnabled(enabled));
        plugin.reloadSettings();
        plugin.game().setWitherEnabled(enabled);
        Msg.to(player, enabled ? "&aWither aktiviert." : "&cWither deaktiviert.");
    }

    // =====================================================================
    // === BASE SNAPSHOTS ==================================================
    // =====================================================================

    private Path templatesDir() {
        return plugin.getDataFolder().toPath().resolve("templates");
    }

    private Path baseZip() {
        return templatesDir().resolve("LuckySkyBase.zip");
    }

    private Path worldContainer() {
        return Bukkit.getWorldContainer().toPath();
    }

    private Path luckyDir() {
        return worldContainer().resolve(LUCKY_WORLD_NAME);
    }

    private void saveBaseSnapshot(Player player) {
        World world = Bukkit.getWorld(LUCKY_WORLD_NAME);
        if (world == null) {
            Msg.to(player, "&cLuckySky-Welt nicht gefunden.");
            return;
        }
        Msg.to(player, "&eSichere LuckySky → Base.zip ...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sync(() -> Bukkit.unloadWorld(world, true));
            try {
                Files.createDirectories(templatesDir());
                try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(baseZip()))) {
                    Files.walk(luckyDir()).forEach(path -> {
                        if (Files.isDirectory(path)) {
                            return;
                        }
                        Path relative = luckyDir().relativize(path);
                        String name = relative.toString().replace('\\', '/');
                        if (name.startsWith("playerdata") || name.equals("session.lock")) {
                            return;
                        }
                        try {
                            zip.putNextEntry(new ZipEntry(name));
                            Files.copy(path, zip);
                            zip.closeEntry();
                        } catch (IOException ignored) {
                        }
                    });
                }
                sync(() -> Msg.to(player, "&aBase gesichert: &f" + relativeBasePath()));
            } catch (IOException ex) {
                sync(() -> Msg.to(player, "&cFehler beim Sichern: &f" + ex.getClass().getSimpleName()));
            } finally {
                sync(() -> Bukkit.createWorld(new WorldCreator(LUCKY_WORLD_NAME)));
            }
        });
    }

    private void restoreBaseSnapshot(Player player) {
        if (!Files.exists(baseZip())) {
            Msg.to(player, "&cKein Snapshot gefunden: &f" + relativeBasePath());
            return;
        }
        if (!hasMV()) {
            Msg.to(player, "&cMultiverse-Core wird benötigt.");
            return;
        }
        Msg.to(player, "&eStelle Base wieder her...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sync(() -> plugin.game().teleportAllToLobby());
            sync(() -> {
                if (Bukkit.getWorld(LUCKY_WORLD_NAME) != null) {
                    dispatch("mv remove " + LUCKY_WORLD_NAME);
                }
            });
            sleep(500);
            try {
                if (Files.exists(luckyDir())) {
                    deleteRecursively(luckyDir());
                }
                try (ZipInputStream in = new ZipInputStream(Files.newInputStream(baseZip()))) {
                    ZipEntry entry;
                    while ((entry = in.getNextEntry()) != null) {
                        Path out = luckyDir().resolve(entry.getName()).normalize();
                        if (!out.startsWith(luckyDir())) {
                            continue;
                        }
                        if (entry.isDirectory()) {
                            Files.createDirectories(out);
                        } else {
                            Files.createDirectories(out.getParent());
                            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                        }
                        in.closeEntry();
                    }
                }
            } catch (IOException ex) {
                sync(() -> Msg.to(player, "&cFehler beim Entpacken: &f" + ex.getClass().getSimpleName()));
                return;
            }
            sync(() -> {
                dispatch("mv import " + LUCKY_WORLD_NAME + " NORMAL");
                dispatch("mv confirm");
            });
            sleep(1500);
            sync(() -> {
                World newWorld = Bukkit.getWorld(LUCKY_WORLD_NAME);
                if (newWorld != null) {
                    newWorld.setSpawnLocation(0, 101, 0);
                    plugin.game().placePlatform();
                    Msg.to(player, "&aBase geladen & Plattform gesetzt.");
                } else {
                    Msg.to(player, "&cWelt nach Import nicht geladen.");
                }
            });
        });
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walk(dir)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
    }

    // =====================================================================
    // === SCHEMATIC HANDLING =============================================
    // =====================================================================

    private Path schemDir() {
        return plugin.getDataFolder().toPath().resolve("schematics");
    }

    private void loadSchematic(Player player) {
        if (!ensureLucky(player)) {
            return;
        }
        try {
            Files.createDirectories(schemDir());
        } catch (IOException ignored) {
        }
        Path worldEditDir = plugin.getDataFolder().toPath().getParent().resolve("WorldEdit").resolve("schematics");
        try {
            Files.createDirectories(worldEditDir);
        } catch (IOException ignored) {
        }
        Path src = schemDir().resolve("platform.schem");
        Path dst = worldEditDir.resolve("platform.schem");
        if (!Files.exists(src)) {
            Msg.to(player, "&cplatform.schem fehlt unter &f" + plugin.getDataFolder().toPath().relativize(src));
            return;
        }
        try {
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Msg.to(player, "&cKonnte Schematic nicht kopieren: &f" + ex.getClass().getSimpleName());
            return;
        }
        player.performCommand("//schem load platform");
        Msg.to(player, "&aSchematic geladen.");
    }

    private void pasteSchematic(Player player) {
        if (!ensureLucky(player)) {
            return;
        }
        player.performCommand("//paste");
        Msg.to(player, "&aSchematic gepastet.");
    }

    private boolean ensureLucky(Player player) {
        if (!player.getWorld().getName().equalsIgnoreCase(LUCKY_WORLD_NAME)) {
            Msg.to(player, "&cBitte in LuckySky ausführen.");
            return false;
        }
        return true;
    }

    // =====================================================================
    // === COMMAND HANDLING ===============================================
    // =====================================================================

    private void executeCommands(Player player, AdminGuiLayout.Action.CommandAction command) {
        if (command.commands().isEmpty()) {
            plugin.getLogger().warning("[LuckySky] Command button executed without any commands configured.");
            Msg.to(player, "&cFür diese Aktion sind keine Befehle konfiguriert. Bitte admin-gui.yml prüfen.");
            return;
        }
        for (String raw : command.commands()) {
            String cmd = raw.replace("{player}", player.getName()).replace("%player%", player.getName());
            switch (command.executor()) {
                case PLAYER -> player.performCommand(cmd);
                case BOTH -> {
                    player.performCommand(cmd);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
        Msg.to(player, "&aAktion ausgeführt.");
    }

    // =====================================================================
    // === PLACEHOLDERS ====================================================
    // =====================================================================

    private Map<String, String> placeholdersFor(Player viewer, AdminGuiLayout.Button button) {
        Map<String, String> map = new HashMap<>();
        map.put("player", viewer.getName());
        map.put("state", plugin.game().state() == GameState.RUNNING ? "&aLäuft" : "&cGestoppt");
        map.put("path", relativeBasePath());
        switch (button.action()) {
            case AdminGuiLayout.Action.BuiltinAction builtin -> {
                switch (builtin.builtin()) {
                    case LOAD_BASE -> map.put("status", Files.exists(baseZip()) ? "&aVorhanden" : "&cFehlt");
                    case START_DURATION -> {
                        int minutes = parseMinutes(builtin.argument());
                        map.put("minutes", String.valueOf(minutes));
                    }
                    case TOGGLE_WITHER -> {
                        boolean enabled = plugin.configs().traps().withers().enabled();
                        map.put("status", enabled ? "&aAN" : "&cAUS");
                    }
                    case TOGGLE_TAUNTS -> {
                        boolean enabled = plugin.configs().traps().withers().taunts().enabled();
                        map.put("status", enabled ? "&aAN" : "&cAUS");
                    }
                    case TOGGLE_SCOREBOARD -> {
                        map.put("status", plugin.scoreboard().isEnabled() ? "&aAN" : "&cAUS");
                    }
                    case TOGGLE_TIMER -> {
                        map.put("status", plugin.scoreboard().isTimerVisible() ? "&aSichtbar" : "&cVersteckt");
                    }
                    case BIND_ALL -> {
                        GameConfig.Spawns spawns = plugin.configs().game().spawns();
                        map.put("warning", spawns.warning());
                        map.put("allowed", spawns.allowBinding() ? "&aErlaubt" : "&cDeaktiviert");
                    }
                    case CYCLE_VARIANT -> map.put("variant", plugin.configs().game().lucky().variant());
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
        return map;
    }

    private String relativeBasePath() {
        Path data = plugin.getDataFolder().toPath();
        Path path = baseZip();
        if (path.startsWith(data)) {
            return data.relativize(path).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private ItemStack createItem(AdminGuiLayout.ButtonDisplay display, Map<String, String> placeholders, boolean locked) {
        ItemStack stack = new ItemStack(display.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = applyPlaceholders(display.name(), placeholders, locked);
            if (!name.isEmpty()) {
                meta.setDisplayName(color(name));
            }
            List<String> lore = new ArrayList<>();
            for (String line : display.lore()) {
                lore.add(color(applyPlaceholders(line, placeholders, locked)));
            }
            if (locked) {
                lore.add(color("&8Während eines laufenden Spiels gesperrt."));
            }
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            if (display.glow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String applyPlaceholders(String input, Map<String, String> placeholders, boolean locked) {
        if (input == null) {
            return "";
        }
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        if (locked) {
            result = result.replace("{status}", "&cGesperrt");
        }
        return result;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    // =====================================================================
    // === UTILITIES =======================================================
    // =====================================================================

    private void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private void dispatch(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private boolean hasMV() {
        Plugin mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        return mv != null && mv.isEnabled();
    }
}
