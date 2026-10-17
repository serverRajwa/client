package com.example.addon.modules;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.util.math.ChunkPos;
final class CentralBaseScorer {
   private static final int CHUNK_SIZE = 16;
   private static final int MAX_POINTS_PER_CHUNK = 160;
   private static final int SUBMIT_COOLDOWN_TICKS = 4;
   private static final ConcurrentHashMap<Long, ConcurrentLinkedDeque<CentralBaseScorer.EvidencePoint>> POINTS_BY_CHUNK = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<Long> DIRTY_CHUNKS = new ConcurrentLinkedQueue<>();
   private static final Set<Long> QUEUED_DIRTY_CHUNKS = ConcurrentHashMap.newKeySet();
   private static final ConcurrentHashMap<Long, Integer> LAST_SUBMIT_TICK = new ConcurrentHashMap<>();

   private CentralBaseScorer() {
   }

   static void submit(CentralBaseScorer.EvidenceType type, double x, double y, double z, int tick, double weight, String reason) {
      if (type != null && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
         if (!(y < -64.0) && !(y > 320.0)) {
            long chunkKey = chunkKey(x, z);
            String normalizedReason = reason != null && !reason.isBlank() ? reason : type.name().toLowerCase(Locale.ROOT);
            long cooldownKey = cooldownKey(chunkKey, type, normalizedReason);
            Integer previous = LAST_SUBMIT_TICK.get(cooldownKey);
            if (previous == null || tick < previous || tick - previous > 4) {
               LAST_SUBMIT_TICK.put(cooldownKey, tick);
               double clampedWeight = Math.max(1.0, Math.min(100.0, weight));
               CentralBaseScorer.EvidencePoint point = new CentralBaseScorer.EvidencePoint(type, x, y, z, tick, clampedWeight, normalizedReason);
               ConcurrentLinkedDeque<CentralBaseScorer.EvidencePoint> queue = POINTS_BY_CHUNK.computeIfAbsent(
                  chunkKey, ignored -> new ConcurrentLinkedDeque<>()
               );
               queue.addLast(point);

               while (queue.size() > 160) {
                  queue.pollFirst();
               }

               markDirty(chunkKey);
            }
         }
      }
   }

   static void submit(CentralBaseScorer.EvidenceType type, double x, double y, double z, int tick, String reason) {
      submit(type, x, y, z, tick, type.defaultWeight, reason);
   }

   static void markNatural(double x, double y, double z, int tick, String reason) {
      submit(CentralBaseScorer.EvidenceType.NATURAL, x, y, z, tick, 100.0, reason);
   }

   static List<Long> pollDirtyChunks(int limit) {
      ArrayList<Long> keys = new ArrayList<>(Math.max(0, limit));

      for (int i = 0; i < limit; i++) {
         Long key = DIRTY_CHUNKS.poll();
         if (key == null) {
            break;
         }

         QUEUED_DIRTY_CHUNKS.remove(key);
         keys.add(key);
      }

      return keys;
   }

   static CentralBaseScorer.Snapshot snapshot(long chunkKey, int nowTick, int maxAgeTicks) {
      ConcurrentLinkedDeque<CentralBaseScorer.EvidencePoint> queue = POINTS_BY_CHUNK.get(chunkKey);
      if (queue == null) {
         return CentralBaseScorer.Snapshot.empty(chunkKey);
      } else {
         while (true) {
            CentralBaseScorer.EvidencePoint first = queue.peekFirst();
            if (first == null || nowTick < first.tick || nowTick - first.tick <= maxAgeTicks) {
               if (queue.isEmpty()) {
                  POINTS_BY_CHUNK.remove(chunkKey, queue);
                  return CentralBaseScorer.Snapshot.empty(chunkKey);
               } else {
                  CentralBaseScorer.SnapshotBuilder builder = new CentralBaseScorer.SnapshotBuilder(chunkKey);

                  for (CentralBaseScorer.EvidencePoint point : queue) {
                     if (nowTick < point.tick || nowTick - point.tick <= maxAgeTicks) {
                        builder.add(point);
                     }
                  }

                  return builder.build();
               }
            }

            queue.pollFirst();
         }
      }
   }

   static void clear() {
      POINTS_BY_CHUNK.clear();
      DIRTY_CHUNKS.clear();
      QUEUED_DIRTY_CHUNKS.clear();
      LAST_SUBMIT_TICK.clear();
   }

   private static void markDirty(long chunkKey) {
      if (QUEUED_DIRTY_CHUNKS.add(chunkKey)) {
         DIRTY_CHUNKS.add(chunkKey);
      }
   }

   private static long chunkKey(double x, double z) {
      int chunkX = (int)Math.floor(x / 16.0);
      int chunkZ = (int)Math.floor(z / 16.0);
      return ChunkPos.toLong(chunkX, chunkZ);
   }

