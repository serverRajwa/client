package dev.dexter.kryptionians.modules;

import dev.dexter.kryptionians.KryptioniansAddon;
import dev.dexter.kryptionians.ui.SpawnerNotifierToast;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.joml.Vector3d;

public final class SpawnerNotifier extends Module {
  private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
  private final SettingGroup sgRender = this.settings.createGroup("Render");
  private final SettingGroup sgAlerts = this.settings.createGroup("Alerts");
  private final Setting<Integer> range =
      this.sgGeneral.add(
          new IntSetting.Builder().name("range").defaultValue(256).min(8).sliderMax(512).build());
  public final Setting<SettingColor> pillarColor =
      this.sgRender.add(
          new ColorSetting.Builder()
              .name("pillar-color")
              .defaultValue(new SettingColor(255, 0, 0, 80))
              .build());
  private final Setting<Integer> pillarOpacity =
      this.sgRender.add(
          new IntSetting.Builder()
              .name("pillar-opacity")
              .defaultValue(80)
              .range(0, 255)
              .sliderMax(255)
              .build());
  private final Setting<Boolean> spawnerEsp =
      this.sgRender.add(new BoolSetting.Builder().name("spawner-esp").defaultValue(true).build());
  private final Setting<SettingColor> spawnerColor =
      this.sgRender.add(
          new ColorSetting.Builder()
              .name("spawner-color")
              .defaultValue(new SettingColor(255, 0, 0, 150))
              .visible(this.spawnerEsp::get)
              .build());
  private final Setting<Integer> spawnerAlpha =
      this.sgRender.add(
          new IntSetting.Builder()
              .name("spawner-alpha")
              .defaultValue(80)
              .range(0, 255)
              .sliderMax(255)
              .visible(this.spawnerEsp::get)
              .build());
  private final Setting<Double> textScale =
      this.sgRender.add(
          new DoubleSetting.Builder()
              .name("text-scale")
              .defaultValue(1.2)
              .min(0.1)
              .sliderMax(5.0)
              .build());
  private final Setting<ShapeMode> shapeMode =
      this.sgRender.add(
          new EnumSetting.Builder<ShapeMode>()
              .name("render-mode")
              .defaultValue(ShapeMode.Both)
              .build());
  private final Setting<Boolean> toastAlerts =
      this.sgAlerts.add(new BoolSetting.Builder().name("toast-alerts").defaultValue(true).build());
  private final Setting<Integer> toastDuration =
      this.sgAlerts.add(
          new IntSetting.Builder()
              .name("toast-duration")
              .defaultValue(6500)
              .range(2500, 15000)
              .sliderRange(2500, 15000)
              .visible(this.toastAlerts::get)
              .build());
  private final Setting<SettingColor> progressBarColor =
      this.sgAlerts.add(
          new ColorSetting.Builder()
              .name("progress-bar-color")
              .defaultValue(new SettingColor(224, 69, 69, 255))
              .visible(this.toastAlerts::get)
              .build());
  private final Setting<Boolean> chatAlerts =
      this.sgAlerts.add(new BoolSetting.Builder().name("chat-alerts").defaultValue(true).build());
  private final Setting<Boolean> soundAlerts =
      this.sgAlerts.add(new BoolSetting.Builder().name("sound-alerts").defaultValue(true).build());
  private final Map<BlockPos, String> trackedSpawners = new HashMap<>();
  private final Map<BlockPos, String> resolvedTypes = new HashMap<>();
  private final Set<Long> alertedSpawnerKeys = new HashSet<>();
  private final Color WORKING_PILLAR_COLOR = new Color();
  private final Color WORKING_SPAWNER_COLOR = new Color();
  private final Color LABEL_ACCENT = new Color();
  private final Color LABEL_BG = new Color(14, 14, 14, 175);
  private final Color LABEL_TITLE = new Color(255, 255, 255, 255);
  private final Color LABEL_SUB = new Color(184, 168, 232, 220);
  private boolean inWorld;
  private boolean scanNow;
  private int preTickDelay = 0;

  public SpawnerNotifier() {
    super(
        KryptioniansAddon.CATEGORY,
        "spawner-notifier",
        "Highlights spawners with a 3x3 filled vertical pillar, alerts, and text tags.");
  }

  public void onActivate() {
    this.reset();
  }

  public void onDeactivate() {
    this.reset();
  }

  private void reset() {
    this.trackedSpawners.clear();
    this.resolvedTypes.clear();
    this.alertedSpawnerKeys.clear();
    this.preTickDelay = 0;
    this.scanNow = true;
    this.inWorld = false;
  }

  @EventHandler
  private void onGameJoined(GameJoinedEvent event) {
    this.reset();
  }

  @EventHandler
  private void onGameLeft(GameLeftEvent event) {
    this.reset();
  }

