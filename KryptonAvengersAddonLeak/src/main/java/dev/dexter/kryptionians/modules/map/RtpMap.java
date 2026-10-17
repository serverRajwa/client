package dev.dexter.kryptionians.modules.map;

import dev.dexter.kryptionians.KryptioniansAddon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;

public class RtpMap extends Module {
  private static final int MAP_SIZE = 9;
  private static final double CELL_WORLD_SIZE = 50_000.0;
  private static final double MAP_OFFSET = 225_000.0;
  private static final double SPOT_HALF = 25_000.0;

  private final SettingGroup displaySettings;
  private final SettingGroup positionSettings;
  private final SettingGroup visualSettings;
  private final SettingGroup themeSettings;

  private final Setting<Integer> mapPosX;
  private final Setting<Integer> mapPosY;
  private final Setting<Integer> cellDimension;
  private final Setting<Boolean> enableCoordinates;
  private final Setting<Boolean> enableLegend;
  private final Setting<Boolean> enableRegionNumbers;
  private final Setting<Boolean> enableMapGrid;
  private final Setting<Boolean> enablePlayerIndicator;
  private final Setting<Boolean> enableDistanceRings;
  private final Setting<Boolean> enableSpotZones;
  private final Setting<Double> mapTransparency;
  private final Setting<Double> spotOpacity;
  private final Setting<Double> labelTextSize;
  private final Setting<SettingColor> mapBackgroundColor;
  private final Setting<SettingColor> playerIndicatorColor;
  private final Setting<SettingColor> gridLineColor;
  private final Setting<SettingColor> numberColor;

  private final RegionDataManager regionData;
  private final SpotDataManager spotData;
  private final MapRenderer mapRenderer;
  private final PlayerTracker playerTracker;

