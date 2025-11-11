package de.opalium.luckysky.gui;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.game.GameState;
import de.opalium.luckysky.game.ScoreboardService;
import de.opalium.luckysky.gui.layout.PlayerGuiLayout;
import de.opalium.luckysky.util.Msg;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerGui implements Listener {
    private final LuckySkyPlugin plugin;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private PlayerGuiLayout layout;

    public PlayerGui(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.layout = PlayerGuiLayout.load(plugin);
    }

    public void open(Player player) {
        if (layout == null) {
            reload();
        }
        String title = Msg.color(layout.title());
        Inventory inventory = Bukkit.createInventory(player, layout.size(), title);
        ItemStack filler = layout.filler().render(Map.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }
        for (PlayerGuiLayout.Button button : layout.buttons()) {
            if (button.slot() < 0 || button.slot() >= inventory.getSize()) {
                continue;
            }
            Map<String, String> placeholders = placeholdersFor(player, button);
            inventory.setItem(button.slot(), button.display().render(placeholders));
        }
        player.openInventory(inventory);
        viewers.add(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (layout == null) {
            reload();
        }
        if (!viewers.contains(player.getUniqueId())) {
            return;
        }
        if (!event.getView().getTitle().equals(Msg.color(layout.title()))) {
            return;
        }
        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        layout.buttonAt(slot).ifPresent(button -> handleClick(player, button));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        viewers.remove(event.getPlayer().getUniqueId());
    }

    private Map<String, String> placeholdersFor(Player player, PlayerGuiLayout.Button button) {
        if (layout == null) {
            reload();
        }
        Map<String, String> placeholders = new HashMap<>(button.placeholders());
        GameManager game = plugin.game();
        ScoreboardService scoreboard = plugin.scoreboard();
        boolean running = game.state() == GameState.RUNNING;
        boolean inLuckyWorld = isInLuckyWorld(player);
        switch (button.action()) {
            case JOIN -> {
                placeholders.putIfAbsent("status", inLuckyWorld ? "&aArena" : "&eLobby");
            }
            case LEAVE -> {
                placeholders.putIfAbsent("status", inLuckyWorld ? "&aArena" : "&eLobby");
            }
            case START -> {
                placeholders.putIfAbsent("minutes", button.argument());
                placeholders.put("state", running ? "&cLäuft" : "&aBereit");
            }
            case LOBBY -> {
                placeholders.putIfAbsent("status", game.lobbySpawnLocation().isPresent() ? "&aVerfügbar" : "&cFehlt");
            }
            case RULES -> {
                placeholders.putIfAbsent("status", "&bInfo");
            }
            case SCOREBOARD -> {
                placeholders.put("status", scoreboard != null && scoreboard.isEnabled() ? "&aAN" : "&cAUS");
            }
            default -> {
            }
        }
        return placeholders;
    }

    private boolean isInLuckyWorld(Player player) {
        World world = player.getWorld();
        String target = plugin.configs().worlds().luckySky().worldName();
        return world.getName().equalsIgnoreCase(target);
    }

    private void handleClick(Player player, PlayerGuiLayout.Button button) {
        switch (button.action()) {
            case JOIN -> {
                joinLuckySky(player);
                player.closeInventory();
            }
            case LEAVE -> {
                leaveLuckySky(player);
                player.closeInventory();
            }
            case START -> {
                startGame(player, button.argument());
                player.closeInventory();
            }
            case LOBBY -> {
                teleportToLobby(player);
                player.closeInventory();
            }
            case RULES -> {
                showRules(player, button.messages());
            }
            case SCOREBOARD -> {
                toggleScoreboard(player);
                Bukkit.getScheduler().runTask(plugin, () -> open(player));
            }
            default -> {
            }
        }
    }

    private void joinLuckySky(Player player) {
        GameManager game = plugin.game();
        game.platformSpawnLocation().ifPresentOrElse(location -> {
            player.teleport(location);
            if (game.state() == GameState.RUNNING) {
                Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SURVIVAL));
            }
            Msg.to(player, "&aDu wurdest nach LuckySky teleportiert.");
        }, () -> Msg.to(player, "&cLuckySky ist derzeit nicht vorbereitet."));
    }

    private void leaveLuckySky(Player player) {
        GameManager game = plugin.game();
        game.teleportPlayerToLobby(player);
        if (game.oneLifeEnabled() && game.state() == GameState.RUNNING && game.isParticipant(player)) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SURVIVAL));
        }
        Msg.to(player, "&eDu hast LuckySky verlassen.");
    }

    private void teleportToLobby(Player player) {
        GameManager game = plugin.game();
        game.teleportPlayerToLobby(player);
        Msg.to(player, "&bZur LuckySky-Lobby teleportiert.");
    }

    private void startGame(Player player, String argument) {
        int minutes = parseMinutes(argument);
        if (minutes <= 0) {
            Msg.to(player, "&cUngültige Dauer.");
            return;
        }
        GameManager game = plugin.game();
        if (game.state() == GameState.RUNNING) {
            Msg.to(player, "&cLuckySky läuft bereits.");
            return;
        }
        game.setDurationMinutes(minutes);
        if (plugin.rounds() == null) {
            Msg.to(player, "&cStateMachine nicht verfügbar.");
            return;
        }
        boolean started = plugin.rounds().requestStart();
        if (started) {
            Msg.to(player, "&aLuckySky mit &f" + minutes + "&a Minuten gestartet.");
        } else {
            Msg.to(player, "&cKonnte LuckySky nicht starten.");
        }
    }

    private int parseMinutes(String argument) {
        try {
            return Integer.parseInt(argument.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void showRules(Player player, java.util.List<String> messages) {
        if (messages.isEmpty()) {
            Msg.to(player, "&7Keine Regeln konfiguriert.");
            return;
        }
        for (String line : messages) {
            Msg.to(player, line);
        }
    }

    private void toggleScoreboard(Player player) {
        ScoreboardService scoreboard = plugin.scoreboard();
        if (scoreboard == null) {
            Msg.to(player, "&cScoreboard-Service nicht verfügbar.");
            return;
        }
        boolean enabled = !scoreboard.isEnabled();
        scoreboard.setEnabled(enabled);
        scoreboard.refresh();
        Msg.to(player, enabled ? "&aScoreboard aktiviert." : "&cScoreboard deaktiviert.");
    }
}
