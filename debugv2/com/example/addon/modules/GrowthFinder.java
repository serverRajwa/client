package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.ChunkPos;
public class GrowthFinder extends Module {
   private final Set<ChunkPos> seen ;
   private final Setting<Integer> J;
   private static final int colorArgb = 2;
   private static final int M = 12;
   private static final double H = 30.0;
   private static final int E = 6;
   private final List<ChunkPos> blockList ;
   private static final Color K = new Color(255, 198, 64);
   private ChunkPos B;
   private static final Color A = new Color(203, 64, 255);
   private final Setting<Boolean> setting ;
   private static final int colorArgb2 = 8;
   private final Setting<Boolean> F;
   private final Set<ChunkPos> seen2 ;
   private final Setting<Boolean> D;
   private int C;
   private final Map<ChunkPos, <unrepresentable>> I;
   private final SettingGroup settingGroup;

   public void k() {
      a.f();
   }

   public GrowthFinder() {
      super(AddonTemplate.F, "growth-finder", "Finds bases via plant growth");
      a.settingGroup = a.settings.getDefaultGroup();
      a.D = a.settingGroup
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("render-vines")).description("Scanns Vines")).defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.F = a.settingGroup
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("render-dripstone")).description("Scanns Dripstone"))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.l = a.settingGroup
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("render-berries")).description("Scanns Sweetberrys"))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.J = a.settingGroup
         .add(
            ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("alpha"))
                     .description("Alpha of the chunks"))
                  .defaultValue200)
               .min0
               .max255
               .sliderRange(0, 255)
               .build()
         );
      int value = 0;
      a.e = new HashSet<>();
      a.I = new HashMap<>();
      a.a = new ArrayList<>();
      a.B = null;
      a.C = value;
      a.i = new HashSet<>();
   }

   public void handler2() {
      a.f();
   }
}
