package com.example.addon.spawner;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
public final class SpawnerNotifierData {
   private static volatile List<SpawnerNotifierData.TrackedSpawner> tracked = List.of();
   public static final CopyOnWriteArrayList<String> HID_FEED = new CopyOnWriteArrayList<>();

   private SpawnerNotifierData() {
   }

   public static List<SpawnerNotifierData.TrackedSpawner> getTracked() {
      return tracked;
   }

   public static void setTracked(List<SpawnerNotifierData.TrackedSpawner> next) {
      tracked = next.isEmpty() ? List.of() : List.copyOf(next);
   }

   public static void pushHidLine(String line) {
      HID_FEED.add(0, line);

      while (HID_FEED.size() > 12) {
         HID_FEED.remove(HID_FEED.size() - 1);
      }
   }

   public static void clearHidFeed() {
      HID_FEED.clear();
   }

   public static record TrackedSpawner(BlockPos pos, String mobLabel, double distanceBlocks, EntityType<?> spawnedType) {
   }
}