  public RtpMap() {
    super(
        KryptioniansAddon.CATEGORY,
        "map",
        "Blank 50k RTP map — glitched/media spots + region numbers (no region colors).");

    this.displaySettings = this.settings.createGroup("Display");
    this.positionSettings = this.settings.createGroup("Position");
    this.visualSettings = this.settings.createGroup("Visual");
    this.themeSettings = this.settings.createGroup("Theme");

    this.mapPosX =
        this.positionSettings.add(
            new IntSetting.Builder()
                .name("position-x")
                .description("Horizontal position of the map.")
                .defaultValue(15)
                .min(0)
                .sliderMax(1920)
                .build());
    this.mapPosY =
        this.positionSettings.add(
            new IntSetting.Builder()
                .name("position-y")
                .description("Vertical position of the map.")
                .defaultValue(15)
                .min(0)
                .sliderMax(1080)
                .build());
    this.cellDimension =
        this.positionSettings.add(
            new IntSetting.Builder()
                .name("cell-size")
                .description("Pixel size of each 50k×50k cell.")
                .defaultValue(26)
                .range(14, 56)
                .sliderRange(14, 56)
                .build());

    this.enableCoordinates =
        this.displaySettings.add(
            new BoolSetting.Builder()
                .name("enable-coordinates")
                .description("Show Position X/Z under the map.")
                .defaultValue(true)
                .build());
    this.enableLegend =
        this.displaySettings.add(
            new BoolSetting.Builder()
                .name("enable-legend")
                .description("Show glitched / media legend.")
                .defaultValue(true)
                .build());
    this.enableRegionNumbers =
        this.displaySettings.add(
            new BoolSetting.Builder()
                .name("enable-region-numbers")
                .description("Draw region IDs inside cells.")
                .defaultValue(true)
                .build());
    this.enableMapGrid =
        this.displaySettings.add(
            new BoolSetting.Builder()
                .name("enable-grid")
                .description("Draw 50k grid lines.")
                .defaultValue(true)
                .build());
    this.enablePlayerIndicator =
        this.displaySettings.add(
            new BoolSetting.Builder()
                .name("enable-player-indicator")
                .description("Show player position + yaw.")
                .defaultValue(true)
                .build());
    this.enableDistanceRings =
        this.displaySettings.add(
            new BoolSetting.Builder()
                .name("enable-distance-rings")
                .description("Subtle spawn rings.")
                .defaultValue(false)
                .build());
    this.enableSpotZones =
        this.displaySettings.add(
            new BoolSetting.Builder()
                .name("enable-spot-zones")
                .description("Overlay glitched / media boxes.")
                .defaultValue(true)
                .build());

    this.mapTransparency =
        this.visualSettings.add(
            new DoubleSetting.Builder()
                .name("panel-transparency")
                .description("Ink panel alpha.")
                .defaultValue(0.68)
                .range(0.15, 1.0)
                .sliderRange(0.15, 1.0)
                .build());
    this.spotOpacity =
        this.visualSettings.add(
            new DoubleSetting.Builder()
                .name("spot-opacity")
                .description("Spot overlay alpha.")
                .defaultValue(0.86)
                .range(0.2, 1.0)
                .sliderRange(0.2, 1.0)
                .build());
    this.labelTextSize =
        this.visualSettings.add(
            new DoubleSetting.Builder()
                .name("number-size")
                .description("Region number scale.")
                .defaultValue(0.78)
                .range(0.4, 1.6)
                .sliderRange(0.4, 1.6)
                .build());

    this.mapBackgroundColor =
        this.themeSettings.add(
            new ColorSetting.Builder()
                .name("background-color")
                .description("Blank map panel (cool ink).")
                .defaultValue(new SettingColor(10, 12, 18, 165))
                .build());
    this.playerIndicatorColor =
        this.themeSettings.add(
            new ColorSetting.Builder()
                .name("player-color")
                .description("Player arrow color.")
                .defaultValue(new SettingColor(255, 64, 72, 255))
                .build());
    this.gridLineColor =
        this.themeSettings.add(
            new ColorSetting.Builder()
                .name("grid-color")
                .description("Grid line color.")
                .defaultValue(new SettingColor(58, 66, 82, 210))
                .build());
    this.numberColor =
        this.themeSettings.add(
            new ColorSetting.Builder()
                .name("number-color")
                .description("Region number color.")
                .defaultValue(new SettingColor(236, 240, 248, 235))
                .build());

    this.regionData = new RegionDataManager();
    this.spotData = new SpotDataManager();
    this.mapRenderer = new MapRenderer();
    this.playerTracker = new PlayerTracker();
  }

  @EventHandler
  private void onRender2D(Render2DEvent event) {
    if (!this.isValidRenderState()) return;

    try {
      Vec3 playerPos =
          new Vec3(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());

      MapRenderContext ctx =
          new MapRenderContext(
              this.mapPosX.get(),
              this.mapPosY.get(),
              this.cellDimension.get(),
              this.mapTransparency.get());

      this.mapRenderer.renderBlankPanel(ctx);
      if (this.enableDistanceRings.get()) {
        this.mapRenderer.renderDistanceRings(ctx);
      }
      if (this.enableSpotZones.get()) {
        this.mapRenderer.renderSpotCells(ctx, this.spotData, this.spotOpacity.get());
      }
      if (this.enableMapGrid.get()) {
        this.mapRenderer.renderGridLines(ctx, this.gridLineColor.get());
      }
      if (this.enableRegionNumbers.get()) {
        this.mapRenderer.renderRegionNumbers(
            ctx, this.regionData, this.labelTextSize.get(), this.numberColor.get());
      }
      if (this.enablePlayerIndicator.get()) {
        this.playerTracker.renderPlayerPosition(
            ctx, playerPos, this.mc.player.getYRot(), this.playerIndicatorColor.get());
      }
      if (this.enableCoordinates.get()) {
        this.renderPlayerInfo(ctx, playerPos);
      }
      if (this.enableLegend.get()) {
        this.renderSpotLegend(ctx);
      }
    } catch (Exception ignored) {
    }
  }

  private boolean isValidRenderState() {
    return this.mc != null && this.mc.player != null && this.mc.level != null;
  }

