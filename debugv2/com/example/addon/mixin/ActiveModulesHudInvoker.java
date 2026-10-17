package com.example.addon.mixin;

import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.ActiveModulesHud;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ActiveModulesHud.class})
public interface ActiveModulesHudInvoker {
   @Invoker("getModuleWidth")
   double invokeGetModuleWidth(HudRenderer hudRenderer, Module module);

   @Invoker("getScale")
   double invokeGetScale();
}
