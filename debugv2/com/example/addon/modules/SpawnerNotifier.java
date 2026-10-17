package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.BlockPos;
public class SpawnerNotifier extends Module {
   private final Setting<Boolean> K;
   private final List<<unrepresentable>> B;
   private final Setting<Boolean> A;
   private final Setting<SettingColor> color ;
   private final Setting<Boolean> setting ;
   private final Setting<SettingColor> F;
   private final Set<BlockPos> seen ;
   private final SettingGroup D;
   private final Setting<Boolean> C;
   private final SettingGroup I;
   private final Setting<Boolean> booleanSetting;

   public void handler2() {
      a.i.clear();
      a.B.clear();
   }

   public SpawnerNotifier() {
      super(AddonTemplate.F, "spawner-notifier", "Advanced spawner detection with stable native tracers.");
      a.D = a.settings.getDefaultGroup();
      a.I = a.settings.createGroup("Rendering");
      a.c = a.D.add(((Builder)((Builder)new Builder().name("chat-alert")).defaultValue(Boolean.valueOf((boolean)(1)))).build());
      a.C = a.D.add(((Builder)((Builder)new Builder().name("toast-alert")).defaultValue(Boolean.valueOf((boolean)(1)))).build());
      a.booleanSetting = a.D.add(((Builder)((Builder)new Builder().name("sound-alert")).defaultValue(Boolean.valueOf((boolean)1))).build());
      a.K = a.I.add(((Builder)((Builder)new Builder().name("pillar-alert")).defaultValue(Boolean.valueOf((boolean)1))).build());
      a.F = a.I
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("pillar-color"))
               .defaultValue(new SettingColor(255, 255, 0, 100))
               .build()
         );
      a.A = a.I.add(((Builder)((Builder)new Builder().name("tracers")).defaultValue(Boolean.valueOf((boolean)1))).build());
      a.l = a.I
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("tracer-color"))
               .defaultValue(new SettingColor(0, 255, 200, 255))
               .build()
         );
      a.i = new HashSet<>();
      a.B = new ArrayList<>();
   }
}
