package dev.dexter.kryptionians.ui;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.Toast.Visibility;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SpawnerNotifierToast implements Toast {
  private static final Identifier TEXTURE = Identifier.parse("toast/advancement");
  private static final ItemStack ICON = Items.SPAWNER.getDefaultInstance();
  private static final int WIDTH = 160;
  private static final int HEIGHT = 32;
  private static final int STRIPE_W = 2;
  private static final int PAD = 3;
  private static final int TEXT_X = 30;
  private static final int BAR_Y = 30;
  private static final int BAR_H = 1;
  private static final int BG = -535423466;
  private static final int TITLE = -1;
  private static final int SUB = -4675352;
  private static final int BAR_BG = -14540254;
  private final String mobType;
  private final String subtitle;
  private final String coords;
  private final SoundInstance sound;
  private final long duration;
  private final int accentColor;
  private long start = -1L;
  private boolean playedSound;
  private Visibility visibility = Visibility.HIDE;

  public SpawnerNotifierToast(
      String mobType, int x, int y, int z, long durationMs, boolean playSound, int progressBarRgb) {
    this.duration = Math.max(2500L, durationMs);
    this.mobType = mobType;
    this.accentColor = 0xFF000000 | progressBarRgb & 16777215;
    int playerY = MeteorClient.mc.player != null ? MeteorClient.mc.player.getBlockY() : y;
    String depth = y < playerY ? "below" : (y > playerY ? "above" : "nearby");
    this.subtitle = "Spawner detected " + depth;
    this.coords = "X " + x + "  Y " + y + "  Z " + z;
    this.sound =
        playSound
            ? SimpleSoundInstance.forUI(
                (SoundEvent) SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F, 1.0F)
            : null;
  }

  public int width() {
    return 160;
  }

  public int height() {
    return 32;
  }

  public Visibility getWantedVisibility() {
    return this.visibility;
  }

  public void update(ToastManager manager, long time) {
    if (this.start == -1L) {
      this.start = time;
    }

    this.visibility = time - this.start >= this.duration ? Visibility.HIDE : Visibility.SHOW;
    if (!this.playedSound && this.sound != null && MeteorClient.mc.getSoundManager() != null) {
      MeteorClient.mc.getSoundManager().play(this.sound);
      this.playedSound = true;
    }
  }

  public void render(GuiGraphics context, Font textRenderer, long time) {
    if (this.start >= 0L) {
      long elapsed = time - this.start;
      float left = 1.0F - Mth.clamp((float) elapsed / (float) this.duration, 0.0F, 1.0F);
      context.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 160, 32);
      context.fill(0, 0, 160, 32, -535423466);
      context.fill(0, 0, 2, 32, this.accentColor);
      context.renderFakeItem(ICON, 5, 8);
      int textMax = 127;
      String title = textRenderer.plainSubstrByWidth(this.mobType + " Spawner Found!", textMax);
      String sub = this.buildSubtitle(textRenderer, textMax);
      context.drawString(textRenderer, title, 30, 7, -1, true);
      context.drawString(textRenderer, sub, 30, 18, -4675352, false);
      int barLeft = 5;
      int barRight = 157;
      int barWidth = barRight - barLeft;
      int fill = Math.max(0, (int) ((float) barWidth * left));
      context.fill(barLeft, 30, barRight, 31, -14540254);
      if (fill > 0) {
        context.fill(barLeft, 30, barLeft + fill, 31, this.accentColor);
      }
    }
  }

  private String buildSubtitle(Font textRenderer, int maxWidth) {
    int prefixWidth = textRenderer.width(this.subtitle + "  ");
    if (prefixWidth >= maxWidth) {
      return this.subtitle;
    } else {
      int coordsWidth = maxWidth - prefixWidth;
      return coordsWidth <= 8
          ? this.subtitle
          : this.subtitle + "  " + textRenderer.plainSubstrByWidth(this.coords, coordsWidth);
    }
  }
}
