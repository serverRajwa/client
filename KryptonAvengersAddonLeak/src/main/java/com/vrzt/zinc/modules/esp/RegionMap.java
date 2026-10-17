package com.vrzt.zinc.modules.esp;

import com.vrzt.zinc.ZincAddon;
import java.util.HashMap;
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

public class RegionMap extends Module {
  private final SettingGroup displaySettings = this.settings.createGroup("Display");
  private final SettingGroup positionSettings = this.settings.createGroup("Position");
  private final SettingGroup visualSettings = this.settings.createGroup("Visual");
  private final SettingGroup themeSettings = this.settings.createGroup("Theme");
  private final Setting<Integer> mapPosX =
      this.positionSettings.add(
          new IntSetting.Builder()
              .name("position-x")
              .description("Horizontal position of the region map.")
              .defaultValue(15)
              .min(0)
              .sliderMax(1920)
              .build());
  private final Setting<Integer> mapPosY =
      this.positionSettings.add(
          new IntSetting.Builder()
              .name("position-y")
              .description("Vertical position of the region map.")
              .defaultValue(15)
              .min(0)
              .sliderMax(1080)
              .build());
  private final Setting<Integer> cellDimension =
      this.positionSettings.add(
          new IntSetting.Builder()
              .name("cell-size")
              .description("Size of each map cell.")
              .defaultValue(22)
              .range(12, 50)
              .sliderRange(12, 50)
              .build());
  private final Setting<Boolean> enableCoordinates =
      this.displaySettings.add(
          new BoolSetting.Builder()
              .name("enable-coordinates")
              .description("Display player coordinates below map.")
              .defaultValue(true)
              .build());
  private final Setting<Boolean> enableRegionLabels =
      this.displaySettings.add(
          new BoolSetting.Builder()
              .name("enable-region-labels")
              .description("Show region type labels with colors.")
              .defaultValue(true)
              .build());
  private final Setting<Boolean> enableMapGrid =
      this.displaySettings.add(
          new BoolSetting.Builder()
              .name("enable-grid")
              .description("Draw grid lines between regions.")
              .defaultValue(true)
              .build());
  private final Setting<Boolean> enablePlayerIndicator =
      this.displaySettings.add(
          new BoolSetting.Builder()
              .name("enable-player-indicator")
              .description("Show player position and direction on map.")
              .defaultValue(true)
              .build());
  private final Setting<Double> mapTransparency =
      this.visualSettings.add(
          new DoubleSetting.Builder()
              .name("transparency")
              .description("Map background transparency level.")
              .defaultValue(0.75)
              .range(0.1, 1.0)
              .sliderRange(0.1, 1.0)
              .build());
  private final Setting<Double> labelTextSize =
      this.visualSettings.add(
          new DoubleSetting.Builder()
              .name("label-size")
              .description("Size of region number labels.")
              .defaultValue(0.9)
              .range(0.4, 2.5)
              .sliderRange(0.4, 2.5)
              .build());
  private final Setting<SettingColor> mapBackgroundColor =
      this.themeSettings.add(
          new ColorSetting.Builder()
              .name("background-color")
              .description("Map background color.")
              .defaultValue(new SettingColor(25, 25, 25, 180))
              .build());
  private final Setting<SettingColor> playerIndicatorColor =
      this.themeSettings.add(
          new ColorSetting.Builder()
              .name("player-color")
              .description("Player direction indicator color.")
              .defaultValue(new SettingColor(255, 50, 50, 255))
              .build());
  private final Setting<SettingColor> gridLineColor =
      this.themeSettings.add(
          new ColorSetting.Builder()
              .name("grid-color")
              .description("Grid line color.")
              .defaultValue(new SettingColor(15, 15, 15, 255))
              .build());
  private final RegionMap.MapDataManager mapData = new RegionMap.MapDataManager();
  private final RegionMap.RegionRenderer regionRenderer = new RegionMap.RegionRenderer();
  private final RegionMap.PlayerTracker playerTracker = new RegionMap.PlayerTracker();

  public RegionMap() {
    super(ZincAddon.esp, "region-map", "DonutSMP region map and shows you your location");
  }

