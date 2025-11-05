package de.opalium.luckysky;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckySkyPlugin extends JavaPlugin {

    @Override
    public void onEnable() { getLogger().info("LuckySky-Opalium enabled."); }

    @Override
    public void onDisable() { getLogger().info("LuckySky-Opalium disabled."); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("ls")) return false;
        if (!sender.hasPermission("luckysky.admin")) {
            sender.sendMessage(ChatColor.RED + "Keine Rechte.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "⛯ LuckySky: /ls plat • plat+ • reset (weitere folgen)");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "plat" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Nur im Spiel."); return true; }
                buildPlatform(p.getWorld(), p.getLocation().getBlockX(), 200, p.getLocation().getBlockZ(), 3, Material.OBSIDIAN);
                sender.sendMessage(ok("Safe platform 3×3 @Y=200 gebaut."));
            }
            case "plat+" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Nur im Spiel."); return true; }
                buildPlatform(p.getWorld(), p.getLocation().getBlockX(), 200, p.getLocation().getBlockZ(), 5, Material.OBSIDIAN);
                sender.sendMessage(ok("Safe platform 5×5 @Y=200 gebaut."));
            }
            case "reset" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Nur im Spiel."); return true; }
                int cleared = wipeEntities(p.getWorld(), p.getLocation(), 64);
                sender.sendMessage(ok("Wipe im Radius 64 – entfernte Entities: " + cleared));
            }
            default -> sender.sendMessage(ChatColor.GRAY + "Unbekannt. /ls für Hilfe.");
        }
        return true;
    }

    private void buildPlatform(World world, int cx, int y, int cz, int size, Material mat) {
        int half = size / 2;
        BlockData bd = mat.createBlockData();
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                world.getBlockAt(x, y, z).setBlockData(bd, false);
            }
        }
        // “3+1” Mittelfläche oben drauf (Geländerplatz) – optional:
        world.getBlockAt(cx, y + 1, cz).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
    }

    private int wipeEntities(World world, Location center, double radius) {
        return (int) world.getNearbyEntities(center, radius, radius, radius, e ->
                !(e instanceof Player)).stream().peek(e -> e.remove()).count();
    }

    private String ok(String msg) { return ChatColor.GREEN + "OK: " + ChatColor.WHITE + msg; }
}
