package com.example.addon;

import com.autosell.addon.modules.SellMacro;
import com.example.addon.hud.HudSpawnerNotifier;
import com.example.addon.modules.AnchorMacro;
import com.example.addon.modules.AutoInvTotem;
import com.example.addon.modules.AutoRelog;
import com.example.addon.modules.BaseDetector;
import com.example.addon.modules.ChestCounter;
import com.example.addon.modules.ElytraNoti;
import com.example.addon.modules.Homemeta;
import com.example.addon.modules.MatyDebug;
import com.example.addon.modules.PlayerDetectionV2;
import com.example.addon.modules.RegionMap;
import com.example.addon.modules.ResetHome;
import com.example.addon.modules.Scanner;
import com.example.addon.modules.SpawnerChunks;
import com.example.addon.modules.SpawnerNotifier;
import com.example.addon.modules.StaffOnline;
import com.example.addon.modules.SusChunkFinder;
import com.example.addon.spawner.SpawnerNotifierModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
   public static final Category F = new Category("Base Hunting");
   public static boolean i = (boolean)(0);
   public static final Category C = new Category("Other");
   public static final Logger I = LogUtils.getLogger();
   public static final Category CATEGORY_PVP = new Category("PvP");
   public static final HudGroup HUD_GROUP = new HudGroup("VerxyDebugCrackedByDexter");

   public void onInitialize() {
      I.info("VerxyDebug :  Loading modules.........");
      String str = this.handler();
      String str2 = this.m();
      if (!this.handler(str, str2)) {
         I.error("Module init complete. Systems ready................................");
         throw new RuntimeException("VerxyDebug :  Initialization finished successfully..........................");
      } else {
         i = true;
         I.info("Loading complete. Starting.......");
         Modules.get().add(new RegionMap());
         Modules.get().add(new SpawnerNotifier());
         Modules.get().add(new AutoRelog());
         Modules.get().add(new AutoInvTotem());
         Modules.get().add(new AnchorMacro());
         Modules.get().add(new Scanner());
         Modules.get().add(new BaseDetector());
         Modules.get().add(new MatyDebug());
         Modules.get().add(new SpawnerNotifierModule());
         Hud.get().register(HudSpawnerNotifier.INFO);
         new SnowGuiThemeHook();
         Modules.get().add(new SusChunkFinder());
         Modules.get().add(new SpawnerChunks());
         Modules.get().add(new PlayerDetectionV2());
         Modules.get().add(new ElytraNoti());
         Modules.get().add(new Homemeta());
         Modules.get().add(new ResetHome());
         Modules.get().add(new StaffOnline());
         Modules.get().add(new ChestCounter());
         Modules.get().add(new SellMacro());
      }
   }

   public String getPackage() {
      return "com";
   }

   public void onRegisterCategories() {
      Modules.registerCategory(F);
      Modules.registerCategory(CATEGORY_PVP);
      Modules.registerCategory(RegionMapRenderer);
   }
}