  @EventHandler
  private void onRender2D(Render2DEvent event) {
    if (this.isValidRenderState()) {
      try {
        Vec3 playerPos =
            new Vec3(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
        RegionMap.MapRenderContext ctx =
            new RegionMap.MapRenderContext(
                this.mapPosX.get(),
                this.mapPosY.get(),
                this.cellDimension.get(),
                this.mapTransparency.get());
        this.regionRenderer.renderMapBackground(ctx);
        this.regionRenderer.renderRegionCells(ctx, this.mapData);
        if (this.enableMapGrid.get()) {
          this.regionRenderer.renderGridLines(ctx, (SettingColor) this.gridLineColor.get());
        }

        this.regionRenderer.renderRegionNumbers(ctx, this.mapData, this.labelTextSize.get());
        if (this.enablePlayerIndicator.get()) {
          this.playerTracker.renderPlayerPosition(
              ctx,
              playerPos,
              this.mc.player.getYRot(),
              (SettingColor) this.playerIndicatorColor.get());
        }

        if (this.enableCoordinates.get()) {
          this.renderPlayerInfo(ctx, playerPos);
        }

        if (this.enableRegionLabels.get()) {
          this.renderRegionLegend(ctx);
        }
      } catch (Exception ignored) {
      }
    }
  }

  private boolean isValidRenderState() {
    return this.mc != null && this.mc.player != null && this.mc.level != null;
  }

  private void renderPlayerInfo(RegionMap.MapRenderContext ctx, Vec3 pos) {
    if (pos != null && ctx != null) {
      try {
        int infoY = ctx.mapY + ctx.getMapHeight() + 8;
        TextRenderer textRenderer = TextRenderer.get();
        if (textRenderer == null) {
          return;
        }

        textRenderer.begin(1.0, false, true);
        String coordsText = String.format("Position: X: %d, Z: %d", (int) pos.x, (int) pos.z);
        textRenderer.render(coordsText, (double) ctx.mapX, (double) infoY, Color.WHITE, false);
        int currentRegionId = this.mapData.getRegionAt(pos.x, pos.z);
        if (currentRegionId != -1) {
          String regionInfo =
              String.format(
                  "Current Region: %d (%s)",
                  currentRegionId, this.mapData.getRegionTypeName(pos.x, pos.z));
          textRenderer.render(
              regionInfo, (double) ctx.mapX, (double) (infoY + 15), Color.WHITE, false);
        }

        textRenderer.end();
      } catch (Exception ignored) {
      }
    }
  }

  private void renderRegionLegend(RegionMap.MapRenderContext ctx) {
    if (ctx != null) {
      try {
        int legendStartY = ctx.mapY + ctx.getMapHeight() + (this.enableCoordinates.get() ? 45 : 15);
        String[] regionTypes = this.mapData.getRegionTypeNames();
        Color[] regionTypeColors = this.mapData.getRegionTypeColors();
        if (regionTypes == null || regionTypeColors == null) {
          return;
        }

        Renderer2D.COLOR.begin();

        for (int i = 0; i < regionTypes.length && i < regionTypeColors.length; i++) {
          int legendY = legendStartY + i * 16;
          Renderer2D.COLOR.quad(
              (double) ctx.mapX, (double) legendY, 14.0, 14.0, regionTypeColors[i]);
        }

        Renderer2D.COLOR.render();
        TextRenderer textRenderer = TextRenderer.get();
        if (textRenderer == null) {
          return;
        }

        textRenderer.begin(1.0, false, true);

        for (int i = 0; i < regionTypes.length; i++) {
          int legendY = legendStartY + i * 16 + 3;
          textRenderer.render(
              regionTypes[i], (double) (ctx.mapX + 18), (double) legendY, Color.WHITE, false);
        }

        textRenderer.end();
      } catch (Exception ignored) {
      }
    }
  }

  private static class MapDataManager {
    private static final int MAP_SIZE = 9;
    private static final double REGION_SIZE = 50000.0;
    private static final double MAP_OFFSET = 225000.0;
    private final Map<Integer, RegionMap.RegionInfo> regionMap = new HashMap<>();
    private final String[] regionTypeNames =
        new String[] {"EU Central", "EU West", "NA East", "NA West", "Asia", "Oceania"};
    private final Color[] regionTypeColors =
        new Color[] {
          new Color(159, 206, 99, 255),
          new Color(0, 166, 99, 255),
          new Color(79, 173, 234, 255),
          new Color(47, 110, 186, 255),
          new Color(245, 194, 66, 255),
          new Color(252, 136, 3, 255)
        };

    public MapDataManager() {
      this.initializeRegionData();
    }

    private void initializeRegionData() {
      int[][] regionLayout =
          new int[][] {
            {82, 5},
            {100, 3},
            {101, 3},
            {102, 3},
            {103, 2},
            {104, 2},
            {105, 2},
            {106, 2},
            {91, 2},
            {83, 5},
            {44, 3},
            {75, 3},
            {42, 3},
            {41, 2},
            {40, 2},
            {39, 2},
            {38, 2},
            {92, 2},
            {84, 5},
            {45, 3},
            {14, 3},
            {13, 3},
            {12, 2},
            {11, 2},
            {10, 2},
            {37, 2},
            {93, 2},
            {85, 5},
            {46, 5},
            {74, 5},
            {3, 3},
            {2, 2},
            {1, 2},
            {25, 2},
            {36, 2},
            {94, 2},
            {86, 4},
            {47, 4},
            {72, 4},
            {71, 4},
            {5, 2},
            {4, 2},
            {24, 2},
            {35, 2},
            {95, 2},
            {87, 4},
            {51, 1},
            {17, 1},
            {9, 0},
            {8, 0},
            {7, 0},
            {23, 0},
            {34, 0},
            {96, 2},
            {88, 4},
            {54, 1},
            {18, 1},
            {61, 0},
            {62, 0},
            {21, 0},
            {22, 0},
            {33, 0},
            {97, 0},
            {89, 0},
            {26, 1},
            {27, 0},
            {28, 0},
            {29, 0},
            {30, 0},
            {59, 0},
            {32, 0},
            {98, 0},
            {90, 0},
            {107, 1},
            {108, 1},
            {109, 1},
            {110, 1},
            {111, 1},
            {112, 1},
            {113, 1},
            {99, 0}
          };

      for (int i = 0; i < regionLayout.length; i++) {
        int row = i / 9;
        int col = i % 9;
        if (regionLayout[i].length >= 2) {
          int regionId = regionLayout[i][0];
          int regionType = Math.min(regionLayout[i][1], this.regionTypeNames.length - 1);
          this.regionMap.put(i, new RegionMap.RegionInfo(regionId, regionType, row, col));
        }
      }
    }

    public RegionMap.RegionInfo getRegionInfo(int gridIndex) {
      return this.regionMap.get(gridIndex);
    }

    public int getRegionAt(double worldX, double worldZ) {
      try {
        int[] gridPos = this.worldToGrid(worldX, worldZ);
        if (this.isValidGridPosition(gridPos[0], gridPos[1])) {
          int index = gridPos[1] * 9 + gridPos[0];
          RegionMap.RegionInfo info = this.regionMap.get(index);
          return info != null ? info.regionId : -1;
        }
      } catch (Exception ignored) {
      }

      return -1;
    }

    public String getRegionTypeName(double worldX, double worldZ) {
      try {
        int[] gridPos = this.worldToGrid(worldX, worldZ);
        RegionMap.RegionInfo info;
        if (this.isValidGridPosition(gridPos[0], gridPos[1])
            && (info = this.regionMap.get(gridPos[1] * 9 + gridPos[0])) != null
            && info.regionType >= 0
            && info.regionType < this.regionTypeNames.length) {
          return this.regionTypeNames[info.regionType];
        }
      } catch (Exception ignored) {
      }

      return "Unknown";
    }

    public Color getRegionColor(int regionType) {
      return regionType >= 0 && regionType < this.regionTypeColors.length
          ? this.regionTypeColors[regionType]
          : Color.WHITE;
    }

    public String[] getRegionTypeNames() {
      return (String[]) this.regionTypeNames.clone();
    }

    public Color[] getRegionTypeColors() {
      return (Color[]) this.regionTypeColors.clone();
    }

    public int[] worldToGrid(double worldX, double worldZ) {
      int gridX = (int) ((worldX + 225000.0) / 50000.0);
      int gridZ = (int) ((worldZ + 225000.0) / 50000.0);
      return new int[] {gridX, gridZ};
    }

    public double[] worldToCellPosition(double worldX, double worldZ) {
      double cellX = (worldX + 225000.0) % 50000.0 / 50000.0;
      double cellZ = (worldZ + 225000.0) % 50000.0 / 50000.0;
      cellX = Math.max(0.0, Math.min(1.0, cellX));
      cellZ = Math.max(0.0, Math.min(1.0, cellZ));
      return new double[] {cellX, cellZ};
    }

    private boolean isValidGridPosition(int gridX, int gridZ) {
      return gridX >= 0 && gridX < 9 && gridZ >= 0 && gridZ < 9;
    }

    public int getMapSize() {
      return 9;
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
      return RegionMap.this.mapData.getMapSize() * this.cellSize;
    }

    int getMapHeight() {
      return RegionMap.this.mapData.getMapSize() * this.cellSize;
    }
  }

  private class PlayerTracker {
    void renderPlayerPosition(
        RegionMap.MapRenderContext ctx, Vec3 playerPos, float yaw, SettingColor indicatorColor) {
      if (ctx != null && playerPos != null && indicatorColor != null) {
        try {
          int[] gridPos = RegionMap.this.mapData.worldToGrid(playerPos.x, playerPos.z);
          if (gridPos[0] >= 0
              && gridPos[0] < RegionMap.this.mapData.getMapSize()
              && gridPos[1] >= 0
              && gridPos[1] < RegionMap.this.mapData.getMapSize()) {
            double[] cellPos = RegionMap.this.mapData.worldToCellPosition(playerPos.x, playerPos.z);
            int indicatorX =
                ctx.mapX + gridPos[0] * ctx.cellSize + (int) (cellPos[0] * (double) ctx.cellSize);
            int indicatorY =
                ctx.mapY + gridPos[1] * ctx.cellSize + (int) (cellPos[1] * (double) ctx.cellSize);
            double rotationAngle = Math.toRadians((double) (-yaw) - 90.0);
            this.renderDirectionalIndicator(indicatorX, indicatorY, rotationAngle, indicatorColor);
          }
        } catch (Exception ignored) {
        }
      }
    }

    private void renderDirectionalIndicator(
        int centerX, int centerY, double angle, SettingColor color) {
      try {
        Renderer2D.COLOR.begin();
        Color indicatorCol = new Color(color);
        int arrowSize = 9;
        int tipX = centerX + (int) (Math.cos(angle) * (double) arrowSize);
        int tipY = centerY - (int) (Math.sin(angle) * (double) arrowSize);
        double leftBaseAngle = angle + Math.toRadians(135.0);
        double rightBaseAngle = angle - Math.toRadians(135.0);
        int leftBaseX = centerX + (int) (Math.cos(leftBaseAngle) * (double) arrowSize);
        int leftBaseY = centerY - (int) (Math.sin(leftBaseAngle) * (double) arrowSize);
        int rightBaseX = centerX + (int) (Math.cos(rightBaseAngle) * (double) arrowSize);
        int rightBaseY = centerY - (int) (Math.sin(rightBaseAngle) * (double) arrowSize);
        this.drawTriangleFilled(
            tipX, tipY, leftBaseX, leftBaseY, rightBaseX, rightBaseY, indicatorCol);
        Renderer2D.COLOR.render();
      } catch (Exception ignored) {
      }
    }

    private void drawTriangleFilled(int x1, int y1, int x2, int y2, int x3, int y3, Color color) {
      if (color != null) {
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));

        for (int scanY = minY; scanY <= maxY; scanY++) {
          int leftX = Integer.MAX_VALUE;
          int rightX = Integer.MIN_VALUE;

          for (int intersection :
              new int[] {
                this.getEdgeIntersection(x1, y1, x2, y2, scanY),
                this.getEdgeIntersection(x2, y2, x3, y3, scanY),
                this.getEdgeIntersection(x3, y3, x1, y1, scanY)
              }) {
            if (intersection != Integer.MAX_VALUE) {
              leftX = Math.min(leftX, intersection);
              rightX = Math.max(rightX, intersection);
            }
          }

          if (leftX <= rightX && leftX != Integer.MAX_VALUE) {
            Renderer2D.COLOR.quad(
                (double) leftX, (double) scanY, (double) (rightX - leftX + 1), 1.0, color);
          }
        }
      }
    }