   private static long cooldownKey(long chunkKey, CentralBaseScorer.EvidenceType type, String reason) {
      long h = (long)reason.hashCode() & 4294967295L;
      return chunkKey ^ (long)type.ordinal() << 48 ^ h << 16;
   }

   private static record EvidencePoint(CentralBaseScorer.EvidenceType type, double x, double y, double z, int tick, double weight, String reason) {
   }

   static enum EvidenceType {
      STORAGE(30.0, CentralBaseScorer.Family.STORAGE),
      SHULKER(42.0, CentralBaseScorer.Family.STORAGE),
      HOPPER(26.0, CentralBaseScorer.Family.AUTOMATION),
      REDSTONE(28.0, CentralBaseScorer.Family.AUTOMATION),
      FARM(34.0, CentralBaseScorer.Family.FARM),
      AMETHYST(58.0, CentralBaseScorer.Family.AMETHYST),
      SPAWNER(44.0, CentralBaseScorer.Family.SPAWNER),
      SOUND(20.0, CentralBaseScorer.Family.ACTIVITY),
      PLAYER(30.0, CentralBaseScorer.Family.ACTIVITY),
      MOB(22.0, CentralBaseScorer.Family.MOB),
      ANOMALY(24.0, CentralBaseScorer.Family.ANOMALY),
      NATURAL(100.0, CentralBaseScorer.Family.NATURAL);

      final double defaultWeight;
      final CentralBaseScorer.Family family;

      private EvidenceType(double defaultWeight, CentralBaseScorer.Family family) {
         this.defaultWeight = defaultWeight;
         this.family = family;
      }
   }

   private static enum Family {
      STORAGE,
      AUTOMATION,
      FARM,
      AMETHYST,
      SPAWNER,
      ACTIVITY,
      MOB,
      ANOMALY,
      NATURAL;
   }

   static final class Snapshot {
      final long chunkKey;
      final int points;
      final int lastTick;
      final double totalWeight;
      final double naturalWeight;
      final double storageWeight;
      final double automationWeight;
      final double redstoneWeight;
      final double farmWeight;
      final double amethystWeight;
      final double spawnerWeight;
      final double soundWeight;
      final double playerWeight;
      final double mobWeight;
      final double anomalyWeight;
      final double weightedX;
      final double weightedY;
      final double weightedZ;
      final double centerWeight;
      final int minY;
      final int maxY;
      private final EnumSet<CentralBaseScorer.Family> families;

      private Snapshot(
         long chunkKey,
         int points,
         int lastTick,
         double totalWeight,
         double naturalWeight,
         double storageWeight,
         double automationWeight,
         double redstoneWeight,
         double farmWeight,
         double amethystWeight,
         double spawnerWeight,
         double soundWeight,
         double playerWeight,
         double mobWeight,
         double anomalyWeight,
         double weightedX,
         double weightedY,
         double weightedZ,
         double centerWeight,
         int minY,
         int maxY,
         EnumSet<CentralBaseScorer.Family> families
      ) {
         this.chunkKey = chunkKey;
         this.points = points;
         this.lastTick = lastTick;
         this.totalWeight = totalWeight;
         this.naturalWeight = naturalWeight;
         this.storageWeight = storageWeight;
         this.automationWeight = automationWeight;
         this.redstoneWeight = redstoneWeight;
         this.farmWeight = farmWeight;
         this.amethystWeight = amethystWeight;
         this.spawnerWeight = spawnerWeight;
         this.soundWeight = soundWeight;
         this.playerWeight = playerWeight;
         this.mobWeight = mobWeight;
         this.anomalyWeight = anomalyWeight;
         this.weightedX = weightedX;
         this.weightedY = weightedY;
         this.weightedZ = weightedZ;
         this.centerWeight = centerWeight;
         this.minY = minY;
         this.maxY = maxY;
         this.families = families;
      }

      static CentralBaseScorer.Snapshot empty(long chunkKey) {
         return new CentralBaseScorer.Snapshot(
            chunkKey,
            0,
            Integer.MIN_VALUE,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            EnumSet.noneOf(CentralBaseScorer.Family.class)
         );
      }

      boolean isEmpty() {
         return this.points <= 0;
      }

      int familyCount() {
         int count = this.families.size();
         return this.families.contains(CentralBaseScorer.Family.NATURAL) ? count - 1 : count;
      }

      boolean hasBaseEvidence() {
         return this.totalWeight > this.naturalWeight && this.familyCount() > 0;
      }

      boolean naturalBlocked() {
         double playerWeight = this.totalWeight - this.naturalWeight;
         return this.naturalWeight >= 80.0 && (playerWeight < 80.0 || this.familyCount() <= 1);
      }