  private void renderPlayerInfo(MapRenderContext ctx, Vec3 pos) {
    TextRenderer textRenderer = TextRenderer.get();
    if (textRenderer == null) return;

    int infoY = ctx.mapY + ctx.getMapHeight() + 8;
    textRenderer.begin(1.0, false, true);
    textRenderer.render(
        String.format("Position: X: %d, Z: %d", (int) pos.x(), (int) pos.z()),
        ctx.mapX,
        infoY,
        Color.WHITE,
        false);
    textRenderer.end();
  }

  private void renderSpotLegend(MapRenderContext ctx) {
    int y = ctx.mapY + ctx.getMapHeight() + (this.enableCoordinates.get() ? 26 : 10);
    String[] labels = this.spotData.getLegendLabels();
    Color[] colors = this.spotData.getLegendColors();

    Renderer2D.COLOR.begin();
    for (int i = 0; i < labels.length; i++) {

      Renderer2D.COLOR.quad(ctx.mapX, y + i * 16, 14.0, 12.0, colors[i]);
    }
    Renderer2D.COLOR.render();

    TextRenderer textRenderer = TextRenderer.get();
    if (textRenderer != null) {
      textRenderer.begin(1.0, false, true);
      for (int i = 0; i < labels.length; i++) {
        textRenderer.render(
            labels[i], ctx.mapX + 18, y + i * 16 + 1, new Color(220, 226, 238, 240), false);
      }
      textRenderer.end();
    }
  }

  private static final class RegionDataManager {
    private final Map<Integer, RegionInfo> regionMap = new HashMap<>();

    RegionDataManager() {

      int[] layout = {
        82, 100, 101, 102, 103, 104, 105, 106, 91,
        83, 44, 75, 42, 41, 40, 39, 38, 92,
        84, 45, 14, 13, 12, 11, 10, 37, 93,
        85, 46, 74, 3, 2, 1, 25, 36, 94,
        86, 47, 72, 71, 5, 4, 24, 35, 95,
        87, 51, 17, 9, 8, 7, 23, 34, 96,
        88, 54, 18, 61, 62, 21, 22, 33, 97,
        89, 26, 27, 28, 29, 30, 59, 32, 98,
        90, 107, 108, 109, 110, 111, 112, 113, 99
      };
      for (int i = 0; i < layout.length; i++) {
        int row = i / MAP_SIZE;
        int col = i % MAP_SIZE;
        regionMap.put(i, new RegionInfo(layout[i], row, col));
      }
    }

    RegionInfo get(int index) {
      return regionMap.get(index);
    }

    int[] worldToGrid(double worldX, double worldZ) {
      return new int[] {
        (int) Math.floor((worldX + MAP_OFFSET) / CELL_WORLD_SIZE),
        (int) Math.floor((worldZ + MAP_OFFSET) / CELL_WORLD_SIZE)
      };
    }

    double[] worldToCellFrac(double worldX, double worldZ) {
      double fx = (worldX + MAP_OFFSET) / CELL_WORLD_SIZE;
      double fz = (worldZ + MAP_OFFSET) / CELL_WORLD_SIZE;
      return new double[] {fx - Math.floor(fx), fz - Math.floor(fz)};
    }

    boolean inBounds(int gx, int gz) {
      return gx >= 0 && gx < MAP_SIZE && gz >= 0 && gz < MAP_SIZE;
    }
  }

  private static final class RegionInfo {
    final int id;
    final int row;
    final int col;

    RegionInfo(int id, int row, int col) {
      this.id = id;
      this.row = row;
      this.col = col;
    }
  }

  private static final class SpotDataManager {
    private static final int PRI_GLITCHED = 3;
    private static final int PRI_BALTOP = 2;
    private static final int PRI_BASES = 1;

    private final List<SpotCell> cells = new ArrayList<>();
    private final String[] legendLabels = {
      "Glitched (no rtp spots)", "Good for media / baltop", "Good for media / other bases"
    };

    private final Color[] legendColors = {
      new Color(186, 110, 255, 235), new Color(64, 140, 255, 235), new Color(176, 188, 208, 235)
    };