  @EventHandler
  private void onTick(Post event) {
    if (this.mc.level != null && this.mc.player != null) {
      if (!this.inWorld) {
        this.alertedSpawnerKeys.clear();
        this.resolvedTypes.clear();
        this.trackedSpawners.clear();
        this.preTickDelay = 0;
        this.scanNow = true;
        this.inWorld = true;
      }

      if (this.scanNow) {
        this.scanNow = false;
      } else {
        this.preTickDelay++;
        if (this.preTickDelay < 20) {
          return;
        }

        this.preTickDelay = 0;
      }

      int r = this.range.get();
      int rSq = r * r;
      BlockPos playerPos = this.mc.player.blockPosition();
      Level world = this.mc.level;
      Map<BlockPos, String> currentSpawners = new HashMap<>();
      Set<Long> seenKeys = new HashSet<>();
      int px = playerPos.getX();
      int pz = playerPos.getZ();
      int minCx = px - r >> 4;
      int maxCx = px + r >> 4;
      int minCz = pz - r >> 4;
      int maxCz = pz + r >> 4;

      for (int cx = minCx; cx <= maxCx; cx++) {
        for (int cz = minCz; cz <= maxCz; cz++) {
          if (world.getChunkSource().hasChunk(cx, cz)) {
            LevelChunk chunk = world.getChunk(cx, cz);

            for (BlockEntity be : chunk.getBlockEntities().values()) {
              if (be instanceof SpawnerBlockEntity) {
                SpawnerBlockEntity spawner = (SpawnerBlockEntity) be;
                BlockPos pos = spawner.getBlockPos();
                double distSq =
                    PlayerUtils.squaredDistanceTo(
                        (double) pos.getX() + 0.5,
                        (double) pos.getY() + 0.5,
                        (double) pos.getZ() + 0.5);
                if (distSq <= (double) rSq) {
                  String type = this.spawnerType(spawner, pos);
                  currentSpawners.put(pos, type);
                  seenKeys.add(pos.asLong());
                  long spawnerKey = pos.asLong();
                  if (!this.alertedSpawnerKeys.contains(spawnerKey)) {
                    this.alertedSpawnerKeys.add(spawnerKey);
                    if (this.toastAlerts.get() && this.mc.getToastManager() != null) {
                      SettingColor bar = (SettingColor) this.progressBarColor.get();
                      this.mc
                          .getToastManager()
                          .addToast(
                              new SpawnerNotifierToast(
                                  type,
                                  pos.getX(),
                                  pos.getY(),
                                  pos.getZ(),
                                  (long) this.toastDuration.get(),
                                  this.soundAlerts.get(),
                                  bar.r << 16 | bar.g << 8 | bar.b));
                    } else if (this.soundAlerts.get()) {
                      this.mc.player.playSound(
                          (SoundEvent) SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 0.8F);
                    }

                    if (this.chatAlerts.get()) {
                      this.mc.player.displayClientMessage(
                          Component.literal(
                              "\u00a7c[Spawner Notifier] "
                                  + type
                                  + " Spawner At X: "
                                  + pos.getX()
                                  + " Z: "
                                  + pos.getZ()),
                          false);
                    }
                  }
                }
              }
            }
          }
        }
      }

      this.alertedSpawnerKeys.removeIf(key -> !seenKeys.contains(key));
      this.resolvedTypes.keySet().removeIf(posx -> !seenKeys.contains(posx.asLong()));
      this.trackedSpawners.clear();
      this.trackedSpawners.putAll(currentSpawners);
    } else {
      this.inWorld = false;
    }
  }

  private String spawnerType(SpawnerBlockEntity spawner, BlockPos pos) {
    String type = this.readType(spawner);
    String cached = this.resolvedTypes.get(pos);
    if (this.badType(type) && cached != null && !this.badType(cached)) {
      return cached;
    } else if (!this.badType(type)) {
      this.resolvedTypes.put(pos, type);
      return type;
    } else {
      return cached != null ? cached : type;
    }
  }

  private boolean badType(String type) {
    return type == null || type.isEmpty() || "Unknown".equals(type) || "Pig".equals(type);
  }

  private String readType(SpawnerBlockEntity spawner) {
    try {
      if (this.mc.level != null) {
        String fromNbt = this.typeFromNbt(spawner.getUpdateTag(this.mc.level.registryAccess()));
        if (fromNbt != null) {
          return fromNbt;
        }

        fromNbt = this.typeFromNbt(spawner.saveWithoutMetadata(this.mc.level.registryAccess()));
        if (fromNbt != null) {
          return fromNbt;
        }
      }

      BaseSpawner logic = spawner.getSpawner();
      if (logic != null) {
        Entity entity = logic.getOrCreateDisplayEntity(this.mc.level, spawner.getBlockPos());
        if (entity != null) {
          return entity.getType().getDescription().getString();
        }
      }
    } catch (Exception ignored) {
    }

    return "Unknown";
  }

