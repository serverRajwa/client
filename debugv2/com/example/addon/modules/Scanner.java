package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.ChunkPos;
public class Scanner extends Module {
   private final List<ChunkPos> blockList ;
   private final Map<Long, Integer> G;
   private final Setting<Boolean> setting ;
   private final Setting<Boolean> setting2 ;
   private final SettingGroup settingGroup ;
   private final Setting<Integer> J;
   private final Setting<Boolean> setting3 ;
   private final Setting<Boolean> M;
   private final Setting<Boolean> H;
   private final Setting<Boolean> E;
   private final SettingGroup settingGroup2 ;
   private final Setting<Boolean> K;
   private final Setting<ShapeMode> B;
   private final Setting<SettingColor> A;
   private int cooldownTicks ;
   private final Setting<Boolean> setting4 ;
   private final Setting<SettingColor> F;
   private final Setting<Boolean> setting5 ;
   private int D;
   private final Setting<Integer> C;
   private ChunkPos I;
   private final Setting<Integer> integerSetting;

   public Scanner() {
      super(AddonTemplate.F, "chunk-finder", "Scans nearby chunks for potential player bases using a precise scoring system.");
      a.e = a.settings.getDefaultGroup();
      a.a = a.settings.createGroup("Render");
      a.C = a.e
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("scan-radius")).description("Scan Radius")).defaultValue10)
               .min2
               .max32
               .build()
         );
      a.integerSetting = a.e
         .add(((Builder)((Builder)((Builder)new Builder().name("min-coord")).description("Min Score")).defaultValue10).min1.max80.build());
      a.J = a.e
         .add(((Builder)((Builder)((Builder)new Builder().name("max-chunks")).description("Max Chunks")).defaultValue(3)).min(1).max8.build());
      a.B = a.a
         .add(
            ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("shape-mode"))
                     .description("Wie die Boxen gerendert werden."))
                  .defaultValue(ShapeMode.Both))
               .build()
         );
      a.F = a.a
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("side-color"))
                  .description("Farbe der Box-Seiten."))
               .defaultValue(new SettingColor(150, 70, 255, 75))
               .build()
         );
      a.A = a.a
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("line-color"))
                  .description("Farbe der Box-Kanten und Tracer."))
               .defaultValue(new SettingColor(150, 70, 255, 255))
               .build()
         );
      a.g = a.a
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("tracers"))
                     .description("Zeigt Tracer-Linien zu den Chunks."))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.M = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("storage"))
                     .description("Storage"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.E = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("obsidian"))
                     .description("Obsidian"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.c = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("redstone"))
                     .description("Redstone"))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.H = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("portals"))
                     .description("Portals"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.K = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("amethyst"))
                     .description("Amethyst"))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.b = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("spawners"))
                     .description("Spawners"))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.d = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("full-beenests"))
                     .description("Full Beenests"))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.i = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("mobs-under-deepslate"))
                     .description("Mobs Under Deepslate"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.G = new HashMap<>();
      a.j = new ArrayList<>();
   }

   public void onTick() {
      a.m();
   }

   public void handler2() {
      a.m();
   }
}
