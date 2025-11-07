package de.opalium.luckysky.gui.editor;

import java.util.Objects;

public class ArenaEditorSession {
    private String arenaId;
    private String variantId;

    public ArenaEditorSession(String arenaId, String variantId) {
        this.arenaId = arenaId;
        this.variantId = variantId;
    }

    public String arenaId() {
        return arenaId;
    }

    public String variantId() {
        return variantId;
    }

    public void setArena(String arenaId, String variantId) {
        this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
        this.variantId = Objects.requireNonNull(variantId, "variantId");
    }
}
