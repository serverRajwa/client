package dev.dexter.kryptionians.ui;

import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.render.color.Color;

public final class RoundedPanel {
  private static final int SEGMENTS = 24;

  private RoundedPanel() {}

  public static void fillVerticalGradient(
      double x, double y, double w, double h, double radius, Color top, Color bottom) {
    if (w <= 0 || h <= 0) return;
    double r = Math.max(0, Math.min(radius, Math.min(w, h) / 2.0));

    Renderer2D.COLOR.begin();

    if (w > 2 * r) {
      Renderer2D.COLOR.quad(x + r, y, w - 2 * r, h, top, top, bottom, bottom);
    }
    if (h > 2 * r && r > 0) {
      Renderer2D.COLOR.quad(x, y + r, r, h - 2 * r, top, top, bottom, bottom);
      Renderer2D.COLOR.quad(x + w - r, y + r, r, h - 2 * r, top, top, bottom, bottom);
    } else if (r <= 0) {
      Renderer2D.COLOR.quad(x, y, w, h, top, top, bottom, bottom);
      Renderer2D.COLOR.render();
      return;
    }

    cornerFan(x + r, y + r, r, Math.PI, Math.PI * 1.5, top);
    cornerFan(x + w - r, y + r, r, Math.PI * 1.5, Math.PI * 2.0, top);
    cornerFan(x + w - r, y + h - r, r, 0.0, Math.PI * 0.5, bottom);
    cornerFan(x + r, y + h - r, r, Math.PI * 0.5, Math.PI, bottom);

    Renderer2D.COLOR.render();
  }

  public static void fillVerticalGradientArgb(
      double x, double y, double w, double h, double radius, int topArgb, int bottomArgb) {
    fillVerticalGradient(x, y, w, h, radius, fromArgb(topArgb), fromArgb(bottomArgb));
  }

  private static void cornerFan(double cx, double cy, double r, double a0, double a1, Color color) {
    double step = (a1 - a0) / SEGMENTS;
    for (int i = 0; i < SEGMENTS; i++) {
      double aA = a0 + step * i;
      double aB = a0 + step * (i + 1);
      Renderer2D.COLOR.triangle(
          cx,
          cy,
          cx + Math.cos(aA) * r,
          cy + Math.sin(aA) * r,
          cx + Math.cos(aB) * r,
          cy + Math.sin(aB) * r,
          color);
    }
  }

  private static Color fromArgb(int argb) {
    return new Color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
  }
}
