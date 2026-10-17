package com.example.addon.spawner;

import com.example.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementRewards;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.text.MutableText;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementRequirements;
import org.joml.Vector3d;

public class SpawnerNotifierModule extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgNotify = this.settings.createGroup("Notify");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final SettingGroup sgPerf = this.settings.createGroup("Performance");
   private final Setting<Integer> range = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("range")).description("Horizontal search radius around you (block entities only in loaded chunks)."))
               .defaultValue(64))
            .min(8)
            .sliderMax(128)
            .build()
      );
   private final Setting<SpawnerNotifierModule.Theme> theme = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("style"))
                  .description("Visual style for the spawner notifier."))
               .defaultValue(SpawnerNotifierModule.Theme.ModernBlack))
            .build()
      );
   public final Setting<SettingColor> colour = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("custom-colour"))
               .description("Custom colour for boxes, tracers, and world pill tint (override theme)."))
            .defaultValue(new SettingColor(225, 65, 65, 200))
            .build()
      );
   private final Setting<Boolean> disconnectOnFind = this.sgNotify
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("disconnect-on-find"))
                  .description("Disconnect from the server the first time a new spawner is detected while this module is on."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> tracers = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("tracers"))
                  .description("Draw lines from your crosshair aim center to each spawner in range."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> popup = this.sgNotify
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("popup"))
                  .description("Optional duplicate in chat when a spawner is first seen."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> advancementToast = this.sgNotify
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("advancement-toast"))
                  .description("Show the vanilla achievement-style toast (top-right) when a new spawner is found."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> worldLabels = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("world-labels"))
                  .description("Mob head icon centered in the spawner block; large mob type name below the block."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> labelScale = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("label-scale"))
                  .description("Nametag scale for world labels."))
               .visible(this.worldLabels::get))
            .defaultValue(1.0)
            .min(0.5)
            .sliderMax(2.5)
            .build()
      );
   private final Setting<Boolean> drawBox = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("box"))
                  .description("Draw a box on spawner blocks."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SpawnerNotifierModule.BoxShape> boxShape = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("box-shape"))
                     .description("Filled boxes cost more GPU. Lines is recommended on low-end PCs."))
                  .defaultValue(SpawnerNotifierModule.BoxShape.Lines))
               .visible(this.drawBox::get))
            .build()
      );
   private final Setting<Integer> scanInterval = this.sgPerf
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("scan-interval"))
                  .description("Ticks between spawner scans. 1 = every tick (constant scan). Higher saves CPU."))
               .defaultValue(1))
            .range(1, 20)
            .sliderRange(1, 10)
            .build()
      );
   private final Setting<Integer> maxWorldLabels = this.sgPerf
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("max-world-labels")).description("Cap on world [Type] labels drawn per frame. 0 = no limit."))
               .defaultValue(14))
            .range(0, 64)
            .sliderRange(0, 32)
            .build()
      );
   private final Setting<Integer> labelMaxDistance = this.sgPerf
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("label-max-distance")).description("Skip world labels beyond this distance (blocks)."))
               .defaultValue(56))
            .min(12)
            .sliderMax(96)
            .build()
      );
   private final Setting<Integer> maxTracers = this.sgPerf
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("max-tracers")).description("Max tracer lines per frame (nearest first). 0 = no limit."))
               .defaultValue(20))
            .range(0, 128)
            .sliderRange(0, 48)
            .build()
      );
   private final Set<Long> announced = new HashSet<>();
   private int scanTickCounter;
   private static final int JOIN_GRACE_TICKS = 40;
   private static final float FACE_IN_BLOCK_SCALE = 4.6F;
   private static final double TYPE_BELOW_SCALE = 1.75;
   private static final double TYPE_BELOW_Y_OFFSET = 0.58;
   private final Color side = new Color(0, 0, 0, 0);
   private final Color line = new Color(0, 0, 0, 0);
   private final Color fill = new Color(0, 0, 0, 0);
   private final Color textCol = new Color(255, 255, 255, 255);
   private final Vector3d faceAnchor = new Vector3d();
   private final Vector3d typeAnchor = new Vector3d();

   public SpawnerNotifierModule() {
      super(AddonTemplate.F, "spawner-notifier", "Spawner ESP with chunk-scoped scans, throttling, and caps tuned for weaker PCs.");
   }

   private SettingColor getThemeColor() {
      SpawnerNotifierModule.Theme t = (SpawnerNotifierModule.Theme)this.theme.get();
      return new SettingColor(t.lineR, t.lineG, t.lineB, t.lineA);
   }

   public void onActivate() {
      this.announced.clear();
      this.scanTickCounter = 0;
   }

   public void onDeactivate() {
      SpawnerNotifierData.setTracked(List.of());
   }

   @EventHandler
   private void on(Post event) {
      if (this.isActive()) {
         if (Utils.canUpdate() && this.mc.world != null && this.mc.player != null) {
            if (this.mc.player.age >= 40) {
               if (this.scanTickCounter++ % (Integer)this.scanInterval.get() == 0) {
                  int r = (Integer)this.range.get();
                  int rSq = r * r;
                  BlockPos playerPos = this.mc.player.getBlockPos();
                  World world = this.mc.world;
                  List<SpawnerNotifierData.TrackedSpawner> list = new ArrayList<>();
                  this.collectSpawnersNearPlayer(world, playerPos, r, rSq, list);
                  list.sort((a, b) -> Double.compare(a.distanceBlocks(), b.distanceBlocks()));
                  SpawnerNotifierData.setTracked(list);
               }
            }
         }
      }
   }

   private void collectSpawnersNearPlayer(World world, BlockPos playerPos, int r, int rSq, List<SpawnerNotifierData.TrackedSpawner> list) {
      int px = playerPos.getX();
      int pz = playerPos.getZ();
      int minCx = px - r >> 4;
      int maxCx = px + r >> 4;
      int minCz = pz - r >> 4;
      int maxCz = pz + r >> 4;

      for (int cx = minCx; cx <= maxCx; cx++) {
         for (int cz = minCz; cz <= maxCz; cz++) {
            if (world.isChunkLoaded(cx, cz)) {
               WorldChunk chunk = world.getChunk(cx, cz);

               for (BlockEntity be : chunk.getBlockEntities().values()) {
                  if (be instanceof MobSpawnerBlockEntity) {
                     MobSpawnerBlockEntity spawner = (MobSpawnerBlockEntity)be;
                     BlockPos pos = spawner.getPos();
                     double distSq = PlayerUtils.squaredDistanceTo(
                        (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5
                     );
                     if (!(distSq > (double)rSq)) {
                        Entity rendered = spawner.getLogic().getRenderedEntity(world, pos);
                        EntityType<?> spawnedType = rendered != null ? rendered.getType() : null;
                        String mob = rendered != null ? rendered.getType().getName().getString() : "Unknown";
                        list.add(new SpawnerNotifierData.TrackedSpawner(pos, mob, Math.sqrt(distSq), spawnedType));
                        long key = pos.asLong();
                        if (!this.announced.contains(key)) {
                           this.announced.add(key);
                           this.onFirstSeen(pos, mob, spawnedType);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void onFirstSeen(BlockPos pos, String mob, EntityType<?> spawnedType) {
      String blockName = Blocks.SPAWNER.getName().getString();
      String summary = blockName + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " — " + mob;
      SpawnerNotifierData.pushHidLine(summary);
      if ((Boolean)this.popup.get()) {
         MutableText t = Text.literal("");
         t.append(Text.literal("[Spawner Notifier] ").styled(s -> s.withColor(16755285)));
         t.append(Text.literal(summary));
         this.info(t);
      }

      if ((Boolean)this.advancementToast.get()) {
         this.showAdvancementToast(pos, mob, blockName, spawnedType);
      }

      if ((Boolean)this.disconnectOnFind.get()) {
         ClientPlayNetworkHandler net = this.mc.getNetworkHandler();
         if (net != null && net.getConnection() != null) {
            net.getConnection().disconnect(Text.literal("[Spawner Notifier] Found: " + summary));
         }
      }
   }

   private static ItemStack displayHeadForType(EntityType<?> type) {
      if (type == null) {
         return new ItemStack(Items.SKELETON_SKULL);
      } else if (type == EntityType.IRON_GOLEM) {
         return new ItemStack(Items.SKELETON_SKULL);
      } else if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED || type == EntityType.ZOMBIE_VILLAGER) {
         return new ItemStack(Items.ZOMBIE_HEAD);
      } else if (type == EntityType.CREEPER) {
         return new ItemStack(Items.CREEPER_HEAD);
      } else if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.BOGGED) {
         return new ItemStack(Items.SKELETON_SKULL);
      } else if (type == EntityType.WITHER_SKELETON) {
         return new ItemStack(Items.WITHER_SKELETON_SKULL);
      } else if (type == EntityType.ENDER_DRAGON) {
         return new ItemStack(Items.DRAGON_HEAD);
      } else {
         return type != EntityType.PIGLIN && type != EntityType.PIGLIN_BRUTE && type != EntityType.ZOMBIFIED_PIGLIN
            ? new ItemStack(Items.SKELETON_SKULL)
            : new ItemStack(Items.PIGLIN_HEAD);
      }
   }

   private void showAdvancementToast(BlockPos pos, String mob, String blockName, EntityType<?> spawnedType) {
      if (this.mc.getToastManager() != null) {
         Text title = Text.literal("Spawner").formatted(Formatting.GREEN);
         Text desc = Text.literal(mob + " · " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " · " + blockName);
         AdvancementDisplay display = new AdvancementDisplay(displayHeadForType(spawnedType), title, desc, Optional.empty(), AdvancementFrame.TASK, true, false, false);
         Advancement advancement = new Advancement(
            Optional.empty(), Optional.of(display), AdvancementRewards.NONE, Collections.emptyMap(), AdvancementRequirements.EMPTY, false, Optional.empty()
         );
         Identifier id = Identifier.of("spawner_notifier", "notify_" + Long.toHexString(pos.asLong()));
         this.mc.getToastManager().add(new AdvancementToast(new AdvancementEntry(id, advancement)));
      }
   }

   @EventHandler
   private void on(Render3DEvent event) {
      if (this.isActive() && Utils.canUpdate() && this.mc.world != null) {
         SpawnerNotifierModule.Theme t = (SpawnerNotifierModule.Theme)this.theme.get();
         this.line.set(t.lineR, t.lineG, t.lineB, t.lineA);
         this.side.set(t.lineR, t.lineG, t.lineB, t.lineA);
         this.side.a = Math.min(t.lineA, 120);
         ShapeMode shape = this.boxShape.get() == SpawnerNotifierModule.BoxShape.Both ? ShapeMode.Both : ShapeMode.Lines;
         int maxT = (Integer)this.maxTracers.get();
         int tracersDrawn = 0;

         for (SpawnerNotifierData.TrackedSpawner e : SpawnerNotifierData.getTracked()) {
            BlockPos pos = e.pos();
            if ((Boolean)this.drawBox.get()) {
               event.renderer.box(pos, this.side, this.line, shape, 0);
            }

            if ((Boolean)this.tracers.get() && (maxT == 0 || tracersDrawn < maxT)) {
               event.renderer
                  .line(
                     RenderUtils.center.x,
                     RenderUtils.center.y,
                     RenderUtils.center.z,
                     (double)pos.getX() + 0.5,
                     (double)pos.getY() + 0.5,
                     (double)pos.getZ() + 0.5,
                     this.line
                  );
               tracersDrawn++;
            }
         }
      }
   }

   @EventHandler
   private void on(Render2DEvent event) {
      if (this.isActive()) {
         if ((Boolean)this.worldLabels.get() && Utils.canUpdate() && this.mc.world != null) {
            TextRenderer textRenderer = TextRenderer.get();
            double labelMax = (double)((Integer)this.labelMaxDistance.get()).intValue();
            int maxLabels = (Integer)this.maxWorldLabels.get();
            int drawn = 0;
            double nameScale = (Double)this.labelScale.get() - 0.2;
            DrawContext ctx = event.drawContext;

            for (SpawnerNotifierData.TrackedSpawner e : SpawnerNotifierData.getTracked()) {
               if (!(e.distanceBlocks() > labelMax)) {
                  if (maxLabels > 0 && drawn >= maxLabels) {
                     break;
                  }

                  drawn++;
                  BlockPos pos = e.pos();
                  double cx = (double)pos.getX() + 0.5;
                  double cy = (double)pos.getY() + 0.5;
                  double cz = (double)pos.getZ() + 0.5;
                  this.faceAnchor.set(cx, cy, cz);
                  if (NametagUtils.to2D(this.faceAnchor, nameScale)) {
                     NametagUtils.begin(this.faceAnchor, ctx);
                     ItemStack head = displayHeadForType(e.spawnedType());
                     int hx = Math.round(-36.8F);
                     int hy = Math.round(-36.8F);
                     RenderUtils.drawItem(ctx, head, hx, hy, 4.6F, false);
                     NametagUtils.end(ctx);
                  }

                  this.typeAnchor.set(cx, (double)pos.getY() - 0.58, cz);
                  if (NametagUtils.to2D(this.typeAnchor, nameScale)) {
                     NametagUtils.begin(this.typeAnchor, ctx);
                     String mobLine = "[" + e.mobLabel() + "]";
                     textRenderer.begin(1.75);
                     double tw = textRenderer.getWidth(mobLine, true);
                     double th = textRenderer.getHeight(true);
                     double padX = 8.0;
                     double padY = 5.0;
                     double pillW = tw + padX * 2.0;
                     double pillH = th + padY * 2.0;
                     double bx = -pillW / 2.0;
                     double by = -pillH / 2.0;
                     SpawnerNotifierModule.Theme t = (SpawnerNotifierModule.Theme)this.theme.get();
                     this.fill.set(t.fillR, t.fillG, t.fillB, Math.min(220, t.fillA + 40));
                     Renderer2D.COLOR.begin();
                     RoundedPill.drawWorld(Renderer2D.COLOR, bx, by, pillW, pillH, this.fill);
                     Renderer2D.COLOR.render();
                     textRenderer.render(mobLine, -tw / 2.0, -th / 2.0, this.textCol, true);
                     textRenderer.end();
                     NametagUtils.end(ctx);
                  }
               }
            }
         }
      }
   }

   public String getInfoString() {
      return Integer.toString(SpawnerNotifierData.getTracked().size());
   }

   public static enum BoxShape {
      Lines,
      Both;

      @Override
      public String toString() {
         return this == Lines ? "Lines (fast)" : "Filled";
      }
   }

   public static enum Theme {
      ModernBlack("Modern Black", 20, 20, 20, 200, 30, 30, 30, 220),
      ChillRed("Chill Red", 139, 50, 50, 200, 180, 80, 80, 220);

      public final String displayName;
      public final int lineR;
      public final int lineG;
      public final int lineB;
      public final int lineA;
      public final int fillR;
      public final int fillG;
      public final int fillB;
      public final int fillA;

      private Theme(String displayName, int lineR, int lineG, int lineB, int lineA, int fillR, int fillG, int fillB, int fillA) {
         this.displayName = displayName;
         this.lineR = lineR;
         this.lineG = lineG;
         this.lineB = lineB;
         this.lineA = lineA;
         this.fillR = fillR;
         this.fillG = fillG;
         this.fillB = fillB;
         this.fillA = fillA;
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }
}
