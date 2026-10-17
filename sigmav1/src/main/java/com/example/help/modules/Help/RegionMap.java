
package com.example.help.modules.Help;

import com.example.help.HelpAddon;
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
import net.minecraft.util.math.Vec3d;
public class RegionMap
extends Module {
    private final SettingGroup displaySettings;
    private final SettingGroup positionSettings;
    private final SettingGroup visualSettings;
    private final SettingGroup themeSettings;
    private final Setting<Integer> mapPosX;
    private final Setting<Integer> mapPosY;
    private final Setting<Integer> cellDimension;
    private final Setting<Boolean> enableCoordinates;
    private final Setting<Boolean> enableRegionLabels;
    private final Setting<Boolean> enableMapGrid;
    private final Setting<Boolean> enablePlayerIndicator;
    private final Setting<Double> mapTransparency;
    private final Setting<Double> labelTextSize;
    private final Setting<SettingColor> mapBackgroundColor;
    private final Setting<SettingColor> mapBorderColor;
    private final Setting<SettingColor> accentColor;
    private final Setting<SettingColor> playerIndicatorColor;
    private final Setting<SettingColor> gridLineColor;
    private final MapDataManager mapData;
    private final RegionRenderer regionRenderer;
    private final PlayerTracker playerTracker;

    public RegionMap() {
        super(HelpAddon.CATEGORY, "region-map", "Region map and shows you your location.");
        this.displaySettings = this.settings.createGroup("Display");
        this.positionSettings = this.settings.createGroup("Position");
        this.visualSettings = this.settings.createGroup("Visual");
        this.themeSettings = this.settings.createGroup("Theme");
        this.mapPosX = this.positionSettings.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("position-x")).description("Horizontal position of the region map.")).defaultValue(15)).min(0).sliderMax(1920).build());
        this.mapPosY = this.positionSettings.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("position-y")).description("Vertical position of the region map.")).defaultValue(15)).min(0).sliderMax(1080).build());
        this.cellDimension = this.positionSettings.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("cell-size")).description("Size of each map cell.")).defaultValue(22)).range(12, 50).sliderRange(12, 50).build());
        this.enableCoordinates = this.displaySettings.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("enable-coordinates")).description("Displays player coordinates below map.")).defaultValue(true)).build());
        this.enableRegionLabels = this.displaySettings.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("enable-region-labels")).description("Shows region type labels with colors.")).defaultValue(true)).build());
        this.enableMapGrid = this.displaySettings.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("enable-grid")).description("Draws grid lines between regions.")).defaultValue(true)).build());
        this.enablePlayerIndicator = this.displaySettings.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("enable-player-indicator")).description("Shows player position and direction on map.")).defaultValue(true)).build());
        this.mapTransparency = this.visualSettings.add((Setting)((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name("transparency")).description("Map background transparency level.")).defaultValue(0.75).range(0.1, 1.0).sliderRange(0.1, 1.0).build());
        this.labelTextSize = this.visualSettings.add((Setting)((DoubleSetting.Builder)((DoubleSetting.Builder)new DoubleSetting.Builder().name("label-size")).description("Size of region number labels.")).defaultValue(0.9).range(0.4, 2.5).sliderRange(0.4, 2.5).build());
        this.mapBackgroundColor = this.themeSettings.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("background-color")).description("Map background color.")).defaultValue(new SettingColor(12, 22, 36, 220)).build());
        this.mapBorderColor = this.themeSettings.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("border-color")).description("Map frame border color.")).defaultValue(new SettingColor(88, 156, 255, 200)).build());
        this.accentColor = this.themeSettings.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("accent-color")).description("Accent color for highlighted map elements.")).defaultValue(new SettingColor(113, 193, 255, 215)).build());
        this.playerIndicatorColor = this.themeSettings.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("player-color")).description("Player direction indicator color.")).defaultValue(new SettingColor(255, 187, 59, 255)).build());
        this.gridLineColor = this.themeSettings.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("grid-color")).description("Grid line color.")).defaultValue(new SettingColor(74, 164, 255, 200)).build());
        this.mapData = new MapDataManager();
        this.regionRenderer = new RegionRenderer();
        this.playerTracker = new PlayerTracker();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!this.isValidRenderState()) {
            return;
        }
        Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
        MapRenderContext ctx = new MapRenderContext((Integer)this.mapPosX.get(), (Integer)this.mapPosY.get(), (Integer)this.cellDimension.get(), (Double)this.mapTransparency.get());
        this.regionRenderer.renderMapBackground(ctx);
        this.regionRenderer.renderRegionCells(ctx, this.mapData);
        if (((Boolean)this.enableMapGrid.get()).booleanValue()) {
            this.regionRenderer.renderGridLines(ctx, (SettingColor)this.gridLineColor.get());
        }
        this.regionRenderer.renderRegionNumbers(ctx, this.mapData, (Double)this.labelTextSize.get());
        if (((Boolean)this.enablePlayerIndicator.get()).booleanValue()) {
            this.playerTracker.renderPlayerPosition(ctx, playerPos, this.mc.player.getYaw(), (SettingColor)this.playerIndicatorColor.get());
        }
        if (((Boolean)this.enableCoordinates.get()).booleanValue()) {
            this.renderPlayerInfo(ctx, playerPos);
        }
        if (((Boolean)this.enableRegionLabels.get()).booleanValue()) {
            this.renderRegionLegend(ctx);
        }
    }

    private boolean isValidRenderState() {
        return this.mc != null && this.mc.player != null && this.mc.world != null;
    }

    private void renderPlayerInfo(MapRenderContext ctx, Vec3d vec) {
        int infoY = ctx.mapY + ctx.getMapHeight() + 8;
        TextRenderer textRenderer = TextRenderer.get();
        textRenderer.begin(1.0, false, true);
        textRenderer.render(String.format("Position: X: %d, Z: %d", (int)vec.x, (int)vec.z), (double)ctx.mapX, (double)infoY, Color.WHITE, false);
        int currentRegionId = this.mapData.getRegionAt(vec.x, vec.z);
        if (currentRegionId != -1) {
            String regionInfo = String.format("Current Region: %d (%s)", currentRegionId, this.mapData.getRegionTypeName(vec.x, vec.z));
            textRenderer.render(regionInfo, (double)ctx.mapX, (double)(infoY + 15), Color.WHITE, false);
        }
        textRenderer.end();
    }

    private void renderRegionLegend(MapRenderContext ctx) {
        int legendStartY = ctx.mapY + ctx.getMapHeight() + ((Boolean)this.enableCoordinates.get() != false ? 45 : 15);
        String[] regionTypes = this.mapData.getRegionTypeNames();
        Color[] regionTypeColors = this.mapData.getRegionTypeColors();
        Renderer2D.COLOR.begin();
        for (int n = 0; n < regionTypes.length && n < regionTypeColors.length; ++n) {
            Renderer2D.COLOR.quad((double)ctx.mapX, (double)(legendStartY + n * 16), 14.0, 14.0, regionTypeColors[n]);
        }
        Renderer2D.COLOR.render();
        TextRenderer textRenderer = TextRenderer.get();
        textRenderer.begin(1.0, false, true);
        for (int n = 0; n < regionTypes.length; ++n) {
            textRenderer.render(regionTypes[n], (double)(ctx.mapX + 18), (double)(legendStartY + n * 16 + 3), Color.WHITE, false);
        }
        textRenderer.end();
    }

    private static class MapDataManager {
        private static final int MAP_SIZE = 9;
        private static final double REGION_SIZE = 50000.0;
        private static final double MAP_OFFSET = 225000.0;
        private final Map<Integer, RegionInfo> regionMap = new HashMap<Integer, RegionInfo>();
        private final String[] regionTypeNames = new String[]{"EU Central", "EU West", "NA East", "NA West", "Asia", "Oceania"};
        private final Color[] regionTypeColors = new Color[]{new Color(159, 206, 99, 255), new Color(0, 166, 99, 255), new Color(79, 173, 234, 255), new Color(47, 110, 186, 255), new Color(245, 194, 66, 255), new Color(252, 136, 3, 255)};

        MapDataManager() {
            this.initializeRegionData();
        }

        private void initializeRegionData() {
            int[][] regionLayout = new int[][]{{82, 5}, {100, 3}, {101, 3}, {102, 3}, {103, 2}, {104, 2}, {105, 2}, {106, 2}, {91, 2}, {83, 5}, {44, 3}, {75, 3}, {42, 3}, {41, 2}, {40, 2}, {39, 2}, {38, 2}, {92, 2}, {84, 5}, {45, 3}, {14, 3}, {13, 3}, {12, 2}, {11, 2}, {10, 2}, {37, 2}, {93, 2}, {85, 5}, {46, 5}, {74, 5}, {3, 3}, {2, 2}, {1, 2}, {25, 2}, {36, 2}, {94, 2}, {86, 4}, {47, 4}, {72, 4}, {71, 4}, {5, 2}, {4, 2}, {24, 2}, {35, 2}, {95, 2}, {87, 4}, {51, 1}, {17, 1}, {9, 0}, {8, 0}, {7, 0}, {23, 0}, {34, 0}, {96, 2}, {88, 4}, {54, 1}, {18, 1}, {61, 0}, {62, 0}, {21, 0}, {22, 0}, {33, 0}, {97, 0}, {89, 0}, {26, 1}, {27, 0}, {28, 0}, {29, 0}, {30, 0}, {59, 0}, {32, 0}, {98, 0}, {90, 0}, {107, 1}, {108, 1}, {109, 1}, {110, 1}, {111, 1}, {112, 1}, {113, 1}, {99, 0}};
            for (int n = 0; n < regionLayout.length; ++n) {
                int row = n / 9;
                int col = n % 9;
                int regionType = Math.min(regionLayout[n][1], this.regionTypeNames.length - 1);
                this.regionMap.put(n, new RegionInfo(regionLayout[n][0], regionType, row, col));
            }
        }

        RegionInfo getRegionInfo(int gridIndex) {
            return this.regionMap.get(gridIndex);
        }

        int getRegionAt(double worldX, double worldZ) {
            int[] gridPos = this.worldToGrid(worldX, worldZ);
            if (!this.isValidGridPosition(gridPos[0], gridPos[1])) {
                return -1;
            }
            RegionInfo info = this.regionMap.get(gridPos[1] * 9 + gridPos[0]);
            return info != null ? info.regionId : -1;
        }

        String getRegionTypeName(double worldX, double worldZ) {
            int[] gridPos = this.worldToGrid(worldX, worldZ);
            if (!this.isValidGridPosition(gridPos[0], gridPos[1])) {
                return "Unknown";
            }
            RegionInfo info = this.regionMap.get(gridPos[1] * 9 + gridPos[0]);
            if (info == null || info.regionType < 0 || info.regionType >= this.regionTypeNames.length) {
                return "Unknown";
            }
            return this.regionTypeNames[info.regionType];
        }

        Color getRegionColor(int regionType) {
            return regionType >= 0 && regionType < this.regionTypeColors.length ? new Color(this.regionTypeColors[regionType]) : Color.WHITE;
        }

        String[] getRegionTypeNames() {
            return (String[])this.regionTypeNames.clone();
        }

        Color[] getRegionTypeColors() {
            Color[] colors = new Color[this.regionTypeColors.length];
            for (int n = 0; n < this.regionTypeColors.length; ++n) {
                colors[n] = new Color(this.regionTypeColors[n]);
            }
            return colors;
        }

        int[] worldToGrid(double worldX, double worldZ) {
            int gridX = (int)((worldX + 225000.0) / 50000.0);
            int gridZ = (int)((worldZ + 225000.0) / 50000.0);
            return new int[]{gridX, gridZ};
        }

        double[] worldToCellPosition(double worldX, double worldZ) {
            double cellX = this.positiveModulo(worldX + 225000.0, 50000.0) / 50000.0;
            double cellZ = this.positiveModulo(worldZ + 225000.0, 50000.0) / 50000.0;
            return new double[]{Math.max(0.0, Math.min(1.0, cellX)), Math.max(0.0, Math.min(1.0, cellZ))};
        }

        private double positiveModulo(double value, double modulo) {
            double result = value % modulo;
            return result < 0.0 ? result + modulo : result;
        }

        private boolean isValidGridPosition(int gridX, int gridZ) {
            return gridX >= 0 && gridX < 9 && gridZ >= 0 && gridZ < 9;
        }

        int getMapSize() {
            return 9;
        }
    }

    private class RegionRenderer {
        private RegionRenderer() {
        }

        void renderMapBackground(MapRenderContext ctx) {
            Color base = new Color((Color)RegionMap.this.mapBackgroundColor.get());
            base.a = (int)(ctx.transparency * 255.0);
            Color frame = new Color((Color)RegionMap.this.mapBorderColor.get());
            Color accent = new Color((Color)RegionMap.this.accentColor.get());
            accent.a = (int)(ctx.transparency * 180.0);
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad((double)(ctx.mapX - 4), (double)(ctx.mapY - 4), (double)(ctx.getMapWidth() + 8), (double)(ctx.getMapHeight() + 8), new Color(frame.r, frame.g, frame.b, Math.min(255, frame.a)));
            Renderer2D.COLOR.quad((double)(ctx.mapX - 2), (double)(ctx.mapY - 2), (double)(ctx.getMapWidth() + 4), (double)(ctx.getMapHeight() + 4), new Color(10, 20, 34, Math.min(255, (int)(ctx.transparency * 240.0))));
            Renderer2D.COLOR.quad((double)ctx.mapX, (double)ctx.mapY, (double)ctx.getMapWidth(), (double)ctx.getMapHeight(), base);
            Renderer2D.COLOR.quad((double)ctx.mapX, (double)(ctx.mapY + ctx.getMapHeight() - 8), (double)ctx.getMapWidth(), 8.0, accent);
            Renderer2D.COLOR.render();
        }

        void renderRegionCells(MapRenderContext ctx, MapDataManager dataManager) {
            Renderer2D.COLOR.begin();
            int mapSize = dataManager.getMapSize();
            for (int row = 0; row < mapSize; ++row) {
                for (int col = 0; col < mapSize; ++col) {
                    RegionInfo regionInfo = dataManager.getRegionInfo(row * mapSize + col);
                    if (regionInfo == null) continue;
                    int cellX = ctx.mapX + col * ctx.cellSize;
                    int cellY = ctx.mapY + row * ctx.cellSize;
                    Color regionColor = dataManager.getRegionColor(regionInfo.regionType);
                    Color shaded = new Color(regionColor.r, regionColor.g, regionColor.b, Math.min(255, (int)(ctx.transparency * 210.0)));
                    Renderer2D.COLOR.quad((double)(cellX + 1), (double)(cellY + 1), (double)(ctx.cellSize - 2), (double)(ctx.cellSize - 2), shaded);
                    Color inner = new Color(regionColor.r, regionColor.g, regionColor.b, Math.min(255, (int)(ctx.transparency * 100.0)));
                    Renderer2D.COLOR.quad((double)(cellX + 3), (double)(cellY + 3), (double)(ctx.cellSize - 6), (double)(ctx.cellSize - 6), inner);
                }
            }
            Renderer2D.COLOR.render();
        }

        void renderGridLines(MapRenderContext ctx, SettingColor gridColor) {
            int n;
            Color lineColor = new Color((Color)gridColor);
            Color glowColor = new Color((Color)RegionMap.this.accentColor.get());
            glowColor.a = Math.min(255, (int)(ctx.transparency * 120.0));
            Renderer2D.COLOR.begin();
            int mapSize = RegionMap.this.mapData.getMapSize();
            for (n = 0; n <= mapSize; ++n) {
                int lineX = ctx.mapX + n * ctx.cellSize;
                Renderer2D.COLOR.quad((double)lineX, (double)ctx.mapY, 2.0, (double)ctx.getMapHeight(), glowColor);
                Renderer2D.COLOR.quad((double)(lineX + 2), (double)ctx.mapY, 1.0, (double)ctx.getMapHeight(), lineColor);
            }
            for (n = 0; n <= mapSize; ++n) {
                int lineY = ctx.mapY + n * ctx.cellSize;
                Renderer2D.COLOR.quad((double)ctx.mapX, (double)lineY, (double)ctx.getMapWidth(), 2.0, glowColor);
                Renderer2D.COLOR.quad((double)ctx.mapX, (double)(lineY + 2), (double)ctx.getMapWidth(), 1.0, lineColor);
            }
            Renderer2D.COLOR.render();
        }

        void renderRegionNumbers(MapRenderContext ctx, MapDataManager dataManager, double textScale) {
            TextRenderer textRenderer = TextRenderer.get();
            textRenderer.begin(textScale, false, true);
            int mapSize = dataManager.getMapSize();
            for (int row = 0; row < mapSize; ++row) {
                for (int col = 0; col < mapSize; ++col) {
                    RegionInfo regionInfo = dataManager.getRegionInfo(row * mapSize + col);
                    if (regionInfo == null) continue;
                    int cellX = ctx.mapX + col * ctx.cellSize;
                    int cellY = ctx.mapY + row * ctx.cellSize;
                    String numberText = String.valueOf(regionInfo.regionId);
                    double textWidth = textRenderer.getWidth(numberText) * textScale;
                    double textHeight = textRenderer.getHeight() * textScale;
                    double centeredX = (double)cellX + ((double)ctx.cellSize - textWidth) / 2.0;
                    double centeredY = (double)cellY + ((double)ctx.cellSize - textHeight) / 2.0;
                    textRenderer.render(numberText, centeredX + 1.0, centeredY + 1.0, new Color(0, 0, 0, 140), false);
                    textRenderer.render(numberText, centeredX, centeredY, Color.WHITE, false);
                }
            }
            textRenderer.end();
        }
    }

    private class PlayerTracker {
        private PlayerTracker() {
        }

        void renderPlayerPosition(MapRenderContext ctx, Vec3d playerPos, float yaw, SettingColor indicatorColor) {
            int[] gridPos = RegionMap.this.mapData.worldToGrid(playerPos.x, playerPos.z);
            if (gridPos[0] < 0 || gridPos[0] >= RegionMap.this.mapData.getMapSize() || gridPos[1] < 0 || gridPos[1] >= RegionMap.this.mapData.getMapSize()) {
                return;
            }
            double[] cellPos = RegionMap.this.mapData.worldToCellPosition(playerPos.x, playerPos.z);
            int indicatorX = ctx.mapX + gridPos[0] * ctx.cellSize + (int)(cellPos[0] * (double)ctx.cellSize);
            int indicatorY = ctx.mapY + gridPos[1] * ctx.cellSize + (int)(cellPos[1] * (double)ctx.cellSize);
            double rotationAngle = Math.toRadians((double)(-yaw) - 90.0);
            this.renderDirectionalIndicator(indicatorX, indicatorY, rotationAngle, indicatorColor);
        }

        private void renderDirectionalIndicator(int centerX, int centerY, double angle, SettingColor color) {
            Color indicatorColor = new Color((Color)color);
            Color glow = new Color(color.r, color.g, color.b, 120);
            int arrowSize = 10;
            int tipX = centerX + (int)(Math.cos(angle) * (double)arrowSize);
            int tipY = centerY - (int)(Math.sin(angle) * (double)arrowSize);
            double leftBaseAngle = angle + Math.toRadians(140.0);
            double rightBaseAngle = angle - Math.toRadians(140.0);
            int leftBaseX = centerX + (int)(Math.cos(leftBaseAngle) * (double)arrowSize);
            int leftBaseY = centerY - (int)(Math.sin(leftBaseAngle) * (double)arrowSize);
            int rightBaseX = centerX + (int)(Math.cos(rightBaseAngle) * (double)arrowSize);
            int rightBaseY = centerY - (int)(Math.sin(rightBaseAngle) * (double)arrowSize);
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.triangle((double)tipX, (double)tipY, (double)leftBaseX, (double)leftBaseY, (double)rightBaseX, (double)rightBaseY, glow);
            Renderer2D.COLOR.triangle((double)tipX, (double)tipY, (double)leftBaseX, (double)leftBaseY, (double)rightBaseX, (double)rightBaseY, indicatorColor);
            Renderer2D.COLOR.quad((double)(centerX - 2), (double)(centerY - 2), 4.0, 4.0, new Color(0, 0, 0, 180));
            Renderer2D.COLOR.quad((double)(centerX - 1), (double)(centerY - 1), 2.0, 2.0, indicatorColor);
            Renderer2D.COLOR.render();
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

    private static class RegionInfo {
        final int regionId;
        final int regionType;

        RegionInfo(int regionId, int regionType, int gridRow, int gridCol) {
            this.regionId = regionId;
            this.regionType = regionType;
        }
    }
}