      int confidenceContribution() {
         if (!this.hasBaseEvidence()) {
            return 0;
         } else {
            int families = this.familyCount();
            double d = 0.0;
            d += Math.min(22.0, this.storageWeight * 0.18);
            d += Math.min(24.0, this.automationWeight * 0.2);
            d += Math.min(22.0, this.redstoneWeight * 0.22);
            d += Math.min(28.0, this.farmWeight * 0.24);
            d += Math.min(36.0, this.amethystWeight * 0.28);
            d += Math.min(30.0, this.spawnerWeight * 0.26);
            d += Math.min(18.0, this.soundWeight * 0.18);
            d += Math.min(22.0, this.playerWeight * 0.2);
            d += Math.min(18.0, this.mobWeight * 0.18);
            d += Math.min(20.0, this.anomalyWeight * 0.18);
            if (families >= 2) {
               d += (double)(families * families) * 3.0;
            }

            if (families >= 3) {
               d += 12.0;
            }

            if ((this.storageWeight > 0.0 || this.spawnerWeight > 0.0) && (this.automationWeight > 0.0 || this.redstoneWeight > 0.0)) {
               d += 16.0;
            }

            if (this.farmWeight > 0.0 && (this.automationWeight > 0.0 || this.redstoneWeight > 0.0 || this.storageWeight > 0.0)) {
               d += 16.0;
            }

            if (this.amethystWeight >= 58.0 && (this.storageWeight > 0.0 || this.automationWeight > 0.0 || this.playerWeight > 0.0)) {
               d += 14.0;
            }

            if (this.mobWeight > 0.0 && (this.spawnerWeight > 0.0 || this.storageWeight > 0.0 || this.automationWeight > 0.0)) {
               d += 10.0;
            }

            d -= Math.min(70.0, this.naturalWeight * 0.55);
            return Math.max(0, Math.min(100, (int)Math.round(d)));
         }
      }
   }

   private static final class SnapshotBuilder {
      private final long chunkKey;
      private final EnumSet<CentralBaseScorer.Family> families = EnumSet.noneOf(CentralBaseScorer.Family.class);
      private int points;
      private int lastTick = Integer.MIN_VALUE;
      private double totalWeight;
      private double naturalWeight;
      private double storageWeight;
      private double automationWeight;
      private double redstoneWeight;
      private double farmWeight;
      private double amethystWeight;
      private double spawnerWeight;
      private double soundWeight;
      private double playerWeight;
      private double mobWeight;
      private double anomalyWeight;
      private double weightedX;
      private double weightedY;
      private double weightedZ;
      private double centerWeight;
      private int minY = Integer.MAX_VALUE;
      private int maxY = Integer.MIN_VALUE;

      private SnapshotBuilder(long chunkKey) {
         this.chunkKey = chunkKey;
      }

      private void add(CentralBaseScorer.EvidencePoint point) {
         this.points++;
         this.lastTick = Math.max(this.lastTick, point.tick());
         this.families.add(point.type().family);
         double weight = point.weight();
         this.totalWeight += weight;
         if (point.type() == CentralBaseScorer.EvidenceType.NATURAL) {
            this.naturalWeight += weight;
         } else {
            this.weightedX = this.weightedX + point.x() * weight;
            this.weightedY = this.weightedY + point.y() * weight;
            this.weightedZ = this.weightedZ + point.z() * weight;
            this.centerWeight += weight;
            this.minY = Math.min(this.minY, (int)Math.floor(point.y()));
            this.maxY = Math.max(this.maxY, (int)Math.ceil(point.y()));
            switch (point.type()) {
               case STORAGE:
               case SHULKER:
                  this.storageWeight += weight;
                  break;
               case HOPPER:
                  this.automationWeight += weight;
                  break;
               case REDSTONE:
                  this.automationWeight += weight;
                  this.redstoneWeight += weight;
                  break;
               case FARM:
                  this.farmWeight += weight;
                  break;
               case AMETHYST:
                  this.amethystWeight += weight;
                  break;
               case SPAWNER:
                  this.spawnerWeight += weight;
                  break;
               case SOUND:
                  this.soundWeight += weight;
                  break;
               case PLAYER:
                  this.playerWeight += weight;
                  break;
               case MOB:
                  this.mobWeight += weight;
                  break;
               case ANOMALY:
                  this.anomalyWeight += weight;
               case NATURAL:
            }
         }
      }

      private CentralBaseScorer.Snapshot build() {
         return new CentralBaseScorer.Snapshot(
            this.chunkKey,
            this.points,
            this.lastTick,
            this.totalWeight,
            this.naturalWeight,
            this.storageWeight,
            this.automationWeight,
            this.redstoneWeight,
            this.farmWeight,
            this.amethystWeight,
            this.spawnerWeight,
            this.soundWeight,
            this.playerWeight,
            this.mobWeight,
            this.anomalyWeight,
            this.weightedX,
            this.weightedY,
            this.weightedZ,
            this.centerWeight,
            this.minY,
            this.maxY,
            this.families.clone()
         );
      }
   }
}
