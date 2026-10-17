package com.example.addon.mixin;

import com.example.addon.gui.SnowBrightGuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({GuiThemes.class})
public class GuiThemesMixin {
   private static final String DEXTER_MARK_1 = "DexterWasHere!";
   @Redirect(
      method = {"init"},
      at = @At(
         value = "NEW",
         target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;"
      )
   )
   private static MeteorGuiTheme snowAddon$replaceDefaultTheme() {
      return new SnowBrightGuiTheme();
   }
}