    private int getEdgeIntersection(int x1, int y1, int x2, int y2, int scanY) {
      if (scanY < Math.min(y1, y2) || scanY > Math.max(y1, y2)) {
        return Integer.MAX_VALUE;
      } else {
        return y1 == y2 ? (x1 + x2) / 2 : x1 + (x2 - x1) * (scanY - y1) / (y2 - y1);
      }
    }
  }

  private static class RegionInfo {
    final int regionId;
    final int regionType;
    final int gridRow;
    final int gridCol;

    RegionInfo(int regionId, int regionType, int gridRow, int gridCol) {
      this.regionId = regionId;
      this.regionType = regionType;
      this.gridRow = gridRow;
      this.gridCol = gridCol;
    }
  }

  private class RegionRenderer {
    void renderMapBackground(RegionMap.MapRenderContext ctx) {
      if (ctx != null && RegionMap.this.mapBackgroundColor.get() != null) {
        try {
          Color bgColor = new Color((Color) RegionMap.this.mapBackgroundColor.get());
          bgColor.a = (int) (ctx.transparency * 255.0);
          Renderer2D.COLOR.begin();
          Renderer2D.COLOR.quad(
              (double) ctx.mapX,
              (double) ctx.mapY,
              (double) ctx.getMapWidth(),
              (double) ctx.getMapHeight(),
              bgColor);
          Renderer2D.COLOR.render();
        } catch (Exception ignored) {
        }
      }
    }

    void renderRegionCells(RegionMap.MapRenderContext ctx, RegionMap.MapDataManager dataManager) {
      if (ctx != null && dataManager != null) {
        try {
          Renderer2D.COLOR.begin();
          int mapSize = dataManager.getMapSize();

          for (int row = 0; row < mapSize; row++) {
            for (int col = 0; col < mapSize; col++) {
              int index = row * mapSize + col;
              RegionMap.RegionInfo regionInfo = dataManager.getRegionInfo(index);
              if (regionInfo != null) {
                int cellX = ctx.mapX + col * ctx.cellSize;
                int cellY = ctx.mapY + row * ctx.cellSize;
                Color regionColor = dataManager.getRegionColor(regionInfo.regionType);
                if (regionColor != null) {
                  regionColor.a = (int) (ctx.transparency * 255.0);
                  Renderer2D.COLOR.quad(
                      (double) (cellX + 1),
                      (double) (cellY + 1),
                      (double) (ctx.cellSize - 2),
                      (double) (ctx.cellSize - 2),
                      regionColor);
                }
              }
            }
          }

          Renderer2D.COLOR.render();
        } catch (Exception ignored) {
        }
      }
    }

    void renderGridLines(RegionMap.MapRenderContext ctx, SettingColor gridColor) {
      if (ctx != null && gridColor != null) {
        try {
          Renderer2D.COLOR.begin();
          Color lineColor = new Color(gridColor);
          int mapSize = RegionMap.this.mapData.getMapSize();

          for (int i = 0; i <= mapSize; i++) {
            int lineX = ctx.mapX + i * ctx.cellSize;
            Renderer2D.COLOR.quad(
                (double) lineX, (double) ctx.mapY, 1.0, (double) ctx.getMapHeight(), lineColor);
          }

          for (int i = 0; i <= mapSize; i++) {
            int lineY = ctx.mapY + i * ctx.cellSize;
            Renderer2D.COLOR.quad(
                (double) ctx.mapX, (double) lineY, (double) ctx.getMapWidth(), 1.0, lineColor);
          }

          Renderer2D.COLOR.render();
        } catch (Exception ignored) {
        }
      }
    }

    void renderRegionNumbers(
        RegionMap.MapRenderContext ctx, RegionMap.MapDataManager dataManager, double textScale) {
      if (ctx != null && dataManager != null) {
        try {
          TextRenderer textRenderer = TextRenderer.get();
          if (textRenderer == null) {
            return;
          }

          textRenderer.begin(textScale, false, true);
          int mapSize = dataManager.getMapSize();

          for (int row = 0; row < mapSize; row++) {
            for (int col = 0; col < mapSize; col++) {
              int index = row * mapSize + col;
              RegionMap.RegionInfo regionInfo = dataManager.getRegionInfo(index);
              if (regionInfo != null) {
                int cellX = ctx.mapX + col * ctx.cellSize;
                int cellY = ctx.mapY + row * ctx.cellSize;
                String numberText = String.valueOf(regionInfo.regionId);
                double textWidth = textRenderer.getWidth(numberText) * textScale;
                double textHeight = textRenderer.getHeight() * textScale;
                double centeredX = (double) cellX + ((double) ctx.cellSize - textWidth) / 2.0;
                double centeredY = (double) cellY + ((double) ctx.cellSize - textHeight) / 2.0;
                textRenderer.render(numberText, centeredX, centeredY, Color.WHITE, false);
              }
            }
          }

          textRenderer.end();
        } catch (Exception ignored) {
        }
      }
    }
  }
}