  private String typeFromNbt(CompoundTag nbt) {
    if (nbt == null) {
      return null;
    } else {
      for (String key : new String[] {"SpawnData", "spawn_data"}) {
        if (nbt.contains(key)) {
          CompoundTag spawnData = (CompoundTag) nbt.getCompound(key).orElse(null);
          if (spawnData != null) {
            if (spawnData.contains("entity")) {
              String name =
                  this.nameFromEntityNbt(
                      (CompoundTag) spawnData.getCompound("entity").orElse(null));
              if (name != null) {
                return name;
              }
            }

            String name = this.nameFromEntityNbt(spawnData);
            if (name != null) {
              return name;
            }
          }
        }
      }

      return null;
    }
  }

  private String nameFromEntityNbt(CompoundTag entityNbt) {
    if (entityNbt != null && entityNbt.contains("id")) {
      String rawId = entityNbt.getString("id").orElse("");
      if (rawId.isEmpty()) {
        return null;
      } else {
        EntityType<?> entityType =
            (EntityType<?>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(rawId));
        return entityType != null ? entityType.getDescription().getString() : null;
      }
    } else {
      return null;
    }
  }

  @EventHandler
  private void onRender3D(Render3DEvent event) {
    if (this.mc.level != null && !this.trackedSpawners.isEmpty() && this.mc.player != null) {
      this.WORKING_PILLAR_COLOR.set((Color) this.pillarColor.get());
      this.WORKING_PILLAR_COLOR.a = this.pillarOpacity.get();
      this.WORKING_SPAWNER_COLOR.set((Color) this.spawnerColor.get());
      this.WORKING_SPAWNER_COLOR.a = this.spawnerAlpha.get();

      for (Entry<BlockPos, String> entry : this.trackedSpawners.entrySet()) {
        BlockPos pos = entry.getKey();
        double x = (double) pos.getX();
        double y = (double) pos.getY();
        double z = (double) pos.getZ();
        event.renderer.box(
            x - 1.0,
            -60.0,
            z - 1.0,
            x + 2.0,
            150.0,
            z + 2.0,
            this.WORKING_PILLAR_COLOR,
            this.WORKING_PILLAR_COLOR,
            (ShapeMode) this.shapeMode.get(),
            0);
        if (this.spawnerEsp.get()) {
          event.renderer.box(
              x,
              y,
              z,
              x + 1.0,
              y + 1.0,
              z + 1.0,
              this.WORKING_SPAWNER_COLOR,
              this.WORKING_SPAWNER_COLOR,
              (ShapeMode) this.shapeMode.get(),
              0);
        }
      }
    }
  }

  @EventHandler
  private void onRender2D(Render2DEvent event) {
    if (this.mc.level != null && !this.trackedSpawners.isEmpty()) {
      SettingColor accent = (SettingColor) this.progressBarColor.get();
      this.LABEL_ACCENT.set(accent.r, accent.g, accent.b, 255);

      for (Entry<BlockPos, String> entry : this.trackedSpawners.entrySet()) {
        BlockPos pos = entry.getKey();
        Vector3d textPos =
            new Vector3d(
                (double) pos.getX() + 0.5, (double) pos.getY() + 1.35, (double) pos.getZ() + 0.5);
        this.renderSpawnerLabel(textPos, entry.getValue(), pos);
      }
    }
  }

  private void renderSpawnerLabel(Vector3d textPos, String mobType, BlockPos pos) {
    if (NametagUtils.to2D(textPos, this.textScale.get())) {
      NametagUtils.begin(textPos);
      TextRenderer text = TextRenderer.get();
      text.begin(1.0, false, true);
      String tag = "Spawner";
      int dist =
          this.mc.player != null
              ? (int)
                  Math.sqrt(
                      this.mc.player.distanceToSqr(
                          (double) pos.getX() + 0.5,
                          (double) pos.getY() + 0.5,
                          (double) pos.getZ() + 0.5))
              : 0;
      String distLabel = dist + "m";
      double mobW = text.getWidth(mobType);
      double gapW = text.getWidth(" ");
      double tagW = text.getWidth(tag);
      double sepW = text.getWidth(" \u00b7 ");
      double distW = text.getWidth(distLabel);
      double contentW = mobW + gapW + tagW + sepW + distW;
      double lineH = text.getHeight();
      double padX = 6.0;
      double padY = 3.0;
      double boxW = contentW + padX * 2.0;
      double boxH = lineH + padY * 2.0;
      double left = -boxW / 2.0;
      double top = -padY;
      Renderer2D.COLOR.begin();
      Renderer2D.COLOR.quad(left, top, boxW, boxH, this.LABEL_BG);
      Renderer2D.COLOR.quad(left, top, 2.0, boxH, this.LABEL_ACCENT);
      Renderer2D.COLOR.render();
      double x = -contentW / 2.0;
      text.render(mobType, x, 0.0, this.LABEL_TITLE);
      x += mobW + gapW;
      text.render(tag, x, 0.0, this.LABEL_ACCENT);
      x += tagW + sepW;
      text.render(distLabel, x, 0.0, this.LABEL_SUB);
      text.end();
      NametagUtils.end();
    }
  }

  public String getInfoString() {
    return Integer.toString(this.trackedSpawners.size());
  }
}
