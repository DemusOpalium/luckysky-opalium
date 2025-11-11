package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class ScoreboardService {
    private static final String OBJECTIVE_NAME = "luckysky";

    private final LuckySkyPlugin plugin;

    private boolean enabled;
    private boolean timerVisible;
    private boolean timerRunning;
    private int ticksRemaining;

    private GameConfig.Scoreboard config;
    private Scoreboard board;
    private Objective objective;
    private List<String> lastEntries = List.of();

    public ScoreboardService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        GameConfig.Scoreboard cfg = plugin.configs().game().scoreboard();
        if (cfg == null) {
            cfg = new GameConfig.Scoreboard(false, "&bLuckySky", List.of());
        }
        this.config = cfg;
        this.enabled = cfg.enabled();
        this.timerVisible = true;
        refresh();
    }

    public void shutdown() {
        clearScoreboard();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        refresh();
    }

    public boolean isTimerVisible() {
        return timerVisible;
    }

    public void setTimerVisible(boolean timerVisible) {
        this.timerVisible = timerVisible;
        refresh();
    }

    public void onTimerStart(int ticks) {
        this.timerRunning = true;
        this.ticksRemaining = ticks;
        refresh();
    }

    public void onTimerTick(int ticks) {
        if (!timerRunning) {
            return;
        }
        this.ticksRemaining = ticks;
        if (timerVisible) {
            refresh();
        }
    }

    public void onTimerStop() {
        this.timerRunning = false;
        this.ticksRemaining = 0;
        refresh();
    }

    public void refresh() {
        if (!enabled || config == null || config.lines().isEmpty()) {
            clearScoreboard();
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        ensureObjective(manager);
        if (objective != null) {
            objective.setDisplayName(Msg.color(config.title()));
        }
        List<String> rendered = renderLines();
        List<String> entries = new ArrayList<>();
        int score = rendered.size();
        for (int i = 0; i < rendered.size(); i++) {
            String line = ensureUnique(rendered.get(i), i);
            entries.add(line);
            objective.getScore(line).setScore(score - i);
        }
        removeMissing(entries);
        lastEntries = entries;
        assignToPlayers(manager);
    }

    public void attachAll() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        if (!enabled || config == null || config.lines().isEmpty()) {
            clearScoreboard(manager);
            return;
        }
        if (board == null || objective == null) {
            refresh();
            return;
        }
        assignToPlayers(manager);
    }

    private void ensureObjective(ScoreboardManager manager) {
        if (board != null && objective != null) {
            return;
        }
        board = manager.getNewScoreboard();
        objective = board.registerNewObjective(OBJECTIVE_NAME, "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(Msg.color(config.title()));
    }

    private void removeMissing(List<String> entries) {
        if (board == null || lastEntries.isEmpty()) {
            return;
        }
        Set<String> current = new HashSet<>(entries);
        for (String previous : lastEntries) {
            if (!current.contains(previous)) {
                board.resetScores(previous);
            }
        }
    }

    private void assignToPlayers(ScoreboardManager manager) {
        if (board == null) {
            return;
        }
        Scoreboard main = manager.getMainScoreboard();
        World target = luckyWorld();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (target != null && player.getWorld().equals(target)) {
                if (player.getScoreboard() != board) {
                    player.setScoreboard(board);
                }
            } else if (player.getScoreboard() == board) {
                player.setScoreboard(main);
            }
        }
    }

    private void clearScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        clearScoreboard(manager);
    }

    private void clearScoreboard(ScoreboardManager manager) {
        if (board != null) {
            Scoreboard main = manager.getMainScoreboard();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getScoreboard() == board) {
                    player.setScoreboard(main);
                }
            }
        }
        board = null;
        objective = null;
        lastEntries = List.of();
    }

    private List<String> renderLines() {
        List<String> lines = new ArrayList<>();
        if (config == null) {
            return lines;
        }
        int limit = Math.min(15, config.lines().size());
        for (int i = 0; i < limit; i++) {
            String template = config.lines().get(i);
            String replaced = replacePlaceholders(template);
            lines.add(Msg.color(replaced));
        }
        return lines;
    }

    private String replacePlaceholders(String template) {
        String result = template;
        result = result.replace("{timer}", formatTimer());
        result = result.replace("{state}", formatState());
        result = result.replace("{players}", formatPlayers());
        result = result.replace("{players_active}", String.valueOf(activePlayers()));
        result = result.replace("{players_total}", String.valueOf(totalPlayers()));
        result = result.replace("{wither}", formatWither());
        return result;
    }

    private String ensureUnique(String line, int index) {
        if (line.length() > 40) {
            line = line.substring(0, 40);
        }
        ChatColor[] colors = ChatColor.values();
        ChatColor suffix = colors[index % colors.length];
        String entry = line + suffix;
        if (entry.length() > 40) {
            entry = entry.substring(0, 40);
        }
        return entry;
    }

    private String formatTimer() {
        if (!timerVisible) {
            return "aus";
        }
        if (!timerRunning) {
            return "--:--";
        }
        int seconds = Math.max(ticksRemaining, 0) / 20;
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, secs);
    }

    private String formatState() {
        GameManager game = plugin.game();
        if (game == null) {
            return "&cOffline";
        }
        return switch (game.state()) {
            case RUN -> "&aLaufend";
            case COUNTDOWN -> "&eCountdown";
            case LOBBY -> "&bLobby";
            case ENDING -> "&6Endphase";
            case RESETTING -> "&cReset";
            case IDLE -> "&7Bereit";
        };
    }

    private String formatPlayers() {
        int active = activePlayers();
        int total = totalPlayers();
        return active + "/" + total;
    }

    private int activePlayers() {
        GameManager game = plugin.game();
        if (game == null) {
            return 0;
        }
        return game.activeParticipants().size();
    }

    private int totalPlayers() {
        GameManager game = plugin.game();
        if (game == null) {
            return 0;
        }
        return game.allParticipants().size();
    }

    private String formatWither() {
        if (!plugin.configs().traps().withers().enabled()) {
            return "&cAus";
        }
        GameManager game = plugin.game();
        if (game == null) {
            return "&eBereit";
        }
        GameState state = game.state();
        if (state != GameState.RUN && state != GameState.COUNTDOWN) {
            return "&eBereit";
        }
        World world = luckyWorld();
        if (world == null) {
            return "&eBereit";
        }
        boolean active = !world.getEntitiesByClass(Wither.class).isEmpty();
        return active ? "&cAktiv" : "&aWartet";
    }

    private World luckyWorld() {
        WorldsConfig.LuckyWorld luckyWorld = plugin.configs().worlds().luckySky();
        if (luckyWorld == null) {
            return null;
        }
        return plugin.getServer().getWorld(luckyWorld.worldName());
    }
}
