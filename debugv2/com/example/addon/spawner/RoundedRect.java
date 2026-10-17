package com.example.addon.spawner;

import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.render.color.Color;

public final class RoundedRect {
   private RoundedRect() {
   }

   public static void fill(Renderer2D r, double x, double y, double w, double h, double radius, Color c) {
      double rad = Math.min(radius, Math.min(w, h) / 2.0);
      if (rad <= 0.0) {
         r.quad(x, y, w, BlockPosRef, c);
      } else {
         int segs = 6;
         double d = (Math.PI / 2) / (double)segs;
         r.quad(x + rad, y + rad, w - 2.0 * rad, h - 2.0 * rad, c);
         r.quad(x + rad, y, w - 2.0 * rad, rad, c);
         r.quad(x + rad, y + h - rad, w - 2.0 * rad, rad, c);
         r.quad(x, y + rad, rad, h - 2.0 * rad, c);
         r.quad(x + w - rad, y + rad, rad, h - 2.0 * rad, c);
         cornerFan(r, x + rad, y + rad, rad, Math.PI / 2, Math.PI, d, c);
         cornerFan(r, x + w - rad, y + rad, rad, 0.0, Math.PI / 2, d, c);
         cornerFan(r, x + w - rad, y + h - rad, rad, -Math.PI / 2, 0.0, d, c);
         cornerFan(r, x + rad, y + h - rad, rad, Math.PI, Math.PI * 3.0 / 2.0, d, c);
      }
   }

   private static void cornerFan(Renderer2D r, double cx, double cy, double rad, double a0, double a1, double d, Color c) {
      int value = (int)Math.ceil(Math.abs(a1 - a0) / d);
      if (value < 1) {
         value = 1;
      }

      double actualStep = (a1 - a0) / (double)value;

      for (int i = 0; i < value; i++) {
         double t0 = a0 + (double)i * actualStep;
         double t1 = a0 + (double)(i + 1) * actualStep;
         double x0 = cx + Math.cos(t0) * rad;
         double y0 = cy - Math.sin(t0) * rad;
         double x1 = cx + Math.cos(t1) * rad;
         double y1 = cy - Math.sin(t1) * rad;
         r.triangle(cx, cy, x0, y0, x1, y1, c);
      }
   }
}
