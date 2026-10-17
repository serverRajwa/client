
package com.example.donut.modules.Donut;

import com.example.donut.DonutAddon;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import org.joml.Vector3d;

public class SpawnerNotifier
extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgRender;
    private final Setting<Integer> maxDistance;
    private final Setting<Boolean> renderBox;
    private final Setting<Boolean> renderBeam;
    private final Setting<Double> beamHeight;
    private final Setting<Double> beamRadius;
    private final Setting<Integer> fillOpacity;
    private final Setting<Boolean> renderNametag;
    private final Setting<Double> nametagScale;
    private final Setting<SettingColor> nametagColor;
    private final Setting<SettingColor> color;
    private final Color lineColor;
    private final Color sideColor;
    private final List<BlockPos> spawnerPositions;
    private int count;

    public SpawnerNotifier() {
        super(DonutAddon.CATEGORY, "spawner-notifier", "Highlights loaded mob spawners.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgRender = this.settings.createGroup("Visuals");
        this.maxDistance = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("max-distance")).description("Maximum distance to render spawners. Set to 0 for loaded chunks only.")).defaultValue(0)).min(0).sliderMax(512).build());
        this.renderBox = this.sgRender.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("render-box")).description("Renders a box around the spawner.")).defaultValue(true)).build());
        this.renderBeam = this.sgRender.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("render-beam")).description("Renders a vertical beam above the spawner.")).defaultValue(true)).build());
        this.beamHeight = this.sgRender.add((Setting)((DoubleSetting.Builder)((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name("beam-height")).description("Height of the vertical beam.")).visible(() -> this.renderBeam.get())).defaultValue(256.0).range(8.0, 512.0).sliderRange(32.0, 320.0).build());
        this.beamRadius = this.sgRender.add((Setting)((DoubleSetting.Builder)((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name("beam-radius")).description("Distance between the center and outer beam lines.")).visible(() -> this.renderBeam.get())).defaultValue(0.18).range(0.0, 0.5).sliderRange(0.0, 0.5).build());
        this.fillOpacity = this.sgRender.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("fill-opacity")).description("Opacity of the box fill.")).visible(() -> this.renderBox.get())).defaultValue(45)).range(0, 255).sliderMax(255).build());
        this.renderNametag = this.sgRender.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("nametag")).description("Renders a Spawner nametag above each spawner.")).defaultValue(true)).build());
        this.nametagScale = this.sgRender.add((Setting)((DoubleSetting.Builder)((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name("nametag-scale")).description("Scale of the nametag text.")).visible(() -> this.renderNametag.get())).defaultValue(1.0).range(0.1, 4.0).sliderRange(0.25, 3.0).build());
        this.nametagColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("nametag-color")).description("Color of the nametag text.")).visible(() -> this.renderNametag.get())).defaultValue(new SettingColor(255, 255, 255, 255)).build());
        this.color = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("color")).description("Color of the spawner marker.")).defaultValue(new SettingColor(255, 0, 0, 255)).build());
        this.lineColor = new Color(255, 0, 0, 255);
        this.sideColor = new Color(255, 0, 0, 45);
        this.spawnerPositions = new ArrayList<BlockPos>();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        this.count = 0;
        this.spawnerPositions.clear();
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (!(blockEntity instanceof MobSpawnerBlockEntity) || !this.isInRange(blockEntity.getPos())) continue;
            BlockPos pos = blockEntity.getPos();
            this.lineColor.set((Color)this.color.get());
            this.sideColor.set((Color)this.color.get());
            this.sideColor.a = (Integer)this.fillOpacity.get();
            if (((Boolean)this.renderBox.get()).booleanValue()) {
                event.renderer.box(pos, this.sideColor, this.lineColor, ShapeMode.Both, 0);
            }
            if (((Boolean)this.renderBeam.get()).booleanValue()) {
                this.renderBeam(event, pos);
            }
            if (((Boolean)this.renderNametag.get()).booleanValue()) {
                this.spawnerPositions.add(pos);
            }
            ++this.count;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!((Boolean)this.renderNametag.get()).booleanValue() || this.spawnerPositions.isEmpty()) {
            return;
        }
        Vector3d vec = new Vector3d();
        for (BlockPos pos : this.spawnerPositions) {
            vec.set((double)pos.getX() + 0.5, (double)pos.getY() + 1.2, (double)pos.getZ() + 0.5);
            if (!NametagUtils.to2D((Vector3d)vec, (double)((Double)this.nametagScale.get()))) continue;
            NametagUtils.begin((Vector3d)vec);
            TextRenderer.get().begin(1.0, false, true);
            String label = "[Spawner]";
            double width = TextRenderer.get().getWidth(label) / 2.0;
            TextRenderer.get().render(label, -width, 0.0, (Color)this.nametagColor.get(), true);
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    private boolean isInRange(BlockPos pos) {
        double z;
        double y;
        if (this.mc.player == null || (Integer)this.maxDistance.get() <= 0) {
            return true;
        }
        double x = this.mc.player.getX() - ((double)pos.getX() + 0.5);
        return x * x + (y = this.mc.player.getY() - ((double)pos.getY() + 0.5)) * y + (z = this.mc.player.getZ() - ((double)pos.getZ() + 0.5)) * z <= (double)((Integer)this.maxDistance.get() * (Integer)this.maxDistance.get());
    }

    private void renderBeam(Render3DEvent event, BlockPos pos) {
        double x = (double)pos.getX() + 0.5;
        double y1 = (double)pos.getY() + 0.5;
        double y2 = Math.min(y1 + (Double)this.beamHeight.get(), (double)(this.mc.world.getBottomY() + this.mc.world.getHeight()));
        double z = (double)pos.getZ() + 0.5;
        double radius = (Double)this.beamRadius.get();
        event.renderer.line(x, y1, z, x, y2, z, this.lineColor);
        if (radius <= 0.0) {
            return;
        }
        event.renderer.line(x - radius, y1, z - radius, x - radius, y2, z - radius, this.lineColor);
        event.renderer.line(x - radius, y1, z + radius, x - radius, y2, z + radius, this.lineColor);
        event.renderer.line(x + radius, y1, z - radius, x + radius, y2, z - radius, this.lineColor);
        event.renderer.line(x + radius, y1, z + radius, x + radius, y2, z + radius, this.lineColor);
    }

    public String getInfoString() {
        return Integer.toString(this.count);
    }
}