    private final Color glitched = new Color(186, 110, 255, 205);
    private final Color baltop = new Color(64, 140, 255, 215);
    private final Color bases = new Color(176, 188, 208, 195);

    SpotDataManager() {

      addCell(glitched, "Glitched (no rtp spots)", PRI_GLITCHED, -200_000, 200_000);
      addCell(glitched, "Glitched (no rtp spots)", PRI_GLITCHED, -200_000, 150_000);
      addCell(glitched, "Glitched (no rtp spots)", PRI_GLITCHED, -100_000, -200_000);
      addCell(glitched, "Glitched (no rtp spots)", PRI_GLITCHED, 0, -200_000);
      addCell(glitched, "Glitched (no rtp spots)", PRI_GLITCHED, 50_000, -200_000);
      addCell(glitched, "Glitched (no rtp spots)", PRI_GLITCHED, 200_000, 100_000);

      addCell(baltop, "Good for media / baltop", PRI_BALTOP, 0, 0);
      addCell(baltop, "Good for media / baltop", PRI_BALTOP, 50_000, 0);
      addCell(baltop, "Good for media / baltop", PRI_BALTOP, 0, 50_000);
      addCell(baltop, "Good for media / baltop", PRI_BALTOP, 50_000, 150_000);

      addCell(bases, "Good for media / other bases", PRI_BASES, -200_000, 0);
      addCell(bases, "Good for media / other bases", PRI_BASES, -200_000, 100_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 0, -50_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 50_000, 50_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 150_000, 50_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, -50_000, 150_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 0, 150_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 100_000, 150_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 150_000, 150_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 200_000, 150_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, -50_000, 200_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 100_000, 200_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 150_000, 200_000);
      addCell(bases, "Good for media / other bases", PRI_BASES, 200_000, 200_000);
    }

    private void addCell(Color color, String label, int priority, double worldX, double worldZ) {
      int col = (int) Math.floor((worldX + MAP_OFFSET) / CELL_WORLD_SIZE);
      int row = (int) Math.floor((worldZ + MAP_OFFSET) / CELL_WORLD_SIZE);
      if (col < 0 || col >= MAP_SIZE || row < 0 || row >= MAP_SIZE) return;

      for (SpotCell existing : cells) {
        if (existing.col == col && existing.row == row) {
          if (priority >= existing.priority) {
            existing.color = color;
            existing.label = label;
            existing.priority = priority;
          }
          return;
        }
      }
      cells.add(new SpotCell(color, label, priority, col, row));
    }

    List<SpotCell> getCells() {
      return cells;
    }

    String[] getLegendLabels() {
      return legendLabels;
    }

    Color[] getLegendColors() {
      return legendColors;
    }
  }

  private static final class SpotCell {
    Color color;
    String label;
    int priority;
    final int col;
    final int row;

    SpotCell(Color color, String label, int priority, int col, int row) {
      this.color = color;
      this.label = label;
      this.priority = priority;
      this.col = col;
      this.row = row;
    }
  }

  private class MapRenderer {
    void renderBlankPanel(MapRenderContext ctx) {
      Color bg = new Color(mapBackgroundColor.get());
      bg.a = (int) (ctx.transparency * bg.a);
      Renderer2D.COLOR.begin();
      Renderer2D.COLOR.quad(ctx.mapX, ctx.mapY, ctx.getMapWidth(), ctx.getMapHeight(), bg);
      Renderer2D.COLOR.render();
    }

    void renderDistanceRings(MapRenderContext ctx) {
      Renderer2D.COLOR.begin();
      Color color = new Color(70, 70, 75, 55);
      double cx = ctx.worldToMapX(0.0);
      double cy = ctx.worldToMapZ(0.0);
      for (int i = 1; i <= 9; i++) {
        double radius = ctx.worldSizeToMapSize(i * 25_000.0);
        drawRingClipped(ctx, cx, cy, radius, color);
      }
      Renderer2D.COLOR.render();
    }

