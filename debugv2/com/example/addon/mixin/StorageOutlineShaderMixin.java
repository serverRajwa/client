package com.example.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.StorageESP;
import meteordevelopment.meteorclient.utils.render.postprocess.StorageOutlineShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({StorageOutlineShader.class})
public class StorageOutlineShaderMixin {
   @Inject(
      method = {"shouldDraw"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void snowAddon$guardNullStorageEsp(CallbackInfoReturnable<Boolean> cir) {
      if (Modules.get().get(StorageESP.class) == null) {
         cir.setReturnValue(false);
      }
   }
}
