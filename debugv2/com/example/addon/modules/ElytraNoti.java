package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
public class ElytraNoti extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> durabilityPercent = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("durability-percent")).description("Durability percentage threshold to trigger warning."))
               .defaultValue(15))
            .min(1)
            .max(100)
            .sliderRange(1, 100)
            .build()
      );
   private int warningTicks;
   private boolean wasBelowThreshold;

   public ElytraNoti() {
      super(AddonTemplate.C, "elytra-noti", "Warns you when your elytra durability is low.");
   }

   public void onActivate() {
      this.warningTicks = 0;
      this.wasBelowThreshold = false;
   }

   public void onDeactivate() {
      this.warningTicks = 0;
   }

   @EventHandler
   private void on(Pre event) {
      if (this.mc.player != null) {
         ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
         if (!chest.isEmpty() && chest.isOf(Items.ELYTRA)) {
            int maxDmg = chest.getMaxDamage();
            int currentDmg = chest.getDamage();
            int remaining = maxDmg - currentDmg;
            int pct = remaining * 100 / maxDmg;
            if (pct < (Integer)this.durabilityPercent.get()) {
               if (!this.wasBelowThreshold) {
                  this.warningTicks = 50;
                  this.wasBelowThreshold = true;
               }
            } else {
               this.wasBelowThreshold = false;
               if (this.warningTicks > 0) {
                  this.warningTicks--;
               }
            }
         } else {
            this.warningTicks = 0;
            this.wasBelowThreshold = false;
         }
      }
   }

   @EventHandler
   private void on(Render2DEvent event) {
      if (this.warningTicks > 0) {
         String str = "mend ur elytra";
         int tw = this.mc.textRenderer.getWidth(str);
         int x = (event.drawContext.getScaledWindowWidth() - tw) / 2;
         int y = event.drawContext.getScaledWindowHeight() / 4;
         event.drawContext.drawTextWithShadow(this.mc.textRenderer, str, x, y, 16733525);
         this.warningTicks--;
      }
   }
}
