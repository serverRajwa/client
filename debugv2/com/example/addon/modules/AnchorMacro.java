package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AnchorMacro extends Module {
   private final Setting<Double> B;
   private boolean A;
   private int cooldownTicks ;
   private int cooldownTicks2 ;
   private boolean F;
   private final Setting<Double> setting ;
   private final SettingGroup D;
   private final Setting<Double> C;
   private final Setting<Integer> I;
   private int value2;

   public AnchorMacro() {
      super(AddonTemplate.value2, "anchor-macro", "Automatically charges and explodes respawn anchors.");
      a.D = a.settings.getDefaultGroup();
      a.B = a.D.add(((Builder)new Builder().name("switch-delay")).defaultValue(0.0).min(0.0).max(20.0).sliderMax(20.0).build());
      a.C = a.D.add(((Builder)new Builder().name("glowstone-delay")).defaultValue(0.0).min(0.0).max(20.0).sliderMax(20.0).build());
      a.i = a.D.add(((Builder)new Builder().name("explode-delay")).defaultValue(0.0).min(0.0).max(20.0).sliderMax(20.0).build());
      a.I = a.D
         .add(
            ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("totem-slot"))
                  .defaultValue(1))
               .min(1)
               .max9
               .build()
         );
   }

   public void k() {
      int value = 0;
      int value4 = 0;
      a.f();
      a.F = (boolean)value4;
      a.A = (boolean)value;
   }

   public void m() {
      int value = 0;
      int value4 = 0;
      a.f();
      a.F = (boolean)value4;
      a.A = (boolean)value;
   }
}
