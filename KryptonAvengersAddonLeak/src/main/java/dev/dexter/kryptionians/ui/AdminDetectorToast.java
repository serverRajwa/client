package dev.dexter.kryptionians.ui;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.Toast.Visibility;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public final class AdminDetectorToast implements Toast {
  private static final int WIDTH = 196;
  private static final int HEIGHT = 40;
  private static final int PAD = 6;
  private static final int HEAD_SIZE = 24;
  private static final int TEXT_X = 36;
  private static final int BAR_Y = 37;
  private static final int BAR_H = 2;
  private static final int BG_TOP = -536476150;
  private static final int BG_BOTTOM = Integer.MIN_VALUE;
  private static final int ONLINE = -12723068;
  private static final int OFFLINE = -38037;
  private static final int BAR_BG = -15461352;
  private static final long DURATION_MS = 5500L;
  private static final long SLIDE_MS = 320L;
  private final String adminKey;
  private final String message;
  private final boolean online;
  private final SoundInstance sound;
  private final int accentColor;
  private long start = -1L;
  private boolean playedSound;
  private Visibility visibility = Visibility.HIDE;

  public AdminDetectorToast(String adminKey, boolean online, boolean playSound) {
    this.adminKey = adminKey;
    this.online = online;
    String name = AdminHeadTextures.displayName(adminKey);
    this.message = name + (online ? " Is Now Online" : " Is Now Offline");
    this.accentColor = online ? -12723068 : -38037;
    if (!playSound) {
      this.sound = null;
    } else if (online) {
      this.sound = SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 0.55F, 1.15F);
    } else {
      this.sound = SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_OUT, 0.45F, 0.9F);
    }
  }

  public int width() {
    return 196;
  }

  public int height() {
    return 40;
  }

  public Visibility getWantedVisibility() {
    return this.visibility;
  }

  public void update(ToastManager manager, long time) {
    if (this.start == -1L) {
      this.start = time;
    }

    this.visibility = time - this.start >= 5500L ? Visibility.HIDE : Visibility.SHOW;
    if (!this.playedSound && this.sound != null && MeteorClient.mc.getSoundManager() != null) {
      MeteorClient.mc.getSoundManager().play(this.sound);
      this.playedSound = true;
    }
  }

  public void render(GuiGraphics context, Font textRenderer, long time) {
    if (this.start >= 0L) {
      long elapsed = time - this.start;
      float life = Mth.clamp((float) elapsed / 5500.0F, 0.0F, 1.0F);
      float slide = easeOut(Mth.clamp((float) elapsed / 320.0F, 0.0F, 1.0F));
      int slideOffset = (int) ((1.0F - slide) * 14.0F);
      float fadeIn = Mth.clamp((float) elapsed / 180.0F, 0.0F, 1.0F);
      float fadeOut = elapsed > 5050L ? 1.0F - (float) (elapsed - 5050L) / 450.0F : 1.0F;
      float alpha = fadeIn * fadeOut;
      int panelAlpha = (int) (alpha * 255.0F);
      if (panelAlpha > 4) {
        context.pose().pushMatrix();
        context.pose().translate((float) slideOffset, 0.0F);
        this.drawPanel(context, panelAlpha);
        int headX = 8;
        int headY = 8;
        AdminHeadTextures.drawHead(context, this.adminKey, headX, headY, 24);
        int textMax = 154;
        String line = textRenderer.plainSubstrByWidth(this.message, textMax);
        int accent = applyAlpha(this.accentColor, panelAlpha);
        context.drawString(textRenderer, line, 36, 14, accent, true);
        String status = this.online ? "Joined tab" : "Left tab";
        context.drawString(textRenderer, status, 36, 26, applyAlpha(-5592406, panelAlpha), false);
        int barLeft = 6;
        int barRight = 190;
        int barWidth = barRight - barLeft;
        int fill = Math.max(0, (int) ((float) barWidth * (1.0F - life)));
        context.fill(barLeft, 37, barRight, 39, applyAlpha(-15461352, panelAlpha));
        if (fill > 0) {
          context.fill(barLeft, 37, barLeft + fill, 39, accent);
        }

        context.pose().popMatrix();
      }
    }
  }

  private void drawPanel(GuiGraphics context, int alpha) {
    int top = applyAlpha(-536476150, alpha);
    int bottom = applyAlpha(Integer.MIN_VALUE, alpha);
    int radius = 5;

    for (int row = 0; row < 40; row++) {
      int inset = cornerInset(row, 40, radius);
      float t = (float) row / 39.0F;
      int color = lerpColor(top, bottom, t);
      context.fill(inset, row, 196 - inset, row + 1, color);
    }
  }

  private static int cornerInset(int row, int height, int radius) {
    int fromBottom = height - 1 - row;
    int dist = Math.min(row, fromBottom);
    if (dist >= radius) {
      return 0;
    } else {
      double dy = (double) (radius - dist);
      return (int)
          Math.round(
              (double) radius - Math.sqrt(Math.max(0.0, (double) (radius * radius) - dy * dy)));
    }
  }

  private static int applyAlpha(int color, int alpha) {
    int rgb = color & 16777215;
    return Math.min(alpha, color >>> 24) << 24 | rgb;
  }

  private static int lerpColor(int from, int to, float t) {
    int fa = from >>> 24 & 0xFF;
    int fr = from >>> 16 & 0xFF;
    int fg = from >>> 8 & 0xFF;
    int fb = from & 0xFF;
    int ta = to >>> 24 & 0xFF;
    int tr = to >>> 16 & 0xFF;
    int tg = to >>> 8 & 0xFF;
    int tb = to & 0xFF;
    int a = (int) ((float) fa + (float) (ta - fa) * t);
    int r = (int) ((float) fr + (float) (tr - fr) * t);
    int g = (int) ((float) fg + (float) (tg - fg) * t);
    int b = (int) ((float) fb + (float) (tb - fb) * t);
    return a << 24 | r << 16 | g << 8 | b;
  }

  private static float easeOut(float t) {
    float inv = 1.0F - t;
    return 1.0F - inv * inv * inv;
  }
}
