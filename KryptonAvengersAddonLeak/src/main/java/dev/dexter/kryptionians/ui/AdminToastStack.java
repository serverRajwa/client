package dev.dexter.kryptionians.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class AdminToastStack {
  private static final int WIDTH = 210;
  private static final int HEIGHT = 40;
  private static final int GAP = 2;
  private static final int MARGIN = 6;
  private static final int RADIUS = 10;
  private static final long DURATION_MS = 5500L;
  private static final long SLIDE_MS = 220L;

  private static final int BG = 0xF00A0A10;
  private static final int ONLINE = 0xFF3DDC74;
  private static final int OFFLINE = 0xFFFF5A4A;
  private static final int SUBTLE = 0xFFB0B0BC;
  private static final int BAR_BG = 0xFF0A0A0C;

  private final List<Entry> entries = new ArrayList<>();

  public void push(String adminKey, boolean online, boolean playSound) {
    entries.add(0, new Entry(adminKey, online, playSound));
    while (entries.size() > 6) entries.remove(entries.size() - 1);
  }

  public void clear() {
    entries.clear();
  }

  public void render(GuiGraphics context) {
    Minecraft mc = MeteorClient.mc;
    if (entries.isEmpty() || mc == null || mc.getWindow() == null || mc.font == null) return;

    try {
      long now = System.currentTimeMillis();
      Iterator<Entry> it = entries.iterator();
      while (it.hasNext()) {
        Entry e = it.next();
        if (now - e.start >= DURATION_MS) it.remove();
      }
      if (entries.isEmpty()) return;

      int y = MARGIN;
      for (Entry e : entries) {
        long elapsed = now - e.start;
        float life = Math.min(1.0f, elapsed / (float) DURATION_MS);
        float slide = easeOut(Math.min(1.0f, elapsed / (float) SLIDE_MS));
        float fadeIn = Math.min(1.0f, elapsed / 140.0f);
        float fadeOut =
            elapsed > DURATION_MS - 400L ? 1.0f - (elapsed - (DURATION_MS - 400L)) / 400.0f : 1.0f;
        float alpha = fadeIn * fadeOut;
        int panelA = Math.max(0, Math.min(255, (int) (alpha * 255)));
        if (panelA <= 4) {
          y += HEIGHT + GAP;
          continue;
        }

        if (!e.playedSound && e.playSound) {
          if (mc.getSoundManager() != null) {
            mc.getSoundManager()
                .play(
                    SimpleSoundInstance.forUI(
                        e.online ? SoundEvents.UI_TOAST_IN : SoundEvents.UI_TOAST_OUT,
                        e.online ? 1.12f : 0.92f,
                        e.online ? 0.5f : 0.4f));
          }
          e.playedSound = true;
        }

        int slideX = (int) ((1.0f - slide) * 12.0f);
        int x = MARGIN + slideX;
        int accent = e.online ? ONLINE : OFFLINE;

        SmoothRoundRect.draw(context, x, y, WIDTH, HEIGHT, RADIUS, withAlpha(BG, panelA));
        context.fill(x + 1, y + RADIUS, x + 3, y + HEIGHT - RADIUS, withAlpha(accent, panelA));
        AdminHeadTextures.drawHead(context, e.adminKey, x + 8, y + 8, 24);

        String title =
            (AdminHeadTextures.displayName(e.adminKey)
                    + (e.online ? " Is Now Online" : " Is Now Offline"))
                .toUpperCase();
        String status = e.online ? "Joined tab" : "Left tab";

        drawText(context, title, x + 36, y + 9, withAlpha(accent, panelA), 0.95f);
        drawText(context, status, x + 36, y + 22, withAlpha(SUBTLE, Math.min(panelA, 220)), 0.85f);

        int barLeft = x + 6;
        int barRight = x + WIDTH - 6;
        int fill = Math.max(0, (int) ((barRight - barLeft) * (1.0f - life)));
        context.fill(barLeft, y + HEIGHT - 3, barRight, y + HEIGHT - 1, withAlpha(BAR_BG, panelA));
        if (fill > 0) {
          context.fill(
              barLeft, y + HEIGHT - 3, barLeft + fill, y + HEIGHT - 1, withAlpha(accent, panelA));
        }

        y += HEIGHT + GAP;
      }
    } catch (Throwable t) {
      System.err.println("[AdminToastStack] render failed: " + t);
    }
  }

  private static void drawText(
      GuiGraphics context, String text, int x, int y, int argb, float scale) {
    Minecraft mc = MeteorClient.mc;
    if (mc == null || mc.font == null) return;
    context.pose().pushMatrix();
    context.pose().translate(x, y);
    context.pose().scale(scale, scale);
    context.drawString(mc.font, Component.literal(text), 0, 0, argb, true);
    context.pose().popMatrix();
  }

  private static int withAlpha(int color, int alpha) {
    int srcA = (color >>> 24) & 0xFF;
    int a = Math.min(alpha, srcA == 0 ? 255 : srcA);
    return (a << 24) | (color & 0xFFFFFF);
  }

  private static float easeOut(float t) {
    float inv = 1.0f - t;
    return 1.0f - inv * inv * inv;
  }

  private static final class Entry {
    private final String adminKey;
    private final boolean online;
    private final boolean playSound;
    private final long start = System.currentTimeMillis();
    private boolean playedSound;

    private Entry(String adminKey, boolean online, boolean playSound) {
      this.adminKey = adminKey;
      this.online = online;
      this.playSound = playSound;
    }
  }
}
