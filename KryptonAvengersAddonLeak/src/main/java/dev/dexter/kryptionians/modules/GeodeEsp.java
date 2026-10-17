package dev.dexter.kryptionians.modules;

import dev.dexter.kryptionians.KryptioniansAddon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class GeodeEsp extends Module {
  private static final int CHUNKS_PER_TICK = 6;
  private static final int SCAN_PADDING = 4;
  private static final Color LINE_COLOR = new Color(255, 0, 255, 0);
  private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
  private final Setting<Boolean> boxEsp =
      this.sgGeneral.add(new BoolSetting.Builder().name("block-esp").defaultValue(true).build());
  private final Setting<Integer> alpha =
      this.sgGeneral.add(
          new IntSetting.Builder()
              .name("alpha")
              .defaultValue(40)
              .range(20, 255)
              .sliderRange(20, 255)
              .build());
  private final Map<ChunkPos, List<Long>> geodeBlocksByChunk = new HashMap<>();
  private final Set<ChunkPos> scannedChunks = new HashSet<>();
  private final List<GeodeEsp.ChunkOffset> scanOrder = new ArrayList<>();
  private final MutableBlockPos pos = new MutableBlockPos();
  private final MutableBlockPos neighbor = new MutableBlockPos();
  private ClientLevel lastWorld;
  private ChunkPos lastOrigin;
  private int lastRadius = -1;
  private int scanIndex;

  public GeodeEsp() {
    super(KryptioniansAddon.CATEGORY, "geode esp", "Highlights amethyst blocks.");
  }

  public void onActivate() {
    this.clearAll();
  }

  public void onDeactivate() {
    this.clearAll();
  }

  @EventHandler
  private void onTick(Post event) {
    if (this.mc.level != null && this.mc.player != null) {
      if (this.mc.level != this.lastWorld) {
        this.clearAll();
        this.lastWorld = this.mc.level;
      }

      ChunkPos playerChunk = this.mc.player.chunkPosition();
      int radius = this.scanRadius();
      if (!playerChunk.equals(this.lastOrigin)
          || radius != this.lastRadius
          || this.scanIndex >= this.scanOrder.size()) {
        this.resetCursor(playerChunk, radius);
      }

      int scanned = 0;

      while (scanned < 6 && this.scanIndex < this.scanOrder.size()) {
        GeodeEsp.ChunkOffset offset = this.scanOrder.get(this.scanIndex++);
        ChunkPos chunkPos = new ChunkPos(playerChunk.x + offset.x, playerChunk.z + offset.z);
        if (this.scannedChunks.add(chunkPos)) {
          LevelChunk chunk = this.getLoadedChunk(chunkPos.x, chunkPos.z);
          if (chunk == null) {
            this.scannedChunks.remove(chunkPos);
            this.geodeBlocksByChunk.remove(chunkPos);
          } else {
            List<Long> positions = this.scanChunk(chunk);
            if (positions.isEmpty()) {
              this.geodeBlocksByChunk.remove(chunkPos);
            } else {
              this.geodeBlocksByChunk.put(chunkPos, positions);
            }

            scanned++;
          }
        }
      }

      this.prune(playerChunk, radius + 4);
    } else {
      this.clearAll();
    }
  }

  @EventHandler
  private void onRender(Render3DEvent event) {
    if (this.mc.level != null && this.mc.player != null) {
      if (this.boxEsp.get()) {
        double maxDistanceSq = this.renderDistanceSq();
        double playerX = this.mc.player.getX();
        double playerY = this.mc.player.getY();
        double playerZ = this.mc.player.getZ();
        Color sideColor = new Color(255, 0, 255, this.alpha.get());

        for (Entry<ChunkPos, List<Long>> entry : this.geodeBlocksByChunk.entrySet()) {
          ChunkPos chunkPos = entry.getKey();
          if (this.getLoadedChunk(chunkPos.x, chunkPos.z) != null) {
            for (long packed : entry.getValue()) {
              int x = BlockPos.getX(packed);
              int y = BlockPos.getY(packed);
              int z = BlockPos.getZ(packed);
              double dx = (double) x + 0.5 - playerX;
              double dy = (double) y + 0.5 - playerY;
              double dz = (double) z + 0.5 - playerZ;
              if (!(dx * dx + dy * dy + dz * dz > maxDistanceSq)) {
                event.renderer.box(
                    (double) x,
                    (double) y,
                    (double) z,
                    (double) x + 1.0,
                    (double) y + 1.0,
                    (double) z + 1.0,
                    sideColor,
                    LINE_COLOR,
                    ShapeMode.Sides,
                    0);
              }
            }
          }
        }
      }
    }
  }

  private List<Long> scanChunk(LevelChunk chunk) {
    List<Long> positions = new ArrayList<>();
    LevelChunkSection[] sections = chunk.getSections();
    int baseSectionY = chunk.getMinSectionY();
    int startX = chunk.getPos().getMinBlockX();
    int startZ = chunk.getPos().getMinBlockZ();

    for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
      LevelChunkSection section = sections[sectionIndex];
      if (section != null && !section.hasOnlyAir() && section.maybeHas(GeodeEsp::isTrackedState)) {
        int baseY = baseSectionY + sectionIndex << 4;

        for (int y = 0; y < 16; y++) {
          int worldY = baseY + y;

          for (int x = 0; x < 16; x++) {
            int worldX = startX + x;

            for (int z = 0; z < 16; z++) {
              BlockState state = section.getBlockState(x, y, z);
              if (isTrackedState(state)) {
                int worldZ = startZ + z;
                this.pos.set(worldX, worldY, worldZ);
                if (this.isSurfaceBlock(this.pos)) {
                  positions.add(this.pos.asLong());
                }
              }
            }
          }
        }
      }
    }

    return positions;
  }

  private boolean isSurfaceBlock(BlockPos blockPos) {
    for (int i = 0; i < 6; i++) {
      switch (i) {
        case 0:
          this.neighbor.set(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ());
          break;
        case 1:
          this.neighbor.set(blockPos.getX() - 1, blockPos.getY(), blockPos.getZ());
          break;
        case 2:
          this.neighbor.set(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
          break;
        case 3:
          this.neighbor.set(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ());
          break;
        case 4:
          this.neighbor.set(blockPos.getX(), blockPos.getY(), blockPos.getZ() + 1);
          break;
        default:
          this.neighbor.set(blockPos.getX(), blockPos.getY(), blockPos.getZ() - 1);
      }

      BlockState neighborState = this.mc.level.getBlockState(this.neighbor);
      if (!isTrackedState(neighborState)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isTrackedState(BlockState state) {
    Block block = state.getBlock();
    return block == Blocks.AMETHYST_BLOCK
        || block == Blocks.BUDDING_AMETHYST
        || block == Blocks.AMETHYST_CLUSTER
        || block == Blocks.LARGE_AMETHYST_BUD
        || block == Blocks.MEDIUM_AMETHYST_BUD
        || block == Blocks.SMALL_AMETHYST_BUD;
  }

  private int scanRadius() {
    int render = 16;

    try {
      render = (Integer) this.mc.options.renderDistance().get();
    } catch (RuntimeException ignored) {
    }

    return render + 1;
  }

  private double renderDistanceSq() {
    double distance = (double) (this.scanRadius() + 1) * 16.0;
    return distance * distance;
  }

  private void resetCursor(ChunkPos origin, int radius) {
    this.scannedChunks.clear();
    this.scanIndex = 0;
    this.lastOrigin = origin;
    this.lastRadius = radius;
    this.rebuildScanOrder(radius);
  }

  private void rebuildScanOrder(int radius) {
    this.scanOrder.clear();

    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        this.scanOrder.add(new GeodeEsp.ChunkOffset(x, z, x * x + z * z));
      }
    }

    this.scanOrder.sort(Comparator.comparingInt(offset -> offset.distanceSq));
  }

  private void prune(ChunkPos center, int radius) {
    int radiusSq = radius * radius;

    for (ChunkPos chunkPos : Set.copyOf(this.geodeBlocksByChunk.keySet())) {
      if (this.chunkDistanceSq(center, chunkPos) > radiusSq) {
        this.geodeBlocksByChunk.remove(chunkPos);
      }
    }

    for (ChunkPos chunkPosx : Set.copyOf(this.scannedChunks)) {
      if (this.chunkDistanceSq(center, chunkPosx) > radiusSq) {
        this.scannedChunks.remove(chunkPosx);
      }
    }
  }

  private LevelChunk getLoadedChunk(int x, int z) {
    return this.mc.level != null && this.mc.level.getChunkSource().hasChunk(x, z)
        ? this.mc.level.getChunk(x, z)
        : null;
  }

  private int chunkDistanceSq(ChunkPos a, ChunkPos b) {
    int dx = a.x - b.x;
    int dz = a.z - b.z;
    return dx * dx + dz * dz;
  }

  private void clearAll() {
    this.geodeBlocksByChunk.clear();
    this.scannedChunks.clear();
    this.scanOrder.clear();
    this.scanIndex = 0;
    this.lastRadius = -1;
    this.lastOrigin = null;
    this.lastWorld = this.mc.level;
  }

  public String getInfoString() {
    int total = 0;

    for (List<Long> positions : this.geodeBlocksByChunk.values()) {
      total += positions.size();
    }

    return total == 0 ? null : Integer.toString(total);
  }

  private static record ChunkOffset(int x, int z, int distanceSq) {}
}
