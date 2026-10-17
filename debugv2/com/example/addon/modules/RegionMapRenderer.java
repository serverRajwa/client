package com.example.addon.modules;

import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

class RegionMapRenderer {
   private final RegionMap regionMap2;

   void render2(MapViewport a, RegionAtlas a) {
      MapViewport mapViewport = a;
      Renderer2D.COLOR.begin();

      int i;
      for (int i3 = i = 0; i3 < 81; i3 = i) {
         RegionCell regionCell = a.render3(i);
         Color color;
         (color = new Color(a.render3(regionCell.I))).a = (int)(mapViewport.C * 255.0);
         double d = (double)(mapViewport.I + regionCell.D * mapViewport.regionMap2 + (1));
         double score = (double)(mapViewport.D + regionCell.C * mapViewport.regionMap2 + 1);
         double currentScore = (double)(mapViewport.regionMap2 - 2);
         int value = mapViewport.regionMap2 - 2;
         i++;
         Renderer2D.COLOR.quad(d, score, currentScore, (double)value, color);
      }

      Renderer2D.COLOR.render();
   }

   void renderLabels(MapViewport a, RegionAtlas a, double a) {
      MapViewport mapViewport = a;
      TextRenderer.get().begin(a, (boolean)(0), (boolean)(1));

      int i;
      for (int i3 = i = 0; i3 < 81; i3 = i) {
         RegionCell regionCell;
         String str = String.valueOf((regionCell = a.render3(i)).regionMap2);
         TextRenderer textRenderer = TextRenderer.get();
         double d = (double)(mapViewport.I + regionCell.D * mapViewport.regionMap2) + ((double)mapViewport.regionMap2 - TextRenderer.get().getWidth(str) * a) / 2.0;
         double score = (double)(mapViewport.D + regionCell.C * mapViewport.regionMap2) + ((double)mapViewport.regionMap2 - TextRenderer.get().getHeight() * a) / 2.0;
         i++;
         textRenderer.render(str, d, score, Color.WHITE, (boolean)0);
      }

      TextRenderer.get().end();
   }

   public RegionMapRenderer(RegionMap regionMap, RegionMap a) {
      a.regionMap2 = a;
   }

   void render3(MapViewport a) {
      Color color;
      (color = new Color((Color)a.regionMap2.B.get())).a = (int)(a.C * 255.0);
      Renderer2D.COLOR.begin();
      Renderer2D.COLOR.quad((double)a.I, (double)a.D, (double)a.m(), (double)a.render3(), color);
      Renderer2D.COLOR.render();
   }

   void render4(MapViewport a, SettingColor a) {
      MapViewport mapViewport = a;
      Renderer2D.COLOR.begin();

      int i;
      for (int i3 = i = 0; i3 <= 9; i3 = i) {
         Renderer2D.COLOR.quad((double)(mapViewport.I + i * mapViewport.regionMap2), (double)mapViewport.D, 1.0, (double)mapViewport.render3(), a);
         Renderer2D renderer2D = Renderer2D.COLOR;
         double d = (double)mapViewport.I;
         double score = (double)(mapViewport.D + i * mapViewport.regionMap2);
         double currentScore = (double)mapViewport.m();
         i++;
         renderer2D.quad(d, score, currentScore, 1.0, a);
      }

      Renderer2D.COLOR.render();
   }
}
