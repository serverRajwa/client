package com.birowo.debug.modules;

import com.birowo.debug.BirowoDebugAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public class SpawnerNotifier extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> chatAlert = sgGeneral.add(
        new BoolSetting.Builder()
            .name("chat-alert")
            .description("Send chat message when spawner is found.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toastAlert = sgGeneral.add(
        new BoolSetting.Builder()
            .name("toast-alert")
            .description("Show toast notification when spawner is found.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> soundAlert = sgGeneral.add(
        new BoolSetting.Builder()
            .name("sound-alert")
            .description("Play sound when spawner is found.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> showTracers = sgRender.add(
        new BoolSetting.Builder()
            .name("tracers")
            .description("Show tracer lines to spawners.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> showBox = sgRender.add(
        new BoolSetting.Builder()
            .name("box")
            .description("Show box around spawners.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("range")
            .description("Range to detect spawners.")
            .defaultValue(16)
            .min(1)
            .max(64)
            .sliderMax(32)
            .build()
    );

    private final Setting<SettingColor> tracerColor = sgRender.add(
        new SettingColor.Builder()
            .name("tracer-color")
            .defaultValue(0, 255, 200)
            .build()
    );

    private final Setting<SettingColor> boxColor = sgRender.add(
        new SettingColor.Builder()
            .name("box-color")
            .defaultValue(255, 255, 0, 100)
            .build()
    );

    private final Set<BlockPos> foundSpawners = new HashSet<>();

    public SpawnerNotifier() {
        super(BirowoDebugAddon.CATEGORY_INFO, "spawner-notifier", "Notifies when mob spawners are nearby.");
    }

    @Override
    public void onActivate() {
        foundSpawners.clear();
    }

    @Override
    public void onDeactivate() {
        foundSpawners.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        double rangeSq = range.get() * range.get();
        int cx = mc.player.getChunkPos().x;
        int cz = mc.player.getChunkPos().z;
        int radius = (int) Math.ceil(range.get() / 16.0) + 1;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx + dx, cz + dz);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof MobSpawnerBlockEntity) {
                        BlockPos pos = be.getPos();
                        double dist = mc.player.getSquaredDistance(pos.getX(), pos.getY(), pos.getZ());

                        if (dist <= rangeSq && foundSpawners.add(pos)) {
                            onNewSpawnerFound(pos);
                        }
                    }
                }
            }
        }
    }

    private void onNewSpawnerFound(BlockPos pos) {
        String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

        if (chatAlert.get()) {
            info("Spawner found at " + coords);
        }

        if (soundAlert.get() && mc.player != null) {
            mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }
    }

    public Set<BlockPos> getFoundSpawners() {
        return foundSpawners;
    }
}
