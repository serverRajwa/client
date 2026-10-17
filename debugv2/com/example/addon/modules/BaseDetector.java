package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.toast.SystemToast.Type;
public class BaseDetector extends Module {
   private static final int TICKS_PER_SECOND = 20;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgChunkDensity = this.settings.createGroup("Chunk Density");
   private final SettingGroup sgSpawner = this.settings.createGroup("Spawners");
   private final SettingGroup sgDepthGuard = this.settings.createGroup("Depth Guard");
   private final SettingGroup sgTracers = this.settings.createGroup("Tracers");
   private final Setting<Double> scanRange = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("scan-range"))
               .description("Horizontal block range to scan loaded chunks. Y is ignored so bedrock spawners are always found."))
            .defaultValue(64.0)
            .sliderMin(16.0)
            .sliderMax(256.0)
            .build()
      );
   private final Setting<Integer> scanIntervalTicks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("scan-interval-ticks"))
                  .description("How often to rescan. Higher = better performance."))
               .defaultValue(5))
            .min(1)
            .sliderMin(1)
            .sliderMax(40)
            .build()
      );
   private final Setting<Boolean> renderDenseChunkMarkers = this.sgChunkDensity
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("render-dense-chunk-markers"))
                  .description("Render a flat marker for chunks with too many block entities."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> denseChunkThreshold = this.sgChunkDensity
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("dense-chunk-threshold"))
                  .description("Chunks with more block entities than this get a marker."))
               .defaultValue(30))
            .min(1)
            .sliderMin(1)
            .sliderMax(300)
            .build()
      );
   private final Setting<Integer> maxDenseChunkMarkers = this.sgChunkDensity
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("max-dense-chunk-markers"))
                  .description("Max dense chunk markers rendered at once."))
               .defaultValue(64))
            .min(0)
            .sliderMin(0)
            .sliderMax(512)
            .build()
      );
   private final Setting<Integer> maxSavedDenseChunks = this.sgChunkDensity
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("max-saved-dense-chunks"))
                  .description("Max dense chunk detections to keep. Oldest removed first."))
               .defaultValue(256))
            .min(1)
            .sliderMin(16)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Double> denseChunkMarkerSize = this.sgChunkDensity
      .add(
         ((Builder)((Builder)new Builder().name("dense-chunk-marker-size")).description("Size of the dense chunk highlight. 16 = full chunk."))
            .defaultValue(20.0)
            .min(4.0)
            .sliderMin(4.0)
            .sliderMax(40.0)
            .build()
      );
   private final Setting<Double> denseChunkMarkerThickness = this.sgChunkDensity
      .add(
         ((Builder)((Builder)new Builder().name("dense-chunk-marker-thickness")).description("Thickness of the dense chunk highlight layer."))
            .defaultValue(0.08)
            .min(0.01)
            .sliderMin(0.01)
            .sliderMax(1.0)
            .build()
      );
   private final Setting<Double> markerHeightOffset = this.sgChunkDensity
      .add(
         ((Builder)((Builder)new Builder().name("marker-height-offset")).description("How far above your feet the marker Y is saved when detected."))
            .defaultValue(0.05)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build()
      );
   private final Setting<Boolean> denseChunkChatNotify = this.sgChunkDensity
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("dense-chunk-chat-notify"))
                  .description("Post a chat message when a new dense chunk is detected."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> denseChunkToastNotify = this.sgChunkDensity
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("dense-chunk-toast-notify"))
                  .description("Show a toast when a new dense chunk is detected."))
               .defaultValue(false))
            .build()
      );
   private final Setting<SettingColor> denseChunkLineColor = this.sgChunkDensity
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("dense-chunk-line-color"))
               .description("Outline color for dense chunk markers."))
            .defaultValue(new SettingColor(0, 255, 0, 230))
            .build()
      );
   private final Setting<SettingColor> denseChunkFillColor = this.sgChunkDensity
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("dense-chunk-fill-color"))
               .description("Fill color for dense chunk markers."))
            .defaultValue(new SettingColor(0, 255, 0, 55))
            .build()
      );
   private final Setting<Boolean> spawnerChatNotify = this.sgSpawner
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("spawner-chat-notify"))
                  .description("Chat message when a new mob spawner enters range."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> spawnerToastNotify = this.sgSpawner
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("spawner-toast-notify"))
                  .description("Toast notification when a new mob spawner is found."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> highlightSpawnerChunks = this.sgSpawner
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("highlight-spawner-chunks"))
                  .description("Highlight the full chunk column for chunks containing spawners."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> renderSpawnerBoxes = this.sgSpawner
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("render-spawner-boxes"))
                  .description("Render rainbow boxes around mob spawners."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> rainbowSpeed = this.sgSpawner
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("spawner-rainbow-speed")).description("Speed of rainbow hue cycling on spawner boxes."))
               .defaultValue(2.0)
               .sliderMin(0.5)
               .sliderMax(10.0)
               .visible(this.renderSpawnerBoxes::get))
            .build()
      );
   private final Setting<Integer> rainbowAlpha = this.sgSpawner
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("spawner-rainbow-alpha"))
                     .description("Opacity of the rainbow spawner box outline."))
                  .defaultValue(220))
               .sliderMin(50)
               .sliderMax(255)
               .visible(this.renderSpawnerBoxes::get))
            .build()
      );
   private final Setting<SettingColor> spawnerChunkLineColor = this.sgSpawner
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("spawner-chunk-line-color"))
                  .description("Outline color for chunks containing spawners."))
               .defaultValue(new SettingColor(0, 255, 0, 220))
               .visible(this.highlightSpawnerChunks::get))
            .build()
      );
   private final Setting<SettingColor> spawnerChunkFillColor = this.sgSpawner
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("spawner-chunk-fill-color"))
                  .description("Fill color for chunks containing spawners."))
               .defaultValue(new SettingColor(0, 255, 0, 35))
               .visible(this.highlightSpawnerChunks::get))
            .build()
      );
   private final Setting<Boolean> depthGuardEnabled = this.sgDepthGuard
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("depth-guard-enabled"))
                  .description("Kick you when you go below the set Y level."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> triggerY = this.sgDepthGuard
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("trigger-y"))
                     .description("Y level that triggers the kick."))
                  .defaultValue(-30))
               .sliderMin(-64)
               .sliderMax(0)
               .visible(this.depthGuardEnabled::get))
            .build()
      );
   private final Setting<Boolean> sendWarning = this.sgDepthGuard
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("warn-before-kick"))
                     .description("Send a chat warning 5 blocks above the trigger Y."))
                  .defaultValue(true))
               .visible(this.depthGuardEnabled::get))
            .build()
      );
   private final Setting<Boolean> showDenseChunkTracers = this.sgTracers
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("show-dense-chunk-tracers"))
                  .description("Draw tracer lines to saved dense chunk detections."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showSpawnerTracers = this.sgTracers
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("show-spawner-tracers"))
                  .description("Draw tracer lines to found mob spawners."))
               .defaultValue(false))
            .build()
      );
   private final Setting<SettingColor> denseChunkTracerColor = this.sgTracers
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("dense-chunk-tracer-color"))
                  .description("Tracer color for dense chunk detections."))
               .defaultValue(new SettingColor(0, 255, 0, 220))
               .visible(this.showDenseChunkTracers::get))
            .build()
      );
   private final Setting<SettingColor> spawnerTracerColor = this.sgTracers
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("spawner-tracer-color"))
                  .description("Tracer color for mob spawners."))
               .defaultValue(new SettingColor(0, 255, 0, 220))
               .visible(this.showSpawnerTracers::get))
            .build()
      );
   private final Map<ChunkPos, BaseDetector.DenseChunkDetection> denseChunks = new LinkedHashMap<>();
   private final Set<BlockPos> spawnerPositions = new HashSet<>();
   private final Set<BlockPos> announcedSpawners = new HashSet<>();
   private final Set<ChunkPos> announcedDenseChunks = new HashSet<>();
   private final Set<ChunkPos> spawnerChunks = new HashSet<>();
   private float rainbowHue;
   private int scanTicks;
   private boolean wasInWorld;
   private boolean warningSent;
   private boolean inDangerZone;
   private boolean kickLockedUntilSafe;
   private boolean rejoinGraceActive;
   private int rejoinGraceTicks;
   private boolean wasDisconnectedByUs;
   private static final int REJOIN_GRACE_TICKS = 200;

   public BaseDetector() {
      super(AddonTemplate.F, "base-detector", "Base Detector.");
   }

   public void onActivate() {
      if (false) {
         String reason = true ? null : "module ready..................";
         this.sendChat("[BaseDetector] Auth required:" + reason, 16733525);
         this.toggle();
      } else {
         this.clearScanCaches();
         this.resetDepthState();
         this.rainbowHue = 0.0F;
         this.scanTicks = 0;
         this.wasInWorld = this.mc.player != null && this.mc.world != null;
      }
   }

   public void onDeactivate() {
      this.clearScanCaches();
      this.resetDepthState();
   }

   @EventHandler
   private void on(Post event) {
      if (false) {
         if (this.isActive()) {
            this.toggle();
         }
      } else {
         this.handleDepthGuardTick();
         this.handleFinderTick();
      }
   }

   @EventHandler
   private void on(Render3DEvent event) {
      if (true) {
         if (this.mc.world != null && this.mc.player != null) {
            if ((Boolean)this.highlightSpawnerChunks.get()) {
               this.renderSpawnerChunks(event);
            }

            if ((Boolean)this.renderSpawnerBoxes.get() || (Boolean)this.showSpawnerTracers.get()) {
               for (BlockPos pos : this.spawnerPositions) {
                  BlockEntity be = this.mc.world.getBlockEntity(pos);
                  if (be instanceof MobSpawnerBlockEntity) {
                     if ((Boolean)this.renderSpawnerBoxes.get()) {
                        this.renderRainbowBox(event, new Box(pos).expand(0.005), pos);
                     }

                     if ((Boolean)this.showSpawnerTracers.get()) {
                        this.renderSpawnerTracer(event, pos);
                     }
                  }
               }
            }

            if ((Boolean)this.renderDenseChunkMarkers.get()) {
               this.renderDenseChunkMarkers(event);
            }

            if ((Boolean)this.showDenseChunkTracers.get()) {
               this.renderDenseChunkTracers(event);
            }
         }
      }
   }

   private void handleFinderTick() {
      if (this.mc.world != null && this.mc.player != null) {
         this.rainbowHue = (this.rainbowHue + (float)((Double)this.rainbowSpeed.get() * 0.5)) % 360.0F;
         if (this.scanTicks > 0) {
            this.scanTicks--;
         } else {
            this.scanTicks = Math.max(1, (Integer)this.scanIntervalTicks.get());
            this.rescanLoadedChunks();
         }
      } else {
         this.spawnerPositions.clear();
         this.spawnerChunks.clear();
      }
   }

   private void rescanLoadedChunks() {
      this.spawnerPositions.clear();
      this.spawnerChunks.clear();
      Map<ChunkPos, Integer> blockEntityCounts = new HashMap<>();
      double range = (Double)this.scanRange.get();
      double rangeSq = range * range;
      double playerX = this.mc.player.getX();
      double playerZ = this.mc.player.getZ();
      ClientChunkManager chunkManager = this.mc.world.getChunkManager();
      int centerCX = this.mc.player.getChunkPos().x;
      int centerCZ = this.mc.player.getChunkPos().z;
      int radius = (int)Math.ceil(range / 16.0) + 1;

      for (int cx = centerCX - radius; cx <= centerCX + radius; cx++) {
         for (int cz = centerCZ - radius; cz <= centerCZ + radius; cz++) {
            WorldChunk chunk = chunkManager.getWorldChunk(cx, cz);
            if (chunk != null) {
               ChunkPos chunkPos = chunk.getPos();
               int count = 0;

               for (Entry<BlockPos, BlockEntity> beEntry : chunk.getBlockEntities().entrySet()) {
                  BlockEntity be = beEntry.getValue();
                  if (be != null) {
                     BlockPos pos = beEntry.getKey().toImmutable();
                     double dx = (double)pos.getX() + 0.5 - playerX;
                     double dz = (double)pos.getZ() + 0.5 - playerZ;
                     if (!(dx * dx + dz * dz > rangeSq)) {
                        count++;
                        if (be instanceof MobSpawnerBlockEntity) {
                           this.spawnerPositions.add(pos);
                           this.spawnerChunks.add(new ChunkPos(pos));
                           if (this.announcedSpawners.add(pos)) {
                              this.notifySpawnerFound(pos);
                           }
                        }
                     }
                  }
               }

               if (count > 0) {
                  blockEntityCounts.put(chunkPos, count);
               }
            }
         }
      }

      int renderedDense = 0;

      for (Entry<ChunkPos, Integer> entry : blockEntityCounts.entrySet()) {
         if (entry.getValue() > (Integer)this.denseChunkThreshold.get()) {
            if (renderedDense >= (Integer)this.maxDenseChunkMarkers.get()) {
               break;
            }

            this.saveDenseChunkDetection(entry.getKey(), entry.getValue());
            renderedDense++;
         }
      }

      this.announcedSpawners.removeIf(posx -> !this.spawnerPositions.contains(posx));
   }

   private void saveDenseChunkDetection(ChunkPos chunkPos, int count) {
      BaseDetector.DenseChunkDetection existing = this.denseChunks.get(chunkPos);
      boolean isNew = existing == null;
      double markerY = isNew ? this.mc.player.getY() + (Double)this.markerHeightOffset.get() : existing.markerY;
      long detectedAtMillis = isNew ? System.currentTimeMillis() : existing.detectedAtMillis;
      this.denseChunks.put(chunkPos, new BaseDetector.DenseChunkDetection(chunkPos, count, markerY, detectedAtMillis));
      this.trimDenseChunkDetections();
      if (isNew && this.announcedDenseChunks.add(chunkPos)) {
         this.notifyDenseChunkFound(chunkPos, count, markerY);
      }
   }

   private void trimDenseChunkDetections() {
      int limit = Math.max(1, (Integer)this.maxSavedDenseChunks.get());

      while (this.denseChunks.size() > limit) {
         ChunkPos oldest = this.denseChunks.keySet().iterator().next();
         this.denseChunks.remove(oldest);
         this.announcedDenseChunks.remove(oldest);
      }
   }

   private void handleDepthGuardTick() {
      if (!(Boolean)this.depthGuardEnabled.get()) {
         this.resetDepthState();
      } else if (this.mc.player != null && this.mc.world != null) {
         if (this.mc.player.getY() <= (double)((Integer)this.triggerY.get()).intValue()) {
            this.inDangerZone = true;
            this.sendChat("Y Entity  Limit Packet Blocked. RE LOG FLY UP", 16733525);
         } else {
            boolean currentlyInWorld = this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null;
            if (!currentlyInWorld) {
               if (this.wasInWorld && this.wasDisconnectedByUs) {
                  this.rejoinGraceActive = true;
                  this.rejoinGraceTicks = 200;
               }

               this.wasInWorld = false;
               this.warningSent = false;
            } else {
               if (!this.wasInWorld) {
                  this.wasInWorld = true;
                  if (!this.rejoinGraceActive) {
                     this.wasDisconnectedByUs = false;
                     this.warningSent = false;
                  }
               }

               double currentY = this.mc.player.getY();
               int limit = (Integer)this.triggerY.get();
               int warnLevel = limit + 5;
               if (this.rejoinGraceActive) {
                  if (currentY <= (double)limit) {
                     if (this.rejoinGraceTicks > 0) {
                        this.rejoinGraceTicks--;
                        return;
                     }

                     this.rejoinGraceActive = false;
                     this.rejoinGraceTicks = 0;
                     this.wasDisconnectedByUs = false;
                     this.inDangerZone = true;
                     this.kickLockedUntilSafe = true;
                     return;
                  }

                  this.rejoinGraceActive = false;
                  this.rejoinGraceTicks = 0;
                  this.wasDisconnectedByUs = false;
                  this.inDangerZone = false;
                  this.warningSent = false;
               }

               if (currentY > (double)limit) {
                  this.inDangerZone = false;
                  this.kickLockedUntilSafe = false;
                  if (currentY > (double)warnLevel) {
                     this.warningSent = false;
                  } else if ((Boolean)this.sendWarning.get() && !this.warningSent) {
                     this.sendChat(String.format("[BaseDetector] Warning: Y=%d current: %.1f          ", limit, currentY), 16755200);
                     this.warningSent = true;
                  }
               } else if (!this.kickLockedUntilSafe) {
                  if (!this.inDangerZone) {
                     this.inDangerZone = true;
                     this.kickLockedUntilSafe = true;
                     this.wasDisconnectedByUs = true;
                     this.rejoinGraceActive = true;
                     this.rejoinGraceTicks = 200;
                     this.disconnect();
                  }
               }
            }
         }
      }
   }

   private void resetDepthState() {
      this.wasInWorld = false;
      this.warningSent = false;
      this.inDangerZone = false;
      this.kickLockedUntilSafe = false;
      this.wasDisconnectedByUs = false;
      this.rejoinGraceActive = false;
      this.rejoinGraceTicks = 0;
   }

   private void disconnect() {
      this.sendChat("Y Entity  Limit Packet Blocked. RE LOG FLY UP", 16733525);
   }

   private void notifyDenseChunkFound(ChunkPos chunkPos, int count, double markerY) {
      if ((Boolean)this.denseChunkChatNotify.get() && this.mc.inGameHud != null) {
         Text msg = Text.literal("[BaseDetector]")
            .styled(s -> s.withColor(4234495))
            .append(Text.literal("Dense Chunk").styled(s -> s.withColor(5635925)))
            .append(
               Text.literal(String.format(" detected at chunk %d, %d with %d block entities", chunkPos.x, chunkPos.z, count))
                  .styled(s -> s.withColor(16777215))
            )
            .append(Text.literal(String.format(" (marker Y %.1f)", markerY)).styled(s -> s.withColor(16777045)));
         this.mc.inGameHud.getChatHud().addMessage(msg);
      }

      if ((Boolean)this.denseChunkToastNotify.get() && this.mc.getToastManager() != null) {
         this.mc
            .getToastManager()
            .add(
               new SystemToast(
                  Type.PERIODIC_NOTIFICATION,
                  Text.literal("BaseDetector: Dense Chunk"),
                  Text.literal(String.format("Chunk %d, %d | %d block entities", chunkPos.x, chunkPos.z, count))
               )
            );
      }
   }

   private void notifySpawnerFound(BlockPos pos) {
      if ((Boolean)this.spawnerChatNotify.get() && this.mc.inGameHud != null) {
         Text msg = Text.literal("[BaseDetector]")
            .styled(s -> s.withColor(4234495))
            .append(Text.literal("Mob Spawner").styled(s -> s.withColor(5635925)))
            .append(Text.literal(" found at ").styled(s -> s.withColor(16777215)))
            .append(
               Text.literal(String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ()))
                  .styled(s -> s.withColor(16777045))
            )
            .append(Text.literal("!").styled(s -> s.withColor(16777215)));
         this.mc.inGameHud.getChatHud().addMessage(msg);
      }

      if ((Boolean)this.spawnerToastNotify.get() && this.mc.getToastManager() != null) {
         this.mc
            .getToastManager()
            .add(
               new SystemToast(
                  Type.PERIODIC_NOTIFICATION,
                  Text.literal("BaseDetector: Spawner Found"),
                  Text.literal(String.format("X %d, Y %d, Z %d", pos.getX(), pos.getY(), pos.getZ()))
               )
            );
      }
   }

   private void sendChat(String message, int value) {
      if (this.mc.inGameHud != null) {
         this.mc.inGameHud.getChatHud().addMessage(Text.literal(message).styled(s -> s.withColor(value)));
      }
   }

   private void renderDenseChunkMarkers(Render3DEvent event) {
      if (!this.denseChunks.isEmpty()) {
         int rendered = 0;
         double halfSize = Math.max(2.0, (Double)this.denseChunkMarkerSize.get() / 2.0);
         double thickness = Math.max(0.01, (Double)this.denseChunkMarkerThickness.get());

         for (BaseDetector.DenseChunkDetection detection : this.denseChunks.values()) {
            if (rendered >= (Integer)this.maxDenseChunkMarkers.get()) {
               break;
            }

            double centerX = (double)detection.chunkPos.getStartX() + 8.0;
            double centerZ = (double)detection.chunkPos.getStartZ() + 8.0;
            double y = detection.markerY;
            event.renderer
               .box(
                  centerX - halfSize,
                  y,
                  centerZ - halfSize,
                  centerX + halfSize,
                  y + thickness,
                  centerZ + halfSize,
                  (Color)this.denseChunkFillColor.get(),
                  (Color)this.denseChunkLineColor.get(),
                  ShapeMode.Both,
                  0
               );
            rendered++;
         }
      }
   }

   private void renderDenseChunkTracers(Render3DEvent event) {
      if (!this.denseChunks.isEmpty()) {
         int rendered = 0;

         for (BaseDetector.DenseChunkDetection detection : this.denseChunks.values()) {
            if (rendered >= (Integer)this.maxDenseChunkMarkers.get()) {
               break;
            }

            event.renderer
               .line(
                  RenderUtils.center.x,
                  RenderUtils.center.y,
                  RenderUtils.center.z,
                  (double)detection.chunkPos.getStartX() + 8.0,
                  detection.markerY + Math.max(0.01, (Double)this.denseChunkMarkerThickness.get()),
                  (double)detection.chunkPos.getStartZ() + 8.0,
                  (Color)this.denseChunkTracerColor.get()
               );
            rendered++;
         }
      }
   }

   private void renderSpawnerChunks(Render3DEvent event) {
      int bottomY = this.mc.world.getBottomY();
      int topY = this.mc.world.getBottomY() + this.mc.world.getHeight();

      for (ChunkPos chunkPos : this.spawnerChunks) {
         event.renderer
            .box(
               (double)chunkPos.getStartX(),
               (double)bottomY,
               (double)chunkPos.getStartZ(),
               (double)(chunkPos.getEndX() + 1),
               (double)topY,
               (double)(chunkPos.getEndZ() + 1),
               (Color)this.spawnerChunkFillColor.get(),
               (Color)this.spawnerChunkLineColor.get(),
               ShapeMode.Both,
               0
            );
      }
   }

   private void renderSpawnerTracer(Render3DEvent event, BlockPos pos) {
      event.renderer
         .line(
            RenderUtils.center.x,
            RenderUtils.center.y,
            RenderUtils.center.z,
            (double)pos.getX() + 0.5,
            (double)pos.getY() + 0.5,
            (double)pos.getZ() + 0.5,
            (Color)this.spawnerTracerColor.get()
         );
   }

   private void renderRainbowBox(Render3DEvent event, Box box, BlockPos pos) {
      int alpha = (Integer)this.rainbowAlpha.get();
      float posOff = (float)((pos.getX() * 73 ^ pos.getY() * 179 ^ pos.getZ() * 31) & 0xFF) / 255.0F * 360.0F;
      float hue = (this.rainbowHue + posOff) % 360.0F;
      int rgbLine = java.awt.Color.HSBtoRGB(hue / 360.0F, 1.0F, 1.0F);
      Color line = new Color(rgbLine >> 16 & 0xFF, rgbLine >> 8 & 0xFF, rgbLine & 0xFF, alpha);
      int rgbFill = java.awt.Color.HSBtoRGB((hue + 60.0F) % 360.0F / 360.0F, 1.0F, 1.0F);
      Color fill = new Color(rgbFill >> 16 & 0xFF, rgbFill >> 8 & 0xFF, rgbFill & 0xFF, Math.max(15, alpha / 8));
      event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, fill, line, ShapeMode.Both, 0);
   }

   private void clearScanCaches() {
      this.denseChunks.clear();
      this.spawnerPositions.clear();
      this.announcedSpawners.clear();
      this.announcedDenseChunks.clear();
      this.spawnerChunks.clear();
   }

   private static class DenseChunkDetection {
      private final ChunkPos chunkPos;
      private final int count;
      private final double markerY;
      private final long detectedAtMillis;

      private DenseChunkDetection(ChunkPos chunkPos, int count, double markerY, long detectedAtMillis) {
         this.chunkPos = chunkPos;
         this.count = count;
         this.markerY = markerY;
         this.detectedAtMillis = detectedAtMillis;
      }
   }
}
