diff --git a/docs/wiki/README.md b/docs/wiki/README.md
index c841c6563d23e351eb8136cd796f2d12e0dad4a2..2f2699b9a8fb8c6ac8cf50f58cd02a4973ecbf13 100644
--- a/docs/wiki/README.md
+++ b/docs/wiki/README.md
@@ -1,31 +1,37 @@
 ![LuckySky Banner](../images/luckysky/branding/banner/Banner-001.png)
 
 # LuckySky Wiki
 
 Diese Dokumentation fasst Betriebs- und Content-Prozesse des LuckySky-Servers zusammen. Die einzelnen Seiten decken spezialisierte Themen wie NPC-Management, grafische Bedienoberflächen, Weltrotation, PvP-Baublöcke und Fallen ab.
 
+## Konfiguration & Provisioning
+- Zentrale Blaupause: `config/luckysky.yml` bündelt Welt-Spawnpunkte, Gate-Befehle, Rundeneinstellungen, Rewards und NPC-Definitionen.
+- GUI-Layouts: Alle Admin/Player-Menüs liegen in `config/gui/` und können ohne Rebuild angepasst werden.
+- Provisioning-Workflow: Nutze das Template beim Aufsetzen neuer Instanzen und führe anschließend `/ls reload`, damit LuckySky Gate-, GUI- und World-Definitionen lädt.
+- Welt-Lifecycle & Rotation werden im Detail unter [LuckySky-Weltrotation](luckysky-weltrotation.md) beschrieben.
+
 ## Inhaltsverzeichnis
 - [NPC-Depot](npc-depot.md)
 - [Admin- und Player-GUIs](admin-player-guis.md)
 - [LuckySky-Weltrotation](luckysky-weltrotation.md)
 - [Duels Crystal PvP Builder](duels-crystal-pvp-builder.md)
 - [Fallen-Handbuch](fallen-handbuch.md)
 - [Permissions & LuckPerms-Setups](permissions.md)
 
 ## Branding Assets Überblick
 
 Der neue Branding-Bereich unter `docs/images/luckysky/branding/` stellt alle grafischen Ressourcen für Marketing, GUI und Wiki zentral bereit. Die folgenden Beispiele zeigen die wichtigsten Artefakte:
 
 ### Banner & Video
 
 ![LuckySky Promotional Banner](../images/luckysky/branding/banner/Banner-001.png)
 
 <video src="../images/luckysky/branding/docs/LuckySky-Catch.mp4" width="480" controls></video>
 
 ### Logo-Varianten
 
 <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
   <img src="../images/luckysky/branding/logo/LuckySky-Logo2.png" alt="LuckySky Logo quadratisch" width="160" />
   <img src="../images/luckysky/branding/logo/LuckySky-Logo4.png" alt="LuckySky Logo breit" width="240" />
 </div>
 
