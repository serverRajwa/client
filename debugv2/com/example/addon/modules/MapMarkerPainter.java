package com.example.addon.modules;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.class_243;

class MapMarkerPainter {
   private final RegionMap regionMap2;

   public MapMarkerPainter(RegionMap regionMap, RegionMap a) {
      a.regionMap2 = a;
   }

   void handler2(MapViewport a, class_243 a, float a, SettingColor a) {
      double d = (a.field_1352 + 225000.0) / 50000.0;
      double score = (a.field_1350 + 225000.0) / 50000.0;
      if (d >= 0.0 && d < 9.0 && score >= 0.0 && score < 9.0) {
         a.handler2(
            (int)((double)a.I + d * (double)a.regionMap2), (int)((double)a.D + score * (double)a.regionMap2), Math.toRadians((double)(-a)), a
         );
      }
   }
}
