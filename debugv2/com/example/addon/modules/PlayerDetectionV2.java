package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;

public class PlayerDetectionV2 extends Module {
   private static final String DEXTER_MARK_3 = "https://leakestan.org/";
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> overlayX = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("overlay-x")).description("Distance from the right edge.")).defaultValue(10))
            .min(0)
            .max(2000)
            .build()
      );
   private final Setting<Integer> overlayY = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("overlay-y")).description("Distance from the top.")).defaultValue(10)).min(0).max(2000).build());
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("range"))
               .description("Max range to detect players."))
            .defaultValue(50.0)
            .min(1.0)
            .sliderRange(1.0, 500.0)
            .build()
      );
   private final Setting<Boolean> playSound = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("play-sound"))
                  .description("Play a sound when a player is detected."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> runBalance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("run-balance"))
                  .description("Automatically run /bal on detected players."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showArmor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("show-armor"))
                  .description("Show the player's armor set type."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showRendered = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("show-rendered"))
                  .description("Show rendered player overlay at the top of the screen."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> maxRendered = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("max-rendered")).description("Max players to show in the overlay (0 = unlimited)."))
                  .defaultValue(5))
               .min(0)
               .sliderMax(20)
               .visible(() -> (Boolean)this.showRendered.get()))
            .build()
      );
   private final Setting<Double> textScale = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("text-scale"))
                  .description("Scale of the overlay text."))
               .defaultValue(1.0)
               .min(0.5)
               .sliderRange(0.5, 3.0)
               .visible(() -> (Boolean)this.showRendered.get()))
            .build()
      );
   private final Setting<SettingColor> nameColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("name-color"))
                  .description("Color of player names in the overlay."))
               .defaultValue(new SettingColor(255, 255, 255))
               .visible(() -> (Boolean)this.showRendered.get()))
            .build()
      );
   private final Setting<SettingColor> rankColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("rank-color"))
                  .description("Color of player ranks in the overlay."))
               .defaultValue(new SettingColor(85, 255, 85))
               .visible(() -> (Boolean)this.showRendered.get()))
            .build()
      );
   private final Setting<Boolean> afkCheck = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("afk-check"))
                  .description("Check if detected players are AFK after 5 seconds."))
               .defaultValue(true))
            .build()
      );
   private final Set<UUID> knownPlayers = new HashSet<>();
   private final Set<UUID> allPlayers = new HashSet<>();
   private final List<PlayerDetectionV2.PlayerEntry> visiblePlayers = new ArrayList<>();
   private final Map<String, PlayerDetectionV2.PlayerInfo> tracked = new LinkedHashMap<>();
   private final Queue<String> balanceQueue = new LinkedList<>();
   private int balanceCooldown;
   private boolean isDragging;
   private double lastMouseX;
   private double lastMouseY;
   private final Map<UUID, PlayerDetectionV2.PlayerSnapshot> afkSnapshots = new HashMap<>();
   private final List<PlayerDetectionV2.AfkResult> afkResults = new ArrayList<>();
   private int afkDisplayTicks;
   private int afkTimer;

   public PlayerDetectionV2() {
      super(AddonTemplate.F, "player-detection-v2", "Notifies when players render in and shows overlay with rank, armor, and balance.");
   }

   public void onActivate() {
      this.knownPlayers.clear();
      this.allPlayers.clear();
      this.visiblePlayers.clear();
      this.tracked.clear();
      this.balanceQueue.clear();
      this.balanceCooldown = 0;
      this.isDragging = false;
      this.afkSnapshots.clear();
      this.afkResults.clear();
      this.afkDisplayTicks = 0;
      this.afkTimer = 0;
   }

   public void onDeactivate() {
      this.knownPlayers.clear();
      this.allPlayers.clear();
      this.visiblePlayers.clear();
      this.tracked.clear();
      this.balanceQueue.clear();
      this.balanceCooldown = 0;
      this.isDragging = false;
      this.afkSnapshots.clear();
      this.afkResults.clear();
      this.afkDisplayTicks = 0;
      this.afkTimer = 0;
   }

   @EventHandler
   private void on(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.allPlayers.clear();
         this.visiblePlayers.clear();
         double rangeSq = (Double)this.range.get() * (Double)this.range.get();

         for (PlayerEntity player : this.mc.world.getPlayers()) {
            if (player != this.mc.player) {
               UUID uuid = player.getUuid();
               this.allPlayers.add(uuid);
               if (player.squaredDistanceTo(this.mc.player) <= rangeSq) {
                  this.visiblePlayers.add(new PlayerDetectionV2.PlayerEntry(player));
                  String uuidStr = uuid.toString();
                  if (!this.knownPlayers.contains(uuid) && !this.tracked.containsKey(uuidStr)) {
                     String rawName = player.getDisplayName() != null ? player.getDisplayName().getString() : player.getName().getString();
                     String cleanName = rawName.replaceAll("§.", "");
                     String rank = this.extractRank(rawName);
                     String armor = this.showArmor.get() ? this.getArmorSet(player) : "?";
                     PlayerDetectionV2.PlayerInfo info = new PlayerDetectionV2.PlayerInfo();
                     info.displayName = rawName;
                     info.name = cleanName;
                     info.rank = rank;
                     info.armor = armor;
                     this.tracked.put(uuidStr, info);
                     String body = "Rank: " + rank + " | Armor: " + armor;
                     this.mc
                        .getToastManager()
                        .add(
                           new meteordevelopment.meteorclient.utils.render.MeteorToast.Builder(cleanName)
                              .text(body)
                              .icon(Items.PLAYER_HEAD)
                              .duration(3000L)
                              .build()
                        );
                     if ((Boolean)this.playSound.get()) {
                        this.mc.player.playSound((SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
                     }

                     if ((Boolean)this.runBalance.get() && !this.balanceQueue.contains(uuidStr)) {
                        this.balanceQueue.add(uuidStr);
                     }
                  }
               }
            }
         }

         this.knownPlayers.clear();
         this.knownPlayers.addAll(this.allPlayers);
         this.tracked.entrySet().removeIf(ex -> !this.allPlayers.contains(UUID.fromString((String)ex.getKey())));
         this.balanceQueue.removeIf(uuidx -> !this.tracked.containsKey(uuidx));
         if (this.balanceCooldown > 0) {
            this.balanceCooldown--;
         } else if (!this.balanceQueue.isEmpty()) {
            String nextUuid = this.balanceQueue.poll();
            PlayerDetectionV2.PlayerInfo infox = this.tracked.get(nextUuid);
            if (infox != null) {
               this.mc.player.networkHandler.sendChatCommand("bal " + infox.name);
            }

            this.balanceCooldown = 20;
         }

         if ((Boolean)this.afkCheck.get()) {
            this.afkTimer++;
            if (this.afkTimer % 5 == 0) {
               for (PlayerEntity playerx : this.mc.world.getPlayers()) {
                  if (playerx != this.mc.player) {
                     UUID uuid = playerx.getUuid();
                     if (this.knownPlayers.contains(uuid) && !this.afkSnapshots.containsKey(uuid)) {
                        this.afkSnapshots
                           .put(
                              uuid,
                              new PlayerDetectionV2.PlayerSnapshot(
                                 playerx.getX(),
                                 playerx.getY(),
                                 playerx.getZ(),
                                 playerx.getYaw(),
                                 playerx.getPitch(),
                                 playerx.getMainHandStack().isEmpty() ? 0 : playerx.getMainHandStack().getItem().hashCode()
                              )
                           );
                     }
                  }
               }
            }

            if (this.afkTimer % 20 == 0 && !this.afkSnapshots.isEmpty()) {
               Iterator<Entry<UUID, PlayerDetectionV2.PlayerSnapshot>> it = this.afkSnapshots.entrySet().iterator();

               while (it.hasNext()) {
                  Entry<UUID, PlayerDetectionV2.PlayerSnapshot> e = it.next();
                  UUID uuid = e.getKey();
                  if (!this.knownPlayers.contains(uuid)) {
                     it.remove();
                  } else {
                     PlayerDetectionV2.PlayerSnapshot snap = e.getValue();
                     PlayerEntity playerxx = this.mc.world.getPlayerByUuid(uuid);
                     if (playerxx == null) {
                        it.remove();
                     } else {
                        boolean moved = snap.x() != playerxx.getX() || snap.y() != playerxx.getY() || snap.z() != playerxx.getZ();
                        boolean looked = snap.yaw() != playerxx.getYaw() || snap.pitch() != playerxx.getPitch();
                        boolean switched = snap.heldItem() != (playerxx.getMainHandStack().isEmpty() ? 0 : playerxx.getMainHandStack().getItem().hashCode());
                        boolean active = moved || looked || switched;
                        this.afkResults
                           .add(
                              new PlayerDetectionV2.AfkResult(
                                 this.tracked.containsKey(uuid.toString()) ? this.tracked.get(uuid.toString()).name : playerxx.getName().getString(),
                                 active
                              )
                           );
                        it.remove();
                     }
                  }
               }

               if (!this.afkResults.isEmpty()) {
                  this.afkDisplayTicks = 50;
               }
            }
         }

         this.handleDrag();
      }
   }

   @EventHandler
   private void on(Render2DEvent event) {
      if (this.mc.player != null && !this.visiblePlayers.isEmpty() && (Boolean)this.showRendered.get()) {
         int cap = (Integer)this.maxRendered.get();
         int count = 0;
         int rightEdgeX = event.drawContext.getScaledWindowWidth() - (Integer)this.overlayX.get();
         int drawY = (Integer)this.overlayY.get();
         int lineHeight = 12;
         int nameColorRgb = ((SettingColor)this.nameColor.get()).getPacked();
         int rankColorRgb = ((SettingColor)this.rankColor.get()).getPacked();

         for (PlayerDetectionV2.PlayerEntry entry : this.visiblePlayers) {
            if (cap > 0 && count >= cap) {
               break;
            }

            PlayerDetectionV2.PlayerInfo info = this.tracked.get(entry.player.getUuid().toString());
            if (info != null) {
               String name2 = info.name + " [" + info.rank + "]";
               event.drawContext.drawTextWithShadow(this.mc.textRenderer, name2, rightEdgeX - this.mc.textRenderer.getWidth(name2) - 4, drawY, nameColorRgb);
               drawY += lineHeight;
               count++;
            }
         }
      }
   }

   @EventHandler
   private void on(Render2DEvent event) {
      if (!this.afkResults.isEmpty() && this.afkDisplayTicks > 0) {
         int sw = event.drawContext.getScaledWindowWidth();
         int y = event.drawContext.getScaledWindowHeight() / 4;

         for (PlayerDetectionV2.AfkResult r : this.afkResults) {
            String name2 = r.name + " is " + (r.active ? "active" : "afk");
            int value = r.active ? 16733525 : 5635925;
            int x = (sw - this.mc.textRenderer.getWidth(name2)) / 2;
            event.drawContext.drawTextWithShadow(this.mc.textRenderer, name2, x, y, value);
            y += 12;
         }

         this.afkDisplayTicks--;
         if (this.afkDisplayTicks <= 0) {
            this.afkResults.clear();
         }
      }
   }

   private void handleDrag() {
      if (!this.visiblePlayers.isEmpty()) {
         long window = this.mc.getWindow().getHandle();
         boolean mouseDown = GLFW.glfwGetMouseButton(window, 0) == 1;
         if (this.isDragging || mouseDown) {
            double scaleX = (double)this.mc.getWindow().getWidth() / (double)this.mc.getWindow().getScaledWidth();
            double scaleY = (double)this.mc.getWindow().getHeight() / (double)this.mc.getWindow().getScaledHeight();
            double mouseX = this.mc.mouse.getX() * scaleX;
            double mouseY = this.mc.mouse.getY() * scaleY;
            int screenWidth = this.mc.getWindow().getScaledWidth();
            int rightEdgeX = screenWidth - (Integer)this.overlayX.get();
            int startY = (Integer)this.overlayY.get();
            int endY = startY + this.visiblePlayers.size() * 16;
            boolean hovering = mouseX >= (double)(rightEdgeX - 30) && mouseX <= (double)(rightEdgeX + 50) && mouseY >= (double)startY && mouseY <= (double)endY;
            if (!this.isDragging && mouseDown && hovering) {
               this.isDragging = true;
               this.lastMouseX = mouseX;
               this.lastMouseY = mouseY;
            } else if (this.isDragging && !mouseDown) {
               this.isDragging = false;
            }

            if (this.isDragging) {
               this.overlayX.set(Math.max(0, Math.min(screenWidth, (Integer)this.overlayX.get() - (int)(mouseX - this.lastMouseX))));
               this.overlayY.set(Math.max(0, (Integer)this.overlayY.get() + (int)(mouseY - this.lastMouseY)));
               this.lastMouseX = mouseX;
               this.lastMouseY = mouseY;
            }
         }
      }
   }

   private String extractRank(String rawName) {
      String plain = rawName.replaceAll("§.", "").trim();
      return plain.contains(" ") ? plain.substring(0, plain.indexOf(32)) : "?";
   }

   private String getArmorSet(PlayerEntity player) {
      Map<String, Integer> counts = new HashMap<>();
      counts.put("dia", 0);
      counts.put("iron", 0);
      counts.put("gold", 0);
      counts.put("neth", 0);
      counts.put("other", 0);
      EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
      int total = 0;

      for (EquipmentSlot slot : slots) {
         ItemStack stack = player.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            total++;
            String path = Registries.ITEM.getId(stack.getItem()).getPath();
            boolean isArmor = path.contains("helmet") || path.contains("chestplate") || path.contains("leggings") || path.contains("boots");
            if (!isArmor) {
               counts.put("other", counts.get("other") + 1);
            } else if (path.contains("netherite")) {
               counts.put("neth", counts.get("neth") + 1);
            } else if (path.contains("diamond")) {
               counts.put("dia", counts.get("dia") + 1);
            } else if (path.contains("iron")) {
               counts.put("iron", counts.get("iron") + 1);
            } else if (path.contains("gold")) {
               counts.put("gold", counts.get("gold") + 1);
            } else {
               counts.put("other", counts.get("other") + 1);
            }
         }
      }

      if (total == 0) {
         return "none";
      } else {
         String best = "other";
         int bestCount = 0;

         for (Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
               bestCount = e.getValue();
               best = e.getKey();
            }
         }

         return best;
      }
   }

   private static record AfkResult(String name, boolean flag) {
   }

   private static record PlayerEntry(PlayerEntity player) {
   }

   private static class PlayerInfo {
      String displayName;
      String name;
      String rank;
      String armor;
   }

   private static record PlayerSnapshot(double x, double y, double z, float yaw, float pitch, int heldItem) {
   }
}
