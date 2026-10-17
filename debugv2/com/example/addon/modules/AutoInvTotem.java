package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AutoInvTotem extends Module {
   private static final String DEXTER_MARK_3 = "https://leakestan.org/";
   private final Setting<Boolean> H;
   private boolean E;
   private int cooldownTicks ;
   private boolean K;
   private int B;
   private final Setting<Boolean> A;
   private final SettingGroup settingGroup ;
   private boolean autoFlag ;
   private boolean F;
   private final Setting<Boolean> setting ;
   private final Setting<Integer> D;
   private final Setting<Integer> C;
   private int I;
   private final Setting<Integer> integerSetting;

   public void A() {
      a.b();
   }

   public AutoInvTotem() {
      super(AddonTemplate.integerSetting, "auto-inv-totem", "Automatically moves totems to offhand when inventory is opened after a totem pop.");
      a.l = a.settings.getDefaultGroup();
      a.C = a.l
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("delay")).description("Delay in ticks before moving totem (1-20 ticks)")).defaultValue(3))
               .min(1)
               .max20
               .sliderMin1
               .sliderMax20
               .build()
         );
      a.A = a.l
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("move-from-hotbar"))
                     .description("Also move totems from hotbar slots"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.i = a.l
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("disable-logs"))
                     .description("Disable chat messages about totem movements"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.H = a.l
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("open-inv"))
                     .description("Automatically open inventory when totem pops"))
                  .defaultValue(Boolean.valueOf((boolean)0)))
               .build()
         );
      a.D = a.l
         .add(
            ((Builder)((Builder)((Builder)((Builder)new Builder().name("inv-open-delay"))
                        .description("Ticks to wait before opening inventory after totem pop (1-10 ticks)"))
                     .defaultValue(2))
                  .min1
                  .max10
                  .sliderMin1
                  .sliderMax10
                  .visible(a.H::get))
               .build()
         );
      a.integerSetting = a.l
         .add(
            ((Builder)((Builder)((Builder)((Builder)new Builder().name("inv-close-delay"))
                        .description("Ticks to wait before closing inventory after opening (5-20 ticks)"))
                     .defaultValue8)
                  .min(5)
                  .max20
                  .sliderMin(5)
                  .sliderMax20
                  .visible(a.H::get))
               .build()
         );
   }

   public void handler2() {
      a.m();
   }
}
