
package com.example.donut.modules.Donut;

import com.example.donut.DonutAddon;
import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.util.math.BlockPos;
public class LightBypasser
extends Module {
    private final Map<ChunkPos, Long> markedChunks = new HashMap<ChunkPos, Long>();
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> lightThreshold = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("light-threshold")).description("Minimum block light level to count as an active firefly glow.")).defaultValue(6)).min(0).max(15).sliderMax(15).build());
    private final Setting<Double> renderDistance = this.sgGeneral.add((Setting)((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name("render-distance")).description("Distance from another player within which a firefly is considered rendered for them.")).defaultValue(128.0).min(16.0).sliderMax(256.0).build());
    private final Setting<Integer> markTimeoutTicks = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("mark-timeout")).description("How many ticks a chunk stays marked after last seeing active firefly light near another player.")).defaultValue(100)).min(0).sliderMax(600).build());
    private final Setting<SettingColor> markColor = this.sgGeneral.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("mark-color")).description("Color used to highlight chunks with active firefly light near other players.")).defaultValue(new SettingColor(255, 220, 100, 120)).build());
    private long tickCounter = 0L;

    public LightBypasser() {
        super(DonutAddon.CATEGORY, "light-bypasser", "Marks chunks where firefly glow light is rendered for another player.");
    }

    public void onActivate() {
        this.markedChunks.clear();
        this.tickCounter = 0L;
    }

    public void onDeactivate() {
        this.markedChunks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (this.mc.world == null || this.mc.player == null) {
            return;
        }
        ++this.tickCounter;
        for (Entity entity : this.mc.world.getEntities()) {
            BlockPos pos;
            int blockLight;
            String translationKey = entity.getType().getTranslationKey();
            if (!translationKey.contains("firefly") && !translationKey.contains("glow") && !entity.getType().toString().contains("firefly") || (blockLight = this.mc.world.getLightLevel(LightType.BLOCK, pos = entity.getBlockPos())) < (Integer)this.lightThreshold.get()) continue;
            boolean seenByOtherPlayer = false;
            for (PlayerEntity player : this.mc.world.getPlayers()) {
                double zDist;
                double yDist;
                double dist;
                double distanceSq;
                if (player == this.mc.player || !((distanceSq = (dist = player.getX() - entity.getX()) * dist + (yDist = player.getY() - entity.getY()) * yDist + (zDist = player.getZ() - entity.getZ()) * zDist) <= (Double)this.renderDistance.get() * (Double)this.renderDistance.get())) continue;
                seenByOtherPlayer = true;
                break;
            }
            if (!seenByOtherPlayer) continue;
            ChunkPos chunkPos = new ChunkPos(pos);
            this.markedChunks.put(chunkPos, this.tickCounter);
        }
        this.markedChunks.entrySet().removeIf(e -> this.tickCounter - (Long)e.getValue() > (long)((Integer)this.markTimeoutTicks.get()).intValue());
    }

    public Map<ChunkPos, Long> getMarkedChunks() {
        return this.markedChunks;
    }
}