    private void drawRingClipped(
        MapRenderContext ctx, double cx, double cy, double radius, Color color) {
      int segments = 64;
      double clipL = ctx.mapX;
      double clipT = ctx.mapY;
      double clipR = ctx.mapX + ctx.getMapWidth();
      double clipB = ctx.mapY + ctx.getMapHeight();
      for (int i = 0; i < segments; i++) {
        double a1 = (Math.PI * 2.0 * i) / segments;
        double a2 = (Math.PI * 2.0 * (i + 1)) / segments;
        double x1 = clamp(cx + Math.cos(a1) * radius, clipL, clipR);
        double y1 = clamp(cy + Math.sin(a1) * radius, clipT, clipB);
        double x2 = clamp(cx + Math.cos(a2) * radius, clipL, clipR);
        double y2 = clamp(cy + Math.sin(a2) * radius, clipT, clipB);
        Renderer2D.COLOR.quad(x1, y1, Math.max(1.0, Math.abs(x2 - x1)), 1.0, color);
        Renderer2D.COLOR.quad(x1, y1, 1.0, Math.max(1.0, Math.abs(y2 - y1)), color);
      }
    }

    void renderSpotCells(MapRenderContext ctx, SpotDataManager data, double opacity) {
      Renderer2D.COLOR.begin();
      for (SpotCell cell : data.getCells()) {
        Color c = new Color(cell.color);
        c.a = (int) (opacity * c.a);
        double x = ctx.mapX + cell.col * ctx.cellSize;
        double y = ctx.mapY + cell.row * ctx.cellSize;

        Renderer2D.COLOR.quad(x + 1.0, y + 1.0, ctx.cellSize - 2.0, ctx.cellSize - 2.0, c);
      }
      Renderer2D.COLOR.render();
    }

    void renderGridLines(MapRenderContext ctx, SettingColor gridColor) {
      Color line = new Color(gridColor);
      Renderer2D.COLOR.begin();
      for (int i = 0; i <= MAP_SIZE; i++) {
        double x = ctx.mapX + i * ctx.cellSize;
        Renderer2D.COLOR.quad(x, ctx.mapY, 1.0, ctx.getMapHeight(), line);
      }
      for (int i = 0; i <= MAP_SIZE; i++) {
        double y = ctx.mapY + i * ctx.cellSize;
        Renderer2D.COLOR.quad(ctx.mapX, y, ctx.getMapWidth(), 1.0, line);
      }
      Renderer2D.COLOR.render();
    }

    void renderRegionNumbers(
        MapRenderContext ctx, RegionDataManager data, double scale, SettingColor colorSetting) {
      TextRenderer textRenderer = TextRenderer.get();
      if (textRenderer == null) return;
      Color color = new Color(colorSetting);
      textRenderer.begin(scale, false, true);
      for (int row = 0; row < MAP_SIZE; row++) {
        for (int col = 0; col < MAP_SIZE; col++) {
          RegionInfo info = data.get(row * MAP_SIZE + col);
          if (info == null) continue;
          String text = String.valueOf(info.id);
          double tw = textRenderer.getWidth(text) * scale;
          double th = textRenderer.getHeight() * scale;
          double x = ctx.mapX + col * ctx.cellSize + (ctx.cellSize - tw) / 2.0;
          double y = ctx.mapY + row * ctx.cellSize + (ctx.cellSize - th) / 2.0;
          textRenderer.render(text, x, y, color, false);
        }
      }
      textRenderer.end();
    }
  }

  private class PlayerTracker {
    void renderPlayerPosition(
        MapRenderContext ctx, Vec3 playerPos, float yaw, SettingColor indicatorColor) {
      int[] grid = regionData.worldToGrid(playerPos.x(), playerPos.z());
      if (!regionData.inBounds(grid[0], grid[1])) return;

      double[] frac = regionData.worldToCellFrac(playerPos.x(), playerPos.z());
      int px = ctx.mapX + (int) Math.round(grid[0] * ctx.cellSize + frac[0] * ctx.cellSize);
      int py = ctx.mapY + (int) Math.round(grid[1] * ctx.cellSize + frac[1] * ctx.cellSize);
      double rotationAngle = Math.toRadians(-yaw - 90.0);
      renderDirectionalIndicator(px, py, rotationAngle, indicatorColor);
    }

