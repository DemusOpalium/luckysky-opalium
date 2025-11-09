package de.opalium.luckysky.game;

import org.bukkit.Bukkit;

public final class PortalService {
    private PortalService() {}

    private static void cmd(String c) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
    }

    /** Öffnet (oder aktualisiert) ein Backspawn-Portal nach LuckySky (0/101/2). */
    public static void openBackspawn() {
        cmd("mvp create backspawn");
        cmd("mvp select backspawn");
        cmd("mvp modify dest e:LuckySky:0,101,2");
    }

    /** Entfernt das Backspawn-Portal. */
    public static void closeBackspawn() {
        cmd("mvp remove backspawn");
    }
}
