package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ColorSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.world.chunk.WorldChunk;
public class SpawnerChunks extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<SettingColor> sideColor = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("side-color")).description("Fill color for spawner chunks."))
            .defaultValue(new SettingColor(255, 0, 0, 60))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("line-color")).description("Outline color for spawner chunks."))
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("shape-mode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<Integer> rescanInterval = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("rescan-ticks"))
                  .description("How often (in ticks) to rescan nearby chunks for spawners."))
               .defaultValue(20))
            .min(5)
            .sliderMax(100)
            .build()
      );
   private final Set<ChunkPos> spawnerChunks = new HashSet<>();
   private int tickCounter = 0;

   public SpawnerChunks() {
      super(AddonTemplate.F, "spawner-chunks", "Highlights chunks that contain a mob spawner.");
   }

   public void onActivate() {
      this.spawnerChunks.clear();
      this.rescan();
   }

   @EventHandler
   private void on(GameLeftEvent event) {
   }

   @EventHandler
   private void on(Post event) {
      this.tickCounter++;
      if (this.tickCounter >= (Integer)this.rescanInterval.get()) {
         this.tickCounter = 0;
         this.rescan();
      }
   }

   @EventHandler
   private void on(Render3DEvent event) {
      for (ChunkPos pos : this.spawnerChunks) {
         event.renderer
            .box(
               (double)pos.getStartX(),
               (double)this.mc.world.getBottomY(),
               (double)pos.getStartZ(),
               (double)(pos.getEndX() + 1),
               (double)(this.mc.world.getBottomY() + this.mc.world.getHeight()),
               (double)(pos.getEndZ() + 1),
               (Color)this.sideColor.get(),
               (Color)this.lineColor.get(),
               (ShapeMode)this.shapeMode.get(),
               0
            );
      }
   }

   private void rescan() {
      if (this.mc.world != null && this.mc.player != null) {
         this.spawnerChunks.clear();
         int viewDist = (Integer)this.mc.options.getViewDistance().getValue();
         ChunkPos playerChunk = this.mc.player.getChunkPos();

         for (int cx = playerChunk.x - viewDist; cx <= playerChunk.x + viewDist; cx++) {
            for (int cz = playerChunk.z - viewDist; cz <= playerChunk.z + viewDist; cz++) {
               WorldChunk chunk = this.mc.world.getChunk(cx, cz);
               boolean hasSpawner = chunk.getBlockEntities().values().stream().anyMatch(be -> be instanceof MobSpawnerBlockEntity);
               if (hasSpawner) {
                  this.spawnerChunks.add(new ChunkPos(cx, cz));
               }
            }
         }
      }
   }
}
