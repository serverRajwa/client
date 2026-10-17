package dev.dexter.kryptionians.ui;

import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class AdminHeadTextures {
  public static final Map<String, String> DISPLAY_NAMES =
      Map.of(
          "archivepedro", "archivePedro",
          "frwost", "Frwost",
          "w1zox_", "W1zoX_",
          "fluffymaster07", "Fluffymaster07",
          "bautiedgar", "bautiedgar",
          "showered", "Showered",
          "pastagamer08", "PastaGamer08",
          "itszdeath", "Itszdeath",
          "0gsummer", "0Gsummer");
  private static final Map<String, Identifier> HEADS =
      Map.of(
          "archivepedro", id("archivepedro"),
          "frwost", id("frwost"),
          "w1zox_", id("w1zox_"),
          "fluffymaster07", id("fluffymaster07"),
          "bautiedgar", id("bautiedgar"),
          "showered", id("showered"),
          "pastagamer08", id("pastagamer08"),
          "itszdeath", id("itszdeath"),
          "0gsummer", id("0gsummer"));

  private AdminHeadTextures() {}

  private static Identifier id(String name) {
    return Identifier.fromNamespaceAndPath("kryptionians", "textures/admin/" + name + ".png");
  }

  public static String toKey(String name) {
    return name == null ? "" : name.toLowerCase(Locale.ROOT);
  }

  public static String displayName(String keyOrName) {
    String key = toKey(keyOrName);
    return DISPLAY_NAMES.getOrDefault(key, keyOrName == null ? "" : keyOrName);
  }

  public static void drawHead(GuiGraphics context, String adminKey, int x, int y, int size) {
    String key = toKey(adminKey);
    Identifier texture = HEADS.get(key);
    float scale = size / 16.0f;

    context.pose().pushMatrix();
    context.pose().translate(x, y);
    context.pose().scale(scale, scale);

    if (texture != null) {
      context.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0f, 0.0f, 16, 16, 16, 16);
    } else {
      context.fill(0, 0, 16, 16, 0xFF2A2A2A);
    }

    context.pose().popMatrix();
  }
}
