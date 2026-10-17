package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.concurrent.TimeUnit;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class ResetHome extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> homeNumber = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("home-number")).description("Which home number to delete and re-set.")).defaultValue(1))
            .min(1)
            .sliderMax(20)
            .build()
      );

   public ResetHome() {
      super(AddonTemplate.C, "reset-home", "Deletes and re-sets a numbered home at your current position via keybind.");
   }

   public void onActivate() {
      if (this.mc.player == null) {
         this.toggle();
      } else {
         String homeName = " " + this.homeNumber.get();
         ChatUtils.info("Resetting " + homeName + "...", new Object[0]);
         this.mc.player.networkHandler.sendChatCommand("delhome " + homeName);
         MeteorExecutor.execute(() -> {
            try {
               TimeUnit.MILLISECONDS.sleep(600L);
            } catch (InterruptedException ex) {
            }

            if (this.mc.player != null) {
               this.mc.player.networkHandler.sendChatCommand("sethome " + homeName);
               ChatUtils.info(homeName + " reset.", new Object[0]);
            }

            this.toggle();
         });
      }
   }
}
