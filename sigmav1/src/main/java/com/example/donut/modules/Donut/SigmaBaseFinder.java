
package com.example.donut.modules.Donut;

import com.example.donut.DonutAddon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.chunk.WorldChunk;
public class SigmaBaseFinder
extends Module {
    private static final double SCAN_RANGE = 64.0;
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int MAX_DENSE_CHUNK_BEAMS = 64;
    private static final int MAX_SAVED_DENSE_CHUNKS = 256;
    private static final double BEAM_WIDTH = 2.0;
    private static final double BEAM_HEIGHT = 320.0;
    private static final long BEAM_LIFETIME_MILLIS = 60000L;
    private final SettingGroup sgGeneral;
    private final Setting<Integer> denseChunkThreshold;
    private final Setting<SettingColor> beamColor;
    private final Map<ChunkPos, DenseChunkDetection> denseChunks;
    private int scanTicks;

    public SigmaBaseFinder() {
        super(DonutAddon.CATEGORY, "sigma-base-finder", "Marks chunks with high block entity density.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.denseChunkThreshold = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("dense-chunk-threshold")).description("Chunks with more block entities than this get a beam.")).defaultValue(30)).min(1).sliderMin(1).sliderMax(300).build());
        this.beamColor = this.sgGeneral.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("beam-color")).description("Color for dense chunk beams.")).defaultValue(new SettingColor(0, 255, 0, 230)).build());
        this.denseChunks = new LinkedHashMap<ChunkPos, DenseChunkDetection>();
    }

    public void onActivate() {
        this.clearScanCaches();
        this.scanTicks = 0;
    }

    public void onDeactivate() {
        this.clearScanCaches();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (this.mc.world == null || this.mc.player == null) {
            return;
        }
        if (this.scanTicks > 0) {
            --this.scanTicks;
            return;
        }
        this.scanTicks = 5;
        this.rescanLoadedChunks();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (this.mc.world == null || this.mc.player == null) {
            return;
        }
        this.renderDenseChunkBeams(event);
    }

    private void rescanLoadedChunks() {
        HashMap<ChunkPos, Integer> blockEntityCounts = new HashMap<ChunkPos, Integer>();
        double rangeSq = 4096.0;
        double playerX = this.mc.player.getX();
        double playerZ = this.mc.player.getZ();
        ChunkPos playerChunk = this.mc.player.getChunkPos();
        int radius = (int)Math.ceil(4.0) + 1;
        for (int cx = playerChunk.x - radius; cx <= playerChunk.x + radius; ++cx) {
            for (int cz = playerChunk.z - radius; cz <= playerChunk.z + radius; ++cz) {
                WorldChunk chunk = this.mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;
                ChunkPos chunkPos = chunk.getPos();
                int count = 0;
                for (Map.Entry entry : chunk.getBlockEntities().entrySet()) {
                    double dz;
                    BlockPos pos;
                    double dx;
                    BlockEntity blockEntity = (BlockEntity)entry.getValue();
                    if (blockEntity == null || (dx = (double)(pos = ((BlockPos)entry.getKey()).toImmutable()).getX() + 0.5 - playerX) * dx + (dz = (double)pos.getZ() + 0.5 - playerZ) * dz > rangeSq) continue;
                    ++count;
                }
                if (count <= 0) continue;
                blockEntityCounts.put(chunkPos, count);
            }
        }
        int renderedDense = 0;
        for (Map.Entry entry : blockEntityCounts.entrySet()) {
            if ((Integer)entry.getValue() <= (Integer)this.denseChunkThreshold.get()) continue;
            if (renderedDense >= 64) break;
            this.saveDenseChunkDetection((ChunkPos)entry.getKey());
            ++renderedDense;
        }
    }

    private void saveDenseChunkDetection(ChunkPos chunkPos) {
        DenseChunkDetection existing = this.denseChunks.get(chunkPos);
        boolean isNew = existing == null;
        long detectedAtMillis = isNew ? System.currentTimeMillis() : existing.detectedAtMillis;
        this.denseChunks.put(chunkPos, new DenseChunkDetection(chunkPos, detectedAtMillis));
        this.trimDenseChunkDetections();
    }

    private void trimDenseChunkDetections() {
        while (this.denseChunks.size() > 256) {
            ChunkPos oldest = this.denseChunks.keySet().iterator().next();
            this.denseChunks.remove(oldest);
        }
    }

    private void renderDenseChunkBeams(Render3DEvent event) {
        if (this.denseChunks.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int rendered = 0;
        HashSet<Long> renderedGroups = new HashSet<Long>();
        double halfWidth = 1.0;
        double bottomY = this.mc.world.getBottomY();
        double topY = Math.max(bottomY + 1.0, 320.0);
        Color line = (Color)this.beamColor.get();
        Color fill = new Color(line.r, line.g, line.b, Math.max(15, line.a / 4));
        for (DenseChunkDetection detection : this.denseChunks.values()) {
            ChunkPos groupOrigin;
            if (rendered >= 64) break;
            if (now - detection.detectedAtMillis >= 60000L || !renderedGroups.add(this.getChunkKey(groupOrigin = this.getBeamGroupOrigin(detection.chunkPos)))) continue;
            double centerX = (double)groupOrigin.getStartX() + 16.0;
            double centerZ = (double)groupOrigin.getStartZ() + 16.0;
            event.renderer.box(centerX - halfWidth, bottomY, centerZ - halfWidth, centerX + halfWidth, topY, centerZ + halfWidth, fill, line, ShapeMode.Both, 0);
            ++rendered;
        }
    }

    private ChunkPos getBeamGroupOrigin(ChunkPos chunkPos) {
        return new ChunkPos(Math.floorDiv(chunkPos.x, 2) * 2, Math.floorDiv(chunkPos.z, 2) * 2);
    }

    private long getChunkKey(ChunkPos chunkPos) {
        return (long)chunkPos.x << 32 ^ (long)chunkPos.z & 0xFFFFFFFFL;
    }

    private void clearScanCaches() {
        this.denseChunks.clear();
    }

    private static class DenseChunkDetection {
        private final ChunkPos chunkPos;
        private final long detectedAtMillis;

        private DenseChunkDetection(ChunkPos chunkPos, long detectedAtMillis) {
            this.chunkPos = chunkPos;
            this.detectedAtMillis = detectedAtMillis;
        }
    }
}

