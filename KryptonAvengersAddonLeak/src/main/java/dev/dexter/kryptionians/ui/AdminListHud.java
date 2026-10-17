package dev.dexter.kryptionians.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class AdminListHud {
  private static final int WIDTH = 168;
  private static final int HEADER_H = 22;
  private static final int ROW_H = 20;
  private static final int PAD = 8;
  private static final int HEAD = 16;
  private static final int RADIUS = 10;
  private static final long ENTER_MS = 350L;
  private static final long EXIT_MS = 300L;

  private static final int BG = 0xE80A0A0E;
  private static final int TITLE = 0xFFFFFFFF;
  private static final int NAME = 0xFFF2F2F8;
  private static final int EMPTY = 0xFFA0A0AA;

  private final List<Row> rows = new ArrayList<>();
  private boolean dragging;
  private double dragOffX;
  private double dragOffY;

  public void sync(
      Set<String> onlineKeys, Set<String> joined, Set<String> left, boolean initialSeed) {
    long now = System.currentTimeMillis();
    if (initialSeed) {
      rows.clear();
      for (String key : onlineKeys) rows.add(new Row(key, false));
      rows.sort(
          Comparator.comparing(r -> AdminHeadTextures.displayName(r.key).toLowerCase(Locale.ROOT)));
      return;
    }

    for (String key : joined) {
      Row row = findRow(key);
      if (row == null) rows.add(new Row(key, true));
      else {
        row.leaving = false;
        row.entering = true;
        row.animStart = now;
      }
    }
    for (String key : left) {
      Row row = findRow(key);
      if (row == null) continue;
      row.leaving = true;
      row.entering = false;
      row.animStart = now;
    }
    for (Row row : rows) {
      if (onlineKeys.contains(row.key) && !left.contains(row.key)) {
        row.leaving = false;
        continue;
      }
      if (onlineKeys.contains(row.key) || row.leaving) continue;
      row.leaving = true;
      row.entering = false;
      row.animStart = now;
    }
    rows.sort(
        Comparator.comparing(r -> AdminHeadTextures.displayName(r.key).toLowerCase(Locale.ROOT)));
    pruneRows(now);
  }

  public void clear() {
    rows.clear();
    dragging = false;
  }

  public void handleDrag(Setting<Integer> hudX, Setting<Integer> hudY, Setting<Integer> listSize) {
    if (MeteorClient.mc.getWindow() == null || !(MeteorClient.mc.screen instanceof ChatScreen)) {
      dragging = false;
      return;
    }

    float scale = listScale(listSize);
    int screenW = MeteorClient.mc.getWindow().getGuiScaledWidth();
    int screenH = MeteorClient.mc.getWindow().getGuiScaledHeight();
    int panelW = scaled(WIDTH, scale);
    int panelH = scaled(panelHeight(), scale);
    int headerH = scaled(HEADER_H, scale);
    int x = resolveX(hudX, screenW, scale);
    int y = resolveY(hudY, screenH, scale);

    double windowScale = MeteorClient.mc.getWindow().getGuiScale();
    double mouseX = MeteorClient.mc.mouseHandler.xpos() / windowScale;
    double mouseY = MeteorClient.mc.mouseHandler.ypos() / windowScale;
    boolean mouseDown =
        GLFW.glfwGetMouseButton(MeteorClient.mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_1)
            == GLFW.GLFW_PRESS;
    boolean overHeader = mouseX >= x && mouseX < x + panelW && mouseY >= y && mouseY < y + headerH;

    if (mouseDown && overHeader && !dragging) {
      dragging = true;
      dragOffX = mouseX - x;
      dragOffY = mouseY - y;
    }
    if (!mouseDown) {
      dragging = false;
      return;
    }
    if (!dragging) return;

    int newX = (int) Math.round(Math.max(0, Math.min(mouseX - dragOffX, screenW - panelW)));
    int newY = (int) Math.round(Math.max(0, Math.min(mouseY - dragOffY, screenH - panelH)));
    if (hudX.get() != newX) hudX.set(newX);
    if (hudY.get() != newY) hudY.set(newY);
  }

  public void render(
      GuiGraphics context,
      Setting<Integer> hudX,
      Setting<Integer> hudY,
      Setting<Integer> listSize) {
    Minecraft mc = MeteorClient.mc;
    if (mc == null || mc.getWindow() == null || mc.font == null) return;

    try {
      float scale = listScale(listSize);
      int screenW = mc.getWindow().getGuiScaledWidth();
      int screenH = mc.getWindow().getGuiScaledHeight();
      int x = resolveX(hudX, screenW, scale);
      int y = resolveY(hudY, screenH, scale);
      int w = scaled(WIDTH, scale);
      int h = scaled(panelHeight(), scale);
      int pad = scaled(PAD, scale);
      int head = Math.max(8, scaled(HEAD, scale));
      int headerH = scaled(HEADER_H, scale);
      int rowH = scaled(ROW_H, scale);
      int radius = Math.max(3, scaled(RADIUS, scale));

      SmoothRoundRect.draw(context, x + 2, y + 2, w, h, radius, 0x55000000);
      SmoothRoundRect.draw(context, x, y, w, h, radius, BG);

      long now = System.currentTimeMillis();
      pruneRows(now);

      drawText(context, "Online Admin List", x + pad, y + scaled(7, scale), TITLE, scale);

      int rowY = y + headerH;
      boolean any = false;
      for (Row row : rows) {
        if (!shouldDrawRow(row, now)) continue;
        float progress = rowProgress(row, now);
        if (progress <= 0.01f && row.leaving) continue;
        any = true;

        int alpha = Math.max(0, Math.min(255, (int) (progress * 255f)));
        int slideX = row.leaving ? 0 : (int) ((1f - easeOut(progress)) * 8f * scale);
        int slideY = row.leaving ? (int) ((1f - progress) * -6f * scale) : 0;
        int rx = x + pad + slideX;
        int ry = rowY + slideY;

        AdminHeadTextures.drawHead(context, row.key, rx, ry + scaled(2, scale), head);
        drawText(
            context,
            AdminHeadTextures.displayName(row.key),
            rx + head + scaled(6, scale),
            ry + scaled(5, scale),
            withAlpha(NAME, alpha),
            scale);
        rowY += (int) (rowH * progress);
      }

      if (!any) {
        drawText(context, "No admins online", x + pad, y + scaled(28, scale), EMPTY, scale);
      }
    } catch (Throwable t) {
      System.err.println("[AdminListHud] render failed: " + t);
    }
  }

  private static void drawText(
      GuiGraphics context, String text, int x, int y, int argb, float hudScale) {
    Minecraft mc = MeteorClient.mc;
    if (mc == null || mc.font == null || text == null || text.isEmpty()) return;

    float fontScale = 1.05f * hudScale;
    context.pose().pushMatrix();
    context.pose().translate(x, y);
    context.pose().scale(fontScale, fontScale);
    context.drawString(mc.font, Component.literal(text), 0, 0, argb, true);
    context.pose().popMatrix();
  }

  private static int withAlpha(int argb, int alpha) {
    return (Math.min(alpha, (argb >>> 24) & 0xFF) << 24) | (argb & 0xFFFFFF);
  }

  private int panelHeight() {
    int visibleRows = 0;
    long now = System.currentTimeMillis();
    for (Row row : rows) {
      if (shouldDrawRow(row, now)) visibleRows++;
    }
    return visibleRows == 0 ? 50 : HEADER_H + visibleRows * ROW_H + PAD;
  }

  private int resolveX(Setting<Integer> hudX, int screenW, float scale) {
    int value = hudX.get();
    int panelW = scaled(WIDTH, scale);
    if (value < 0 || value > screenW - 20) return Math.max(0, screenW - panelW - 10);
    return value;
  }

  private int resolveY(Setting<Integer> hudY, int screenH, float scale) {
    int value = hudY.get();
    int panelH = scaled(panelHeight(), scale);
    if (value < 0 || value > screenH - 20) return 10;
    return Math.min(value, Math.max(0, screenH - panelH));
  }

  private static float listScale(Setting<Integer> listSize) {
    return listSize.get() / 100.0f;
  }

  private static int scaled(int value, float scale) {
    return Math.max(1, Math.round(value * scale));
  }

  private Row findRow(String key) {
    for (Row row : rows) {
      if (row.key.equals(key)) return row;
    }
    return null;
  }

  private void pruneRows(long now) {
    Iterator<Row> it = rows.iterator();
    while (it.hasNext()) {
      Row row = it.next();
      if (row.leaving && rowProgress(row, now) <= 0.01f) it.remove();
    }
  }

  private boolean shouldDrawRow(Row row, long now) {
    return !row.leaving || rowProgress(row, now) > 0.01f;
  }

  private float rowProgress(Row row, long now) {
    long elapsed = now - row.animStart;
    if (row.leaving) return 1.0f - easeOut(Math.min(1.0f, (float) elapsed / EXIT_MS));
    if (row.entering) return easeOut(Math.min(1.0f, (float) elapsed / ENTER_MS));
    return 1.0f;
  }

  private static float easeOut(float t) {
    float inv = 1.0f - t;
    return 1.0f - inv * inv * inv;
  }

  private static final class Row {
    private final String key;
    private boolean entering;
    private boolean leaving;
    private long animStart;

    private Row(String key, boolean animateEnter) {
      this.key = key;
      this.entering = animateEnter;
      this.animStart = System.currentTimeMillis();
    }
  }
}
