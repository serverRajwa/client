package com.birowo.debug.modules;

import com.birowo.debug.BirowoDebugAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public class AmethystESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(
        new IntSetting.Builder()
            .name("range")
            .description("Range to scan for amethyst.")
            .defaultValue(32)
            .min(1)
            .max(64)
            .sliderMax(48)
            .build()
    );

    private final Setting<Boolean> showBoxes = sgRender.add(
        new BoolSetting.Builder()
            .name("show-boxes")
            .description("Show boxes around amethyst blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> boxColor = sgRender.add(
        new SettingColor.Builder()
            .name("box-color")
            .description("Color of the box.")
            .defaultValue(180, 100, 255, 100)
            .build()
    );

    private final Setting<Boolean> showTracers = sgRender.add(
        new BoolSetting.Builder()
            .name("tracers")
            .description("Show tracer lines.")
            .defaultValue(false)
            .build()
    );

    private final Setting<SettingColor> tracerColor = sgRender.add(
        new SettingColor.Builder()
            .name("tracer-color")
            .description("Color of tracers.")
            .defaultValue(180, 100, 255)
            .build()
    );

    private final Set<BlockPos> foundAmethyst = new HashSet<>();
    private int scanTick = 0;

    public AmethystESP() {
        super(BirowoDebugAddon.CATEGORY_INFO, "amethyst-esp", "Highlights amethyst blocks and geodes.");
    }

    @Override
    public void onActivate() {
        foundAmethyst.clear();
        scanTick = 0;
    }

    @Override
    public void onDeactivate() {
        foundAmethyst.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        scanTick++;
        if (scanTick < 10) return;
        scanTick = 0;

        foundAmethyst.clear();

        int cx = mc.player.getChunkPos().x;
        int cz = mc.player.getChunkPos().z;
        int radius = (int) Math.ceil(range.get() / 16.0) + 1;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx + dx, cz + dz);
                if (chunk == null) continue;

                scanChunk(chunk);
            }
        }
    }

    private void scanChunk(WorldChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        ChunkSection[] sections = chunk.getSectionArray();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            if (section == null || section.isEmpty()) continue;

            int sectionY = chunk.getBottomSectionCoord() + (sectionIndex << 4);

            for (int lx = 0; lx < 16; lx++) {
                for (int ly = 0; ly < 16; ly++) {
                    for (int lz = 0; lz < 16; lz++) {
                        var state = section.getBlockState(lx, ly, lz);
                        if (isAmethyst(state.getBlock())) {
                            int worldX = chunkPos.getStartX() + lx;
                            int worldY = sectionY + ly;
                            int worldZ = chunkPos.getStartZ() + lz;
                            foundAmethyst.add(new BlockPos(worldX, worldY, worldZ));
                        }
                    }
                }
            }
        }
    }

    private boolean isAmethyst(net.minecraft.block.Block block) {
        return block == Blocks.AMETHYST_BLOCK ||
               block == Blocks.BUDDING_AMETHYST ||
               block == Blocks.AMETHYST_CLUSTER ||
               block == Blocks.LARGE_AMETHYST_BUD ||
               block == Blocks.MEDIUM_AMETHYST_BUD ||
               block == Blocks.SMALL_AMETHYST_BUD;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!showBoxes.get() && !showTracers.get()) return;
        if (foundAmethyst.isEmpty()) return;

        Color boxCol = new Color(boxColor.get());
        Color tracerCol = new Color(tracerColor.get());

        for (BlockPos pos : foundAmethyst) {
            if (showBoxes.get()) {
                event.renderer.box(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                    boxCol, boxCol,
                    ShapeMode.Both,
                    0
                );
            }

            if (showTracers.get() && mc.player != null) {
                event.renderer.line(
                    mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(),
                    mc.player.getZ(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    tracerCol
                );
            }
        }
    }
}
