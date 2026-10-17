package com.example.addon.mixin;

import com.example.addon.spawner.RoundedPill;
import java.util.List;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.ActiveModulesHud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ActiveModulesHud.class})
public class ActiveModulesHudMixin {
   private static final String DEXTER_MARK_1 = "DexterWasHere!";
   @Shadow
   @Final
   private List<Module> modules;
   @Shadow
   private Setting<Boolean> shadow;

   @Inject(
      method = {"renderModule"},
      at = {@At(
         value = "INVOKE",
         target = "Lmeteordevelopment/meteorclient/systems/hud/HudRenderer;text(Ljava/lang/String;DDLmeteordevelopment/meteorclient/utils/render/color/Color;ZD)D",
         ordinal = 0
      )}
   )
   private void snowaddon$roundedModuleRowBg(HudRenderer renderer, int index, double x, double y, CallbackInfo ci) {
      ActiveModulesHud self = (ActiveModulesHud)this;
      ActiveModulesHudInvoker inv = (ActiveModulesHudInvoker)self;
      Module module = this.modules.get(index);
      double w = inv.invokeGetModuleWidth(renderer, module);
      double scale = inv.invokeGetScale();
      double h = renderer.textHeight((Boolean)this.shadow.get(), scale);
      double padX = 4.0;
      double padY = 2.0;
      Color bg = new Color(12, 16, 24, 228);
      Color accent = new Color(72, 170, 255, 215);
      RoundedPill.drawHud(renderer, x - padX, y - padY, w + padX * 2.0, h + padY * 2.0, bg);
      renderer.line(x - padX + 3.0, y - padY + 2.0, x - padX + w + padX * 2.0 - 3.0, y - padY + 2.0, accent);
   }
}
