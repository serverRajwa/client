package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.KeybindSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;

public class AutoRelog extends Module {
   private final SettingGroup F;
   private int cooldownTicks ;
   private final Setting<Integer> D;
   private boolean C;
   private boolean I;
   private final Setting<Keybind> keybind;

   public AutoRelog() {
      super(AddonTemplate.C, "auto-relog", "Instantly disconnects you from the server via keybind or low Y-level.");
      a.F = a.settings.getDefaultGroup();
      a.keybind = a.F
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("relog-button")).description("Press this key to disconnect immediately."))
                  .defaultValue(Keybind.none()))
               .build()
         );
      a.D = a.F
         .add(
            ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("relog-height"))
                     .description("Disconnects you automatically if your Y-level drops below this value."))
                  .defaultValue-64)
               .min-128
               .max320
               .sliderMin-64
               .sliderMax(0)
               .build()
         );
      int value = 0;
      a.i = a.I = (boolean)(0);
      a.C = (boolean)value;
   }

   public void onTick() {
      a.C = (boolean)0;
   }

   public void handler2() {
      a.i = 200;
      a.C = (boolean)(a.mc.world != null ? 1 : 0);
   }
}
