package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.world.chunk.WorldChunk;
public class ChestCounter extends Module {
   private final SettingGroup settingGroup = this.settings.getDefaultGroup();
   private final Setting<Integer> scanInterval = this.settingGroup
      .add(((Builder)((Builder)new Builder().name("scan-interval")).defaultValue(40)).min(10).sliderMax(200).build());
   private final Setting<ChestCounter.NotifyMode> mode = this.settingGroup
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("notify-mode"))
               .defaultValue(ChestCounter.NotifyMode.Both))
            .build()
      );
   private int tick = 0;
   private int lastCount = -1;
   private final Map<Long, Integer> chunkChestCounts = new HashMap<>();

   public ChestCounter() {
      super(AddonTemplate.F, "chest-counter", "Counts chests.");
   }

   public void onActivate() {
      this.tick = 0;
      this.lastCount = -1;
      this.chunkChestCounts.clear();
   }

   public void onDeactivate() {
      this.chunkChestCounts.clear();
   }

   private void notify(String t, String str) {
      ChestCounter.NotifyMode m = (ChestCounter.NotifyMode)this.mode.get();
      if (m == ChestCounter.NotifyMode.Toast || m == ChestCounter.NotifyMode.Both) {
         MeteorToast toast = new meteordevelopment.meteorclient.utils.render.MeteorToast.Builder(t).text(str).icon(Items.CHEST).build();
         this.mc.getToastManager().add(toast);
      }

      if (m == ChestCounter.NotifyMode.Chat || m == ChestCounter.NotifyMode.Both) {
         this.info(t + " | " + str, new Object[0]);
      }
   }

   @EventHandler
   private void onE(Post e) {
      this.tick++;
      if (this.tick >= (Integer)this.scanInterval.get()) {
         this.tick = 0;
         if (this.mc.world != null && this.mc.player != null) {
            int r = (Integer)this.mc.options.getViewDistance().getValue();
            ChunkPos cp = this.mc.player.getChunkPos();
            int total = 0;

            for (int dx = -r; dx <= r; dx++) {
               for (int dz = -r; dz <= r; dz++) {
                  WorldChunk c = this.mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
                  if (c != null) {
                     long key = ChunkPos.toLong(c.getPos().x, c.getPos().z);
                     int cc = 0;

                     for (BlockEntity be : c.getBlockEntities().values()) {
                        if (be instanceof ChestBlockEntity) {
                           BlockPos p = be.getPos();
                           if (p.getY() < this.mc.player.getBlockY() - 5 || p.getY() < 60) {
                              cc++;
                           }
                        }
                     }

                     this.chunkChestCounts.put(key, cc);
                     total += cc;
                  }
               }
            }

            if (total > 0 && total != this.lastCount) {
               this.lastCount = total;
               this.notify("Chest Counter", total + " Storage");
            }
         }
      }
   }

   @EventHandler
   private void onE(ChunkDataEvent e) {
      this.lastCount = -1;
   }

   public static enum NotifyMode {
      Chat,
      Toast,
      Both;
   }
}
