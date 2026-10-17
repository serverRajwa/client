package com.example.addon.spawner;

import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public final class RoundedPill {
   private RoundedPill() {
   }

   public static void drawHud(HudRenderer r, double x, double y, double w, double h, Color fill) {
      double ry = h / 2.0;
      if (w <= ry * 2.0) {
         r.quad(x, y, w, BlockPosRef, fill);
      } else {
         double cxL = x + ry;
         double cxR = x + w - ry;
         double cy = y + ry;
         int segs = 8;
         double halfPi = Math.PI / 2;
         r.quad(cxL, y, cxR - cxL, BlockPosRef, fill);

         for (int i = 0; i < segs; i++) {
            double a0 = halfPi + (double)i * (Math.PI / (double)segs);
            double a1 = halfPi + (double)(i + 1) * (Math.PI / (double)segs);
            double x0 = cxL + Math.cos(a0) * ry;
            double y0 = cy - Math.sin(a0) * ry;
            double x1 = cxL + Math.cos(a1) * ry;
            double y1 = cy - Math.sin(a1) * ry;
            r.triangle(cxL, cy, x0, y0, x1, y1, fill);
         }

         for (int i = 0; i < segs; i++) {
            double a0 = -halfPi + (double)i * (Math.PI / (double)segs);
            double a1 = -halfPi + (double)(i + 1) * (Math.PI / (double)segs);
            double x0 = cxR + Math.cos(a0) * ry;
            double y0 = cy - Math.sin(a0) * ry;
            double x1 = cxR + Math.cos(a1) * ry;
            double y1 = cy - Math.sin(a1) * ry;
            r.triangle(cxR, cy, x0, y0, x1, y1, fill);
         }
      }
   }

   public static void drawWorld(Renderer2D r, double x, double y, double w, double h, Color fill) {
      double ry = h / 2.0;
      if (w <= ry * 2.0) {
         r.quad(x, y, w, BlockPosRef, fill);
      } else {
         double cxL = x + ry;
         double cxR = x + w - ry;
         double cy = y + ry;
         int segs = 8;
         double halfPi = Math.PI / 2;
         r.quad(cxL, y, cxR - cxL, BlockPosRef, fill);

         for (int i = 0; i < segs; i++) {
            double a0 = halfPi + (double)i * (Math.PI / (double)segs);
            double a1 = halfPi + (double)(i + 1) * (Math.PI / (double)segs);
            double x0 = cxL + Math.cos(a0) * ry;
            double y0 = cy - Math.sin(a0) * ry;
            double x1 = cxL + Math.cos(a1) * ry;
            double y1 = cy - Math.sin(a1) * ry;
            r.triangle(cxL, cy, x0, y0, x1, y1, fill);
         }

         for (int i = 0; i < segs; i++) {
            double a0 = -halfPi + (double)i * (Math.PI / (double)segs);
            double a1 = -halfPi + (double)(i + 1) * (Math.PI / (double)segs);
            double x0 = cxR + Math.cos(a0) * ry;
            double y0 = cy - Math.sin(a0) * ry;
            double x1 = cxR + Math.cos(a1) * ry;
            double y1 = cy - Math.sin(a1) * ry;
            r.triangle(cxR, cy, x0, y0, x1, y1, fill);
         }
      }
   }
}
