package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
public class StaffOnline extends Module {
   private static final List<String> DEFAULT_STAFF = Arrays.asList(
      "Dough4",
      "FallerFly",
      "Evxn",
      "Ryuui_",
      "Shyalyy",
      "OGsummer",
      "ItsDefRealMe",
      "LzouZMp5",
      "Munkerlich",
      "Chaon",
      "Showered",
      "PastaGamer",
      "Bautiegar",
      "BloodSplulse",
      "GsMusie",
      "Frwost",
      "FluffyMaster07",
      "W1zox_",
      "ItsZDeath",
      "ArchivePedro",
      "Evify",
      "Noah",
      "Zababi",
      "Nathan"
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgOverlay = this.settings.createGroup("Overlay");
   private final SettingGroup sgStaff = this.settings.createGroup("Staff List");
   private final Setting<StaffOnline.Mode> displayMode = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("display-mode")).defaultValue(StaffOnline.Mode.Overlay)).build());
   private final Setting<Boolean> autoEnable = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("auto-enable"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> refreshInterval = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("refresh-interval"))
               .defaultValue(100))
            .min(20)
            .sliderMax(600)
            .build()
      );
   private final Setting<Boolean> notifyOnChange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("notify-on-change"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> overlayX = this.sgOverlay
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("x-position"))
            .defaultValue(10.0)
            .min(0.0)
            .sliderMax(2000.0)
            .build()
      );
   private final Setting<Double> overlayY = this.sgOverlay
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("y-position"))
            .defaultValue(10.0)
            .min(0.0)
            .sliderMax(2000.0)
            .build()
      );
   private final Setting<Double> overlayScale = this.sgOverlay
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("scale"))
            .defaultValue(1.2)
            .min(0.5)
            .max(3.0)
            .build()
      );
   private final Setting<Integer> panelWidth = this.sgOverlay
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("panel-width"))
               .defaultValue(120))
            .min(60)
            .max(500)
            .sliderMax(500)
            .build()
      );
   private final Setting<Boolean> showOffline = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("show-offline"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> bgAlpha = this.sgOverlay
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("background-alpha"))
               .defaultValue(120))
            .min(0)
            .max(255)
            .sliderMax(255)
            .build()
      );
   private final Setting<Boolean> detectVanished = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("detect-vanished"))
               .defaultValue(true))
            .build()
      );
   private final Setting<List<String>> staffList = this.sgStaff
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                     .name("staff-names"))
                  .description("List of staff names to track."))
               .defaultValue(DEFAULT_STAFF))
            .build()
      );
   private int tickCounter = 0;
   private Set<String> previouslyOnline = new HashSet<>();
   public static volatile List<String> lastOnlineStaff = new ArrayList<>();
   public static volatile List<String> lastOfflineStaff = new ArrayList<>();

   public StaffOnline() {
      super(AddonTemplate.F, "staff-online", "Displays which staff members are online.");
   }

   public void onActivate() {
      this.tickCounter = 0;
      this.previouslyOnline.clear();
      if (this.mc.getNetworkHandler() != null) {
         this.checkStaff();
      }
   }

   public void onDeactivate() {
      this.previouslyOnline.clear();
      lastOnlineStaff = new ArrayList<>();
      lastOfflineStaff = new ArrayList<>();
   }

   @EventHandler
   private void onE(Post e) {
      this.tickCounter++;
      if (this.tickCounter >= (Integer)this.refreshInterval.get()) {
         this.tickCounter = 0;
         this.checkStaff();
      }
   }

   private void drawRoundedRect(double x, double y, double w, double h, double r, SettingColor col) {
      int steps = 30;
      Renderer2D.COLOR.begin();
      Renderer2D.COLOR.quad(x + r, y, w - r * 2.0, BlockPosRef, col);
      Renderer2D.COLOR.quad(x, y + r, r, h - r * 2.0, col);
      Renderer2D.COLOR.quad(x + w - r, y + r, r, h - r * 2.0, col);
      Renderer2D.COLOR.render();
      double[][] corners = new double[][]{
         {x + r, y + r, Math.PI, Math.PI / 2},
         {x + w - r, y + r, Math.PI / 2, 0.0},
         {x + r, y + h - r, Math.PI, Math.PI * 3.0 / 2.0},
         {x + w - r, y + h - r, Math.PI * 3.0 / 2.0, Math.PI * 2}
      };

      for (double[] c : corners) {
         double cx = c[0];
         double cy = c[1];
         double sa = c[2];
         double ea = c[3];
         if (ea < sa) {
            ea += Math.PI * 2;
         }

         Renderer2D.COLOR.begin();

         for (int i = 0; i < steps; i++) {
            double a1 = sa + (ea - sa) * (double)i / (double)steps;
            double a2 = sa + (ea - sa) * (double)(i + 1) / (double)steps;
            Renderer2D.COLOR.triangle(cx, cy, cx + Math.cos(a1) * r, cy - Math.sin(a1) * r, cx + Math.cos(a2) * r, cy - Math.sin(a2) * r, col);
         }

         Renderer2D.COLOR.render();
      }
   }

   private void drawCornerFill(double cx, double cy, double r, double startAngle, double endAngle, SettingColor col) {
      int steps = 30;
      double range = endAngle - startAngle;
      if (range < 0.0) {
         range += Math.PI * 2;
      }

      Renderer2D.COLOR.begin();

      for (int i = 0; i < steps; i++) {
         double a1 = startAngle + range * (double)i / (double)steps;
         double a2 = startAngle + range * (double)(i + 1) / (double)steps;
         Renderer2D.COLOR.triangle(cx, cy, cx + Math.cos(a1) * r, cy - Math.sin(a1) * r, cx + Math.cos(a2) * r, cy - Math.sin(a2) * r, col);
      }

      Renderer2D.COLOR.render();
   }

   @EventHandler
   private void on(Render2DEvent event) {
      if (this.displayMode.get() != StaffOnline.Mode.Chat) {
         List<String> online = lastOnlineStaff;
         List<String> offline = lastOfflineStaff;
         if (online == null) {
            online = new ArrayList<>();
         }

         if (offline == null) {
            offline = new ArrayList<>();
         }

         List<String> display = new ArrayList<>(online);
         if ((Boolean)this.showOffline.get()) {
            display.addAll(offline);
         }

         double x = (Double)this.overlayX.get();
         double y = (Double)this.overlayY.get();
         double s = (Double)this.overlayScale.get();
         double pad = 14.0 * s;
         double lineH = 20.0 * s;
         double cornerR = 12.0 * s;
         double headerH = 26.0 * s;
         double dotSize = 8.0 * s;
         int alpha = (Integer)this.bgAlpha.get();
         String title = "Online Staff";
         double bgW = (double)((Integer)this.panelWidth.get()).intValue() * s;
         double totalH = headerH + pad + (display.isEmpty() ? lineH : (double)display.size() * lineH) + pad;
         SettingColor bgCol = new SettingColor(0, 0, 0, alpha);
         this.drawRoundedRect(x + 2.0, y + 2.0, bgW, totalH, cornerR, new SettingColor(0, 0, 0, alpha / 2));
         this.drawRoundedRect(x, y, bgW, totalH, cornerR, bgCol);
         Renderer2D.COLOR.begin();
         Renderer2D.COLOR.quad(x + cornerR, y, bgW - cornerR * 2.0, headerH, bgCol);
         Renderer2D.COLOR.quad(x, y + cornerR, cornerR, headerH - cornerR, bgCol);
         Renderer2D.COLOR.quad(x + bgW - cornerR, y + cornerR, cornerR, headerH - cornerR, bgCol);
         Renderer2D.COLOR.render();
         this.drawCornerFill(x + cornerR, y + cornerR, cornerR, Math.PI / 2, Math.PI, bgCol);
         this.drawCornerFill(x + bgW - cornerR, y + cornerR, cornerR, 0.0, Math.PI / 2, bgCol);
         TextRenderer.get().render(title, x + pad, y + (headerH - TextRenderer.get().getHeight()) / 2.0, new SettingColor(255, 255, 255, 255));
         double drawY = y + headerH + pad;
         if (display.isEmpty()) {
            TextRenderer.get().render("No staff online", x + pad, drawY, new SettingColor(150, 150, 150, 255));
         } else {
            for (String name : display) {
               boolean on = online.contains(name);
               SettingColor dotCol = on ? new SettingColor(0, 255, 100, 255) : new SettingColor(255, 60, 60, 255);
               double dotX = x + pad;
               double dotY = drawY + (lineH - dotSize) / 2.0;
               Renderer2D.COLOR.begin();
               Renderer2D.COLOR.quad(dotX, dotY, dotSize, dotSize, dotCol);
               Renderer2D.COLOR.render();
               int c = on ? 255 : 190;
               TextRenderer.get()
                  .render(name, x + pad + dotSize + 6.0 * s, drawY + (lineH - TextRenderer.get().getHeight()) / 2.0, new SettingColor(c, c, c, 255));
               drawY += lineH;
            }
         }
      }
   }

   @EventHandler
   private void onE(GameJoinedEvent e) {
      if ((Boolean)this.autoEnable.get() && !this.isActive()) {
         this.toggle();
      }

      this.previouslyOnline.clear();
      if (this.isActive()) {
         this.checkStaff();
      }
   }

   @EventHandler
   private void onE(GameLeftEvent e) {
      this.previouslyOnline.clear();
      lastOnlineStaff = new ArrayList<>();
      lastOfflineStaff = new ArrayList<>();
   }

   private void checkStaff() {
      if (this.mc.getNetworkHandler() == null) {
         lastOnlineStaff = new ArrayList<>();
         lastOfflineStaff = new ArrayList<>();
      } else {
         Set<String> staffNames = new HashSet<>((Collection<? extends String>)this.staffList.get());
         Set<String> onlinePlayers = new HashSet<>();
         Map<String, String> lowerToReal = new HashMap<>();

         for (PlayerListEntry e : this.mc.getNetworkHandler().getPlayerList()) {
            onlinePlayers.add(e.getProfile().name().toLowerCase());
            lowerToReal.put(e.getProfile().name().toLowerCase(), e.getProfile().name());
         }

         if ((Boolean)this.detectVanished.get() && this.mc.world != null) {
            for (PlayerEntity player : this.mc.world.getPlayers()) {
               String name = player.getName().getString();
               onlinePlayers.add(name.toLowerCase());
               lowerToReal.put(name.toLowerCase(), name);
            }
         }

         List<String> onlineStaff = new ArrayList<>();
         List<String> offlineStaff = new ArrayList<>();

         for (String sn : staffNames) {
            if (!sn.isBlank()) {
               String real = lowerToReal.get(sn.toLowerCase());
               if (real != null) {
                  onlineStaff.add(real);
               } else {
                  offlineStaff.add(sn);
               }
            }
         }

         if ((Boolean)this.notifyOnChange.get() && !this.previouslyOnline.isEmpty()) {
            Set<String> cur = new HashSet<>(onlineStaff);

            for (String str : cur) {
               if (!this.previouslyOnline.contains(str)) {
                  this.info(str + " online", new Object[0]);
               }
            }

            for (String nx : this.previouslyOnline) {
               if (!cur.contains(nx)) {
                  this.warning(nx + " offline", new Object[0]);
               }
            }
         }

         this.previouslyOnline = new HashSet<>(onlineStaff);
         lastOnlineStaff = new ArrayList<>(onlineStaff);
         lastOfflineStaff = new ArrayList<>(offlineStaff);
         if (this.displayMode.get() == StaffOnline.Mode.Chat || this.displayMode.get() == StaffOnline.Mode.Both) {
            if (onlineStaff.isEmpty()) {
               ChatUtils.infoPrefix("Staff", "No staff online.", new Object[0]);
            } else {
               ChatUtils.infoPrefix("Staff", "Online: " + String.join(", ", onlineStaff), new Object[0]);
            }
         }
      }
   }

   public static enum Mode {
      Chat,
      Overlay,
      Both;
   }
}
