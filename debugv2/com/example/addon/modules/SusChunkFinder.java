package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
public class SusChunkFinder extends Module {
   private static final int Y_MIN_AMETHYST = -58;
   private static final int Y_MAX_AMETHYST_LIGHT = 50;
   private static final int Y_MAX_AMETHYST_BLOCK = 30;
   private static final int AMETHYST_SECTION_THRESHOLD = 100;
   private static final int CLUSTER_LIGHT_CAP = 5;
   private static final int RESCAN_INTERVAL_TICKS = 200;
   private static final int RECALC_TOP_INTERVAL = 10;
   private static final int SCAN_BUDGET = 2;
   private static final int RETRY_ATTEMPTS = 20;
   private static final int MAX_RENDERED = 20;
   private static final int HIGHLIGHT_Y = 64;
   private static final double SLAB_HEIGHT = 0.06;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> simulationDistance = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("simulation-distance"))
                  .description("Chunks must be at this edge distance before new suspicious hits are created."))
               .defaultValue(5))
            .min(1)
            .max(32)
            .sliderRange(1, 32)
            .build()
      );
   private final Setting<Integer> sensitivity = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("sensitivity"))
                  .description("Lower values flag weaker suspicious chunks. Higher values reduce false positives."))
               .defaultValue(5))
            .min(1)
            .max(20)
            .sliderRange(1, 20)
            .build()
      );
   private final Setting<Integer> alpha = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("alpha")).description("Opacity for the blue suspicious chunk ESP.")).defaultValue(42))
            .min(1)
            .max(255)
            .sliderRange(1, 255)
            .build()
      );
   private final Setting<Boolean> kelp = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("kelp"))
                  .description("Detect high kelp density chunks."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> caveVines = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("cave-vines"))
                  .description("Detect cave vine density chunks."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> vines = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("vines"))
                  .description("Detect vine density chunks."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> amethyst = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("amethyst"))
                  .description("Detect depleted amethyst geode edge chunks."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> bamboo = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("bamboo"))
                  .description("Detect bamboo density chunks."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> beeNest = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("bee-nest"))
                  .description("Detect bee nest and beehive chunks."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> rotatedDeepslate = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("rotated-deepslate"))
                  .description("Detect horizontally rotated deepslate, a strong player placement signal."))
               .defaultValue(false))
            .build()
      );
   private final Map<Long, SusChunkFinder.ChunkData> dataByChunk = new ConcurrentHashMap<>();
   private final Map<Long, Integer> scores = new ConcurrentHashMap<>();
   private final Map<Long, String> reasons = new ConcurrentHashMap<>();
   private final Map<Long, SusChunkFinder.Cluster> clustersByLeader = new ConcurrentHashMap<>();
   private final Map<Long, Integer> scanRetries = new ConcurrentHashMap<>();
   private final Set<Long> flagged = ConcurrentHashMap.newKeySet();
   private final ConcurrentLinkedQueue<Long> scanQueue = new ConcurrentLinkedQueue<>();
   private final Set<Long> queuedScans = ConcurrentHashMap.newKeySet();
   private final ConcurrentLinkedQueue<Long> evalQueue = new ConcurrentLinkedQueue<>();
   private final Set<Long> queuedEvals = ConcurrentHashMap.newKeySet();
   private long topCandidate = Long.MIN_VALUE;
   private ChunkPos lastPlayerChunk;
   private int tickCounter;
   private boolean dirtyTop;

   public SusChunkFinder() {
      super(AddonTemplate.F, "sus-chunk", "Suspicious key to enabled: finds edge-loaded chunks with selected suspicious block signals.");
   }

   public void onActivate() {
      this.clear();
      this.queueLoadedChunks();
   }

   public void onDeactivate() {
      this.clear();
   }

   @EventHandler
   private void on(ChunkDataEvent event) {
      if (event.chunk() != null && this.mc.player != null) {
         this.queueScan(event.chunk().getPos());
      }
   }

   @EventHandler
   private void on(Post event) {
      if (this.mc.world != null && this.mc.player != null) {
         this.tickCounter++;
         ChunkPos current = this.mc.player.getChunkPos();
         if (this.lastPlayerChunk != null) {
            int moved = this.chebyshev(current, this.lastPlayerChunk);
            if (moved > 32) {
               this.clear();
               this.queueLoadedChunks();
               this.lastPlayerChunk = current;
               return;
            }
         }

         this.lastPlayerChunk = current;
         if (this.tickCounter % 200 == 0) {
            this.queueLoadedChunks();
         }

         for (int i = 0; i < 2; i++) {
            Long key = this.scanQueue.poll();
            if (key == null) {
               break;
            }

            this.queuedScans.remove(key);
            this.scanChunk(key);
         }

         for (int i = 0; i < 8; i++) {
            Long key = this.evalQueue.poll();
            if (key == null) {
               break;
            }

            this.queuedEvals.remove(key);
            this.evaluateChunk(key);
         }

         if (this.dirtyTop && this.tickCounter % 10 == 0) {
            this.recalcTop();
            this.dirtyTop = false;
         }
      }
   }

   @EventHandler
   private void on(Render3DEvent event) {
      if (this.mc.world != null && this.mc.player != null && !this.flagged.isEmpty()) {
         if (this.dirtyTop || this.clustersByLeader.isEmpty()) {
            this.recalcTop();
            this.dirtyTop = false;
         }

         SettingColor normal = this.blue((Integer)this.alpha.get());
         SettingColor top = this.blue(Math.min(255, Math.max((Integer)this.alpha.get() + 40, (Integer)this.alpha.get() * 2)));
         int rendered = 0;
         SusChunkFinder.Cluster topCluster = this.clustersByLeader.get(this.topCandidate);
         if (topCluster != null) {
            this.renderCluster(event, topCluster, top);
            rendered++;
         }

         for (SusChunkFinder.Cluster cluster : this.clustersByLeader.values()) {
            if (cluster.leader != this.topCandidate) {
               if (rendered >= 20) {
                  break;
               }

               this.renderCluster(event, cluster, normal);
               rendered++;
            }
         }
      }
   }

   private void queueLoadedChunks() {
      if (this.mc.world != null && this.mc.player != null && this.hasAnySignalEnabled()) {
         ChunkPos center = this.mc.player.getChunkPos();
         int radius = Math.max(8, (Integer)this.simulationDistance.get() + 2);
         ArrayList<ChunkPos> chunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               ChunkPos chunkPos = new ChunkPos(center.x + dx, center.z + dz);
               if (this.chebyshev(chunkPos, center) >= (Integer)this.simulationDistance.get()) {
                  chunks.add(chunkPos);
               }
            }
         }

         chunks.sort(Comparator.<ChunkPos>comparingInt(posx -> this.chebyshev(posx, center)).reversed());

         for (ChunkPos chunkPos : chunks) {
            WorldChunk chunk = this.mc.world.getChunkManager().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
            if (chunk != null) {
               this.queueScan(chunkPos);
            }
         }
      }
   }

   private void queueScan(ChunkPos chunkPos) {
      if (this.hasAnySignalEnabled()) {
         long key = chunkPos.toLong();
         if (this.queuedScans.add(key)) {
            this.scanQueue.add(key);
         }
      }
   }

   private void queueEvaluation(ChunkPos center) {
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            long key = ChunkPos.toLong(center.x + dx, center.z + dz);
            if (this.queuedEvals.add(key)) {
               this.evalQueue.add(key);
            }
         }
      }
   }

   private void scanChunk(long key) {
      if (this.mc.world != null) {
         ChunkPos chunkPos = new ChunkPos(key);
         WorldChunk chunk = this.mc.world.getChunkManager().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
         if (chunk == null) {
            int attempts = this.scanRetries.merge(key, 1, Integer::sum);
            if (attempts <= 20) {
               this.queueScan(chunkPos);
            }
         } else {
            this.scanRetries.remove(key);
            this.dataByChunk.put(key, this.scan(chunk));
            this.queueEvaluation(chunkPos);
         }
      }
   }

   private SusChunkFinder.ChunkData scan(WorldChunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      int baseX = chunkPos.getStartX();
      int baseZ = chunkPos.getStartZ();
      Mutable mutable = new Mutable();
      int amethystLightHits = 0;
      int budding = 0;
      int shellAmethyst = 0;
      int maxAmethystSection = 0;
      int kelpCount = 0;
      int caveVineCount = 0;
      int vineCount = 0;
      int bambooCount = 0;
      int beeNestCount = 0;
      int rotatedDeepslateCount = 0;
      BlockPos nearestSignal = null;
      boolean scanAmethyst = (Boolean)this.amethyst.get();
      boolean scanFullHeight = (Boolean)this.kelp.get()
         || (Boolean)this.caveVines.get()
         || (Boolean)this.vines.get()
         || (Boolean)this.bamboo.get()
         || (Boolean)this.beeNest.get()
         || (Boolean)this.rotatedDeepslate.get();
      ChunkSection[] sections = chunk.getSectionArray();

      for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
         ChunkSection section = sections[sectionIndex];
         if (section != null && !section.isEmpty()) {
            int sectionY = chunk.getBottomSectionCoord() + sectionIndex << 4;
            if (scanFullHeight || scanAmethyst && this.overlaps(sectionY, sectionY + 15, -58, 50)) {
               int minLocalY = scanFullHeight ? 0 : Math.max(0, -58 - sectionY);
               int maxLocalY = scanFullHeight ? 15 : Math.min(15, 50 - sectionY);
               int sectionAmethystBlocks = 0;

               for (int lx = 0; lx < 16; lx++) {
                  for (int lz = 0; lz < 16; lz++) {
                     for (int ly = minLocalY; ly <= maxLocalY; ly++) {
                        int worldY = sectionY + ly;
                        BlockState state = section.getBlockState(lx, ly, lz);
                        Block block = state.getBlock();
                        int worldX = baseX + lx;
                        int worldZ = baseZ + lz;
                        boolean signal = false;
                        if (scanAmethyst && this.isAmethyst(block)) {
                           if (worldY >= -58 && worldY <= 30) {
                              shellAmethyst++;
                              if (block == Blocks.BUDDING_AMETHYST) {
                                 budding++;
                              }

                              if (block == Blocks.AMETHYST_BLOCK) {
                                 sectionAmethystBlocks++;
                              }

                              signal = true;
                           }

                           if (worldY >= -58 && worldY <= 50 && amethystLightHits < 5) {
                              amethystLightHits = this.countNearbyAmethystLight(worldX, worldY, worldZ, amethystLightHits, mutable);
                           }
                        }

                        if ((Boolean)this.kelp.get() && this.isKelp(block)) {
                           kelpCount++;
                           signal = true;
                        }

                        if ((Boolean)this.caveVines.get() && this.isCaveVine(block)) {
                           caveVineCount++;
                           signal = true;
                        }

                        if ((Boolean)this.vines.get() && this.isVine(block)) {
                           vineCount++;
                           signal = true;
                        }

                        if ((Boolean)this.bamboo.get() && this.isBamboo(block)) {
                           bambooCount++;
                           signal = true;
                        }

                        if ((Boolean)this.beeNest.get() && this.isBeeSignal(state)) {
                           beeNestCount++;
                           signal = true;
                        }

                        if ((Boolean)this.rotatedDeepslate.get() && this.isRotatedDeepslate(state)) {
                           rotatedDeepslateCount++;
                           signal = true;
                        }

                        if (signal && nearestSignal == null) {
                           nearestSignal = new BlockPos(worldX, worldY, worldZ);
                        }
                     }
                  }
               }

               maxAmethystSection = Math.max(maxAmethystSection, sectionAmethystBlocks);
            }
         }
      }

      return new SusChunkFinder.ChunkData(
         Math.min(amethystLightHits, 5),
         budding,
         shellAmethyst,
         maxAmethystSection,
         kelpCount,
         caveVineCount,
         vineCount,
         bambooCount,
         beeNestCount,
         rotatedDeepslateCount,
         nearestSignal
      );
   }

   private int countNearbyAmethystLight(int x, int y, int z, int current, Mutable mutable) {
      if (this.mc.world == null) {
         return current;
      } else {
         int hits = current;

         for (int dy = -1; dy <= 1 && hits < 5; dy++) {
            int yy = y + dy;
            if (yy >= -58 && yy <= 50) {
               for (int dx = -3; dx <= 3 && hits < 5; dx++) {
                  for (int dz = -3; dz <= 3; dz++) {
                     if (this.mc.world.getLightLevel(LightType.BLOCK, mutable.set(x + dx, yy, z + dz)) == 5) {
                        if (++hits >= 5) {
                           break;
                        }
                     }
                  }
               }
            }
         }

         return hits;
      }
   }

   private void evaluateChunk(long key) {
      SusChunkFinder.ChunkData data = this.dataByChunk.get(key);
      if (data != null && this.hasAnySignalEnabled()) {
         if (this.flagged.contains(key)
            || this.mc.player == null
            || this.chebyshev(new ChunkPos(key), this.mc.player.getChunkPos()) >= (Integer)this.simulationDistance.get()) {
            SusChunkFinder.Score score = this.score(data, key);
            if (score.value >= (Integer)this.sensitivity.get()) {
               this.flagged.add(key);
               this.scores.put(key, score.value);
               this.reasons.put(key, score.reason);
               this.dirtyTop = true;
               this.submitEvidence(key, data, score);
            } else {
               this.removeFlag(key);
            }
         }
      }
   }

   private SusChunkFinder.Score coord(SusChunkFinder.ChunkData data, long key) {
      int value = 0;
      String reason = "";
      int neighbourEvidence = this.countNeighbourEvidence(key);
      if ((Boolean)this.amethyst.get() && data.buddingCount == 0 && this.anyNeighbourHasAmethyst(key)) {
         int depleted = this.countDepletedAmethystNeighbours(key);
         int amethystScore = 0;
         if (data.maxAmethystSection >= 100) {
            amethystScore += 5;
         }

         amethystScore += Math.min(6, data.shellAmethyst / 20);
         amethystScore += Math.min(6, depleted * 2);
         if (data.amethystLightHits == 0 && data.shellAmethyst >= 2) {
            amethystScore += 3;
         }

         if (amethystScore > value) {
            value = amethystScore;
            reason = "amethyst";
         }
      }

      SusChunkFinder.Score kelpScore = this.maxReason(value, reason, data.kelpCount / 24, "kelp");
      value = kelpScore.value;
      reason = kelpScore.reason;
      SusChunkFinder.Score caveScore = this.maxReason(value, reason, data.caveVineCount / 18, "cave vines");
      value = caveScore.value;
      reason = caveScore.reason;
      SusChunkFinder.Score vineScore = this.maxReason(value, reason, data.vineCount / 24, "vines");
      value = vineScore.value;
      reason = vineScore.reason;
      SusChunkFinder.Score bambooScore = this.maxReason(value, reason, data.bambooCount / 20, "bamboo");
      value = bambooScore.value;
      reason = bambooScore.reason;
      SusChunkFinder.Score beeScore = this.maxReason(value, reason, data.beeNestCount * 5, "bee nest");
      value = beeScore.value;
      reason = beeScore.reason;
      SusChunkFinder.Score deepslateScore = this.maxReason(value, reason, data.rotatedDeepslateCount / 12, "rotated deepslate");
      value = deepslateScore.value;
      reason = deepslateScore.reason;
      if (value > 0) {
         value += Math.min(5, neighbourEvidence);
         if (neighbourEvidence >= 3) {
            value += 2;
         }
      }

      if (reason.isEmpty()) {
         reason = "suspicious";
      }

      return new SusChunkFinder.Score(value, reason);
   }

   private SusChunkFinder.Score maxReason(int currentValue, String currentReason, int candidateValue, String candidateReason) {
      return candidateValue > currentValue ? new SusChunkFinder.Score(candidateValue, candidateReason) : new SusChunkFinder.Score(currentValue, currentReason);
   }

   private boolean anyNeighbourHasAmethyst(long key) {
      SusChunkFinder.ChunkData self = this.dataByChunk.get(key);
      if (self != null && self.shellAmethyst >= 2) {
         return true;
      } else {
         ChunkPos chunkPos = new ChunkPos(key);

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if (dx != 0 || dz != 0) {
                  SusChunkFinder.ChunkData data = this.dataByChunk.get(ChunkPos.toLong(chunkPos.x + dx, chunkPos.z + dz));
                  if (data != null && data.shellAmethyst >= 2) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private int countDepletedAmethystNeighbours(long key) {
      int count = 0;
      ChunkPos chunkPos = new ChunkPos(key);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (dx != 0 || dz != 0) {
               SusChunkFinder.ChunkData data = this.dataByChunk.get(ChunkPos.toLong(chunkPos.x + dx, chunkPos.z + dz));
               if (data != null && data.buddingCount <= 0 && data.amethystLightHits <= 1) {
                  count++;
               }
            }
         }
      }

      return count;
   }

   private int countNeighbourEvidence(long key) {
      int count = 0;
      ChunkPos chunkPos = new ChunkPos(key);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (dx != 0 || dz != 0) {
               SusChunkFinder.ChunkData data = this.dataByChunk.get(ChunkPos.toLong(chunkPos.x + dx, chunkPos.z + dz));
               if (data != null && this.hasSuspiciousEvidence(data)) {
                  count++;
               }
            }
         }
      }

      return count;
   }

   private boolean hasSuspiciousEvidence(SusChunkFinder.ChunkData data) {
      return (Boolean)this.amethyst.get() && data.buddingCount == 0 && data.shellAmethyst >= 2
         || (Boolean)this.kelp.get() && data.kelpCount >= 64
         || (Boolean)this.caveVines.get() && data.caveVineCount >= 48
         || (Boolean)this.vines.get() && data.vineCount >= 64
         || (Boolean)this.bamboo.get() && data.bambooCount >= 64
         || (Boolean)this.beeNest.get() && data.beeNestCount > 0
         || (Boolean)this.rotatedDeepslate.get() && data.rotatedDeepslateCount >= 16;
   }

   private void submitEvidence(long key, SusChunkFinder.ChunkData data, SusChunkFinder.Score score) {
      ChunkPos chunkPos = new ChunkPos(key);
      double x = (double)chunkPos.getStartX() + 8.0;
      double z = (double)chunkPos.getStartZ() + 8.0;
      double y = data.nearestSignal == null ? 64.0 : (double)data.nearestSignal.getY();
      CentralBaseScorer.EvidenceType type = score.reason.equals("amethyst") ? CentralBaseScorer.EvidenceType.AMETHYST : CentralBaseScorer.EvidenceType.FARM;
      CentralBaseScorer.submit(type, x, y, z, this.tickCounter, Math.min(100.0, 35.0 + (double)score.value * 8.0), "sus chunk finder " + score.reason);
   }

   private void removeFlag(long key) {
      if (this.flagged.remove(key)) {
         this.dirtyTop = true;
      }

      this.scores.remove(key);
      this.reasons.remove(key);
   }

   private void recalcTop() {
      this.clustersByLeader.clear();
      if (this.flagged.isEmpty()) {
         this.topCandidate = Long.MIN_VALUE;
      } else {
         Set<Long> unvisited = new HashSet<>(this.flagged);

         while (!unvisited.isEmpty()) {
            long seed = unvisited.iterator().next();
            SusChunkFinder.Cluster cluster = this.buildCluster(seed, unvisited);
            this.clustersByLeader.put(cluster.leader, cluster);
         }

         this.topCandidate = this.clustersByLeader
            .values()
            .stream()
            .max(Comparator.comparingInt(clusterx -> clusterx.coord))
            .map(clusterx -> clusterx.leader)
            .orElse(Long.MIN_VALUE);
      }
   }

   private SusChunkFinder.Cluster buildCluster(long seed, Set<Long> unvisited) {
      ArrayDeque<Long> queue = new ArrayDeque<>();
      List<Long> members = new ArrayList<>();
      queue.add(seed);
      unvisited.remove(seed);
      int minX = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxZ = Integer.MIN_VALUE;
      long leader = seed;
      int leaderScore = Integer.MIN_VALUE;
      int totalScore = 0;

      while (!queue.isEmpty()) {
         long key = queue.removeFirst();
         members.add(key);
         ChunkPos chunkPos = new ChunkPos(key);
         minX = Math.min(minX, chunkPos.x);
         maxX = Math.max(maxX, chunkPos.x);
         minZ = Math.min(minZ, chunkPos.z);
         maxZ = Math.max(maxZ, chunkPos.z);
         int value = this.scores.getOrDefault(key, 0);
         totalScore += value;
         if (value > leaderScore) {
            leaderScore = value;
            leader = key;
         }

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if (dx != 0 || dz != 0) {
                  long neighbour = ChunkPos.toLong(chunkPos.x + dx, chunkPos.z + dz);
                  if (unvisited.remove(neighbour)) {
                     queue.addLast(neighbour);
                  }
               }
            }
         }
      }

      int clusterScore = totalScore + Math.min(20, members.size() * 3);
      return new SusChunkFinder.Cluster(leader, minX, maxX, minZ, maxZ, members.size(), clusterScore);
   }

   private void renderCluster(Render3DEvent event, SusChunkFinder.Cluster cluster, SettingColor settingColor) {
      event.renderer
         .box(
            (double)cluster.minChunkX * 16.0,
            64.0,
            (double)cluster.minChunkZ * 16.0,
            (double)(cluster.maxChunkX + 1) * 16.0,
            64.06,
            (double)(cluster.maxChunkZ + 1) * 16.0,
            settingColor,
            settingColor,
            ShapeMode.Both,
            0
         );
   }

   private SettingColor blue(int opacity) {
      return new SettingColor(0, 120, 255, Math.max(1, Math.min(255, opacity)));
   }

   private boolean hasAnySignalEnabled() {
      return (Boolean)this.kelp.get()
         || (Boolean)this.caveVines.get()
         || (Boolean)this.vines.get()
         || (Boolean)this.amethyst.get()
         || (Boolean)this.bamboo.get()
         || (Boolean)this.beeNest.get()
         || (Boolean)this.rotatedDeepslate.get();
   }

   private boolean isAmethyst(Block block) {
      return block == Blocks.AMETHYST_BLOCK
         || block == Blocks.BUDDING_AMETHYST
         || block == Blocks.AMETHYST_CLUSTER
         || block == Blocks.LARGE_AMETHYST_BUD
         || block == Blocks.MEDIUM_AMETHYST_BUD
         || block == Blocks.SMALL_AMETHYST_BUD;
   }

   private boolean isKelp(Block block) {
      return block == Blocks.KELP || block == Blocks.KELP_PLANT;
   }

   private boolean isCaveVine(Block block) {
      return block == Blocks.CAVE_VINES || block == Blocks.CAVE_VINES_PLANT;
   }

   private boolean isVine(Block block) {
      return block == Blocks.VINE
         || block == Blocks.WEEPING_VINES
         || block == Blocks.WEEPING_VINES_PLANT
         || block == Blocks.TWISTING_VINES
         || block == Blocks.TWISTING_VINES_PLANT;
   }

   private boolean isBamboo(Block block) {
      return block == Blocks.BAMBOO || block == Blocks.BAMBOO_SAPLING;
   }

   private boolean isBeeSignal(BlockState state) {
      Block block = state.getBlock();
      if (block == Blocks.BEEHIVE) {
         return true;
      } else {
         return block == Blocks.BEE_NEST && state.contains(Properties.HONEY_LEVEL)
            ? (Integer)state.get(Properties.HONEY_LEVEL) >= 4
            : false;
      }
   }

   private boolean isRotatedDeepslate(BlockState state) {
      return state.getBlock() == Blocks.DEEPSLATE
         && state.contains(Properties.AXIS)
         && state.get(Properties.AXIS) != Axis.Y;
   }

   private int chebyshev(ChunkPos a, ChunkPos b) {
      return Math.max(Math.abs(a.x - b.x), Math.abs(a.z - b.z));
   }

   private boolean overlaps(int minA, int maxA, int minB, int maxB) {
      return minA <= maxB && maxA >= minB;
   }

   private void clear() {
      this.dataByChunk.clear();
      this.scores.clear();
      this.reasons.clear();
      this.clustersByLeader.clear();
      this.scanRetries.clear();
      this.flagged.clear();
      this.scanQueue.clear();
      this.queuedScans.clear();
      this.evalQueue.clear();
      this.queuedEvals.clear();
      this.topCandidate = Long.MIN_VALUE;
      this.lastPlayerChunk = null;
      this.tickCounter = 0;
      this.dirtyTop = false;
   }

   private static final class ChunkData {
      final int amethystLightHits;
      final int buddingCount;
      final int shellAmethyst;
      final int maxAmethystSection;
      final int kelpCount;
      final int caveVineCount;
      final int vineCount;
      final int bambooCount;
      final int beeNestCount;
      final int rotatedDeepslateCount;
      final BlockPos nearestSignal;

      ChunkData(
         int amethystLightHits,
         int buddingCount,
         int shellAmethyst,
         int maxAmethystSection,
         int kelpCount,
         int caveVineCount,
         int vineCount,
         int bambooCount,
         int beeNestCount,
         int rotatedDeepslateCount,
         BlockPos nearestSignal
      ) {
         this.amethystLightHits = amethystLightHits;
         this.buddingCount = buddingCount;
         this.shellAmethyst = shellAmethyst;
         this.maxAmethystSection = maxAmethystSection;
         this.kelpCount = kelpCount;
         this.caveVineCount = caveVineCount;
         this.vineCount = vineCount;
         this.bambooCount = bambooCount;
         this.beeNestCount = beeNestCount;
         this.rotatedDeepslateCount = rotatedDeepslateCount;
         this.nearestSignal = nearestSignal;
      }
   }

   private static final class Cluster {
      final long leader;
      final int minChunkX;
      final int maxChunkX;
      final int minChunkZ;
      final int maxChunkZ;
      final int size;
      final int coord;

      Cluster(long leader, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, int size, int coord) {
         this.leader = leader;
         this.minChunkX = minChunkX;
         this.maxChunkX = maxChunkX;
         this.minChunkZ = minChunkZ;
         this.maxChunkZ = maxChunkZ;
         this.size = size;
         this.coord = coord;
      }
   }

   private static record Score(int value, String reason) {
      private Score(int value, String reason) {
         reason = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
         this.value = value;
         this.reason = reason;
      }
   }
}
