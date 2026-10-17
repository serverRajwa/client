package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Homemeta extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> homeNumber = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("home-number")).description("Which home slot to use.")).defaultValue(3))
            .min(1)
            .max(10)
            .sliderMin(1)
            .sliderMax(10)
            .build()
      );
   private final Setting<Double> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("delay"))
               .description("Delay between commands in seconds."))
            .defaultValue(0.7)
            .min(0.25)
            .max(1.0)
            .sliderMin(0.25)
            .sliderMax(1.0)
            .build()
      );

   public Homemeta() {
      super(AddonTemplate.C, "home-meta", "Runs my home reset sequence.");
   }

   public void onActivate() {
      if (this.mc.player == null) {
         this.toggle();
      } else {
         int home = (Integer)this.homeNumber.get();
         String[] commands = new String[]{"delhome " + home, "sethome " + home, "rtp", "home " + home};
         Thread worker = new Thread(() -> {
            for (String command : commands) {
               this.mc.execute(() -> {
                  if (this.mc.player != null) {
                     this.mc.player.networkHandler.sendChatCommand(command);
                  }
               });

               try {
                  Thread.sleep((long)((Double)this.delay.get() * 1000.0));
               } catch (InterruptedException ex) {
               }
            }

            this.mc.execute(this::toggle);
         });
         worker.start();
      }
   }
}