    private void renderDirectionalIndicator(
        int centerX, int centerY, double angle, SettingColor color) {
      Renderer2D.COLOR.begin();
      Color indicatorCol = new Color(color);
      int arrowSize = 8;
      int tipX = centerX + (int) (Math.cos(angle) * arrowSize);
      int tipY = centerY - (int) (Math.sin(angle) * arrowSize);
      double leftBaseAngle = angle + Math.toRadians(135.0);
      double rightBaseAngle = angle - Math.toRadians(135.0);
      int leftBaseX = centerX + (int) (Math.cos(leftBaseAngle) * arrowSize);
      int leftBaseY = centerY - (int) (Math.sin(leftBaseAngle) * arrowSize);
      int rightBaseX = centerX + (int) (Math.cos(rightBaseAngle) * arrowSize);
      int rightBaseY = centerY - (int) (Math.sin(rightBaseAngle) * arrowSize);
      drawTriangleFilled(tipX, tipY, leftBaseX, leftBaseY, rightBaseX, rightBaseY, indicatorCol);
      Renderer2D.COLOR.render();
    }

    private void drawTriangleFilled(int x1, int y1, int x2, int y2, int x3, int y3, Color color) {
      int minY = Math.min(y1, Math.min(y2, y3));
      int maxY = Math.max(y1, Math.max(y2, y3));
      for (int scanY = minY; scanY <= maxY; scanY++) {
        int leftX = Integer.MAX_VALUE;
        int rightX = Integer.MIN_VALUE;
        int[] intersections = {
          edgeIntersection(x1, y1, x2, y2, scanY),
          edgeIntersection(x2, y2, x3, y3, scanY),
          edgeIntersection(x3, y3, x1, y1, scanY)
        };
        for (int intersection : intersections) {
          if (intersection == Integer.MAX_VALUE) continue;
          leftX = Math.min(leftX, intersection);
          rightX = Math.max(rightX, intersection);
        }
        if (leftX > rightX || leftX == Integer.MAX_VALUE) continue;
        Renderer2D.COLOR.quad(leftX, scanY, rightX - leftX + 1, 1.0, color);
      }
    }

    private int edgeIntersection(int x1, int y1, int x2, int y2, int scanY) {
      if (scanY < Math.min(y1, y2) || scanY > Math.max(y1, y2)) return Integer.MAX_VALUE;
      if (y1 == y2) return (x1 + x2) / 2;
      return x1 + (x2 - x1) * (scanY - y1) / (y2 - y1);
    }
  }

  private class MapRenderContext {
    final int mapX;
    final int mapY;
    final int cellSize;
    final double transparency;

    MapRenderContext(int x, int y, int cellSize, double transparency) {
      this.mapX = x;
      this.mapY = y;
      this.cellSize = Math.max(1, cellSize);
      this.transparency = Math.max(0.1, Math.min(1.0, transparency));
    }

    int getMapWidth() {
      return MAP_SIZE * cellSize;
    }

    int getMapHeight() {
      return MAP_SIZE * cellSize;
    }

    double worldToMapX(double worldX) {
      return mapX + ((worldX + MAP_OFFSET) / (MAP_SIZE * CELL_WORLD_SIZE)) * getMapWidth();
    }

    double worldToMapZ(double worldZ) {
      return mapY + ((worldZ + MAP_OFFSET) / (MAP_SIZE * CELL_WORLD_SIZE)) * getMapHeight();
    }

    double worldSizeToMapSize(double worldSize) {
      return worldSize / (MAP_SIZE * CELL_WORLD_SIZE) * getMapWidth();
    }
  }

  private static double clamp(double v, double lo, double hi) {
    return Math.max(lo, Math.min(hi, v));
  }
}
