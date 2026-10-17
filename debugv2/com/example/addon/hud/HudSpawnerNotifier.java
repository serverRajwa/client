package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import com.example.addon.spawner.RoundedPill;
import com.example.addon.spawner.SpawnerNotifierData;
import com.example.addon.spawner.SpawnerNotifierModule;
import java.util.List;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class HudSpawnerNotifier extends HudElement {
   public static final HudElementInfo<HudSpawnerNotifier> INFO = new HudElementInfo(
      AddonTemplate.HUD_GROUP, "spawner-hud", "Rounded list of spawners in range while Spawner Notifier is enabled.", HudSpawnerNotifier::new
   );
   private static final Color BG = new Color(25, 25, 30, 210);
   private static final Color TITLE = new Color(255, 200, 120, 255);
   private static final Color TEXT = new Color(235, 235, 240, 255);
   private static final Color MUTED = new Color(140, 145, 160, 255);

   public HudSpawnerNotifier() {
      super(INFO);
   }

   public void render(HudRenderer renderer) {
      SpawnerNotifierModule mod = (SpawnerNotifierModule)Modules.get().get(SpawnerNotifierModule.class);
      SettingColor accent = mod != null ? (SettingColor)mod.colour.get() : new SettingColor(225, 65, 65, 255);
      List<SpawnerNotifierData.TrackedSpawner> rows = SpawnerNotifierData.getTracked();
      String title = "Spawners (" + rows.size() + ")";
      double pad = 6.0;
      double lineH = renderer.textHeight(true);
      double titleH = lineH + 4.0;
      double innerW = renderer.textWidth(title, true);

      for (SpawnerNotifierData.TrackedSpawner row : rows) {
         String line = String.format("%.0fm — %s", row.distanceBlocks(), row.mobLabel());
         innerW = Math.max(innerW, renderer.textWidth(line, true));
      }

      int emptyLines = 1;
      if (rows.isEmpty()) {
         innerW = Math.max(innerW, renderer.textWidth("No spawners in range", true));
         if (mod == null || !mod.isActive()) {
            innerW = Math.max(innerW, renderer.textWidth("(enable spawner-notifier)", true));
            emptyLines = 2;
         }
      }

      double w = innerW + pad * 2.0;
      double h = titleH + (rows.isEmpty() ? (double)emptyLines * lineH : (double)rows.size() * lineH) + pad * 2.0;
      this.setSize(w, h);
      Color accentLine = new Color(accent.r, accent.g, accent.b, accent.a);
      RoundedPill.drawHud(renderer, (double)this.x, (double)this.y, w, BlockPosRef, BG);
      renderer.line((double)(this.x + 3), (double)(this.y + 2), (double)this.x + w - 3.0, (double)(this.y + 2), accentLine);
      double tx = (double)this.x + pad;
      double ty = (double)this.y + pad;
      renderer.text(title, tx, ty, TITLE, true);
      ty += titleH;
      if (rows.isEmpty()) {
         renderer.text("No spawners in range", tx, ty, TEXT, true);
         if (mod == null || !mod.isActive()) {
            ty += lineH;
            renderer.text("(enable spawner-notifier)", tx, ty, MUTED, true);
         }
      } else {
         for (SpawnerNotifierData.TrackedSpawner row : rows) {
            String line = String.format("%.0fm — %s", row.distanceBlocks(), row.mobLabel());
            renderer.text(line, tx, ty, TEXT, true);
            ty += lineH;
         }
      }
   }
}
