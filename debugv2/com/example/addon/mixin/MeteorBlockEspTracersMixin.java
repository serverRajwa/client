package com.example.addon.mixin;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.BlockESP;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockESP.class})
public class MeteorBlockEspTracersMixin {
   @Shadow
   @Final
   private Setting<Boolean> tracers;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void snowAddon$defaultTracersOn(CallbackInfo ci) {
      this.tracers.set(true);
   }
}
