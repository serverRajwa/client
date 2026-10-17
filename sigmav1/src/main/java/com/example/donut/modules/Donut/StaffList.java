
package com.example.donut.modules.Donut;

import com.example.donut.DonutAddon;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.TreeSet;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
public class StaffList
extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup positionSettings;
    private final SettingGroup sgRender;
    private static final TreeSet<String> WATCH_SET = StaffList.createWatchSet();
    private final Setting<Integer> x;
    private final Setting<Integer> y;
    private final Setting<SettingColor> accentColor;
    private final Setting<SettingColor> nameColor;
    private final Setting<SettingColor> backgroundColor;
    private final TreeSet<String> onlineStaff;
    private long nextRefreshTime;
    private static final long REFRESH_INTERVAL_MS = 60000L;

    public StaffList() {
        super(DonutAddon.CATEGORY, "staff-list", "Shows a live GUI list of watched staff currently online.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.positionSettings = this.settings.createGroup("Position");
        this.sgRender = this.settings.createGroup("Render");
        this.x = this.positionSettings.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("position-x")).description("Horizontal position of the staff list panel.")).defaultValue(15)).min(0).sliderMax(2400).build());
        this.y = this.positionSettings.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("position-y")).description("Vertical position of the staff list panel.")).defaultValue(15)).min(0).sliderMax(1080).build());
        this.accentColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("accent-color")).description("Primary accent color used for the header, badge outline, and status glow.")).defaultValue(new SettingColor(190, 110, 255)).build());
        this.nameColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("name-color")).description("Color of staff names.")).defaultValue(new SettingColor(235, 235, 240)).build());
        this.backgroundColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("background-color")).description("Base panel background color.")).defaultValue(new SettingColor(15, 13, 20, 235)).build());
        this.onlineStaff = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        this.nextRefreshTime = 0L;
    }

    public void onActivate() {
        this.onlineStaff.clear();
        this.nextRefreshTime = 0L;
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        if (this.mc.getNetworkHandler() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= this.nextRefreshTime) {
            TreeSet<String> foundThisFrame = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            for (PlayerListEntry entry : this.mc.getNetworkHandler().getPlayerList()) {
                try {
                    String displayName;
                    if (entry == null || entry.getProfile() == null) continue;
                    String username = this.getProfileName(entry);
                    String match = this.findWatchedName(username, displayName = entry.getDisplayName() != null ? entry.getDisplayName().getString() : null);
                    if (match == null) continue;
                    foundThisFrame.add(match);
                }
                catch (Exception exception) {}
            }
            this.onlineStaff.clear();
            this.onlineStaff.addAll(foundThisFrame);
            this.nextRefreshTime = now + 60000L;
        }
        this.render(event, now);
    }

    private void render(Render2DEvent event, long now) {
        TextRenderer tr = TextRenderer.get();
        float s = 1.0f;
        int outerPadding = 12;
        int headerHeight = (int)(tr.getHeight() * (double)s + 22.0);
        int chipHeight = (int)(tr.getHeight() * (double)s + 12.0);
        int chipGap = 6;
        int chipSideMargin = 8;
        String headerText = "STAFF";
        String countText = String.valueOf(this.onlineStaff.size());
        double widest = tr.getWidth(headerText) + tr.getWidth(countText) + 60.0;
        for (String name : this.onlineStaff) {
            widest = Math.max(widest, tr.getWidth(name) + 44.0);
        }
        int panelWidth = (int)(widest * (double)s) + outerPadding * 2;
        int chipCount = this.onlineStaff.isEmpty() ? 1 : this.onlineStaff.size();
        int bodyHeight = chipHeight * chipCount + chipGap * Math.max(0, chipCount - 1);
        int panelHeight = headerHeight + bodyHeight + outerPadding * 2;
        double posX = ((Integer)this.x.get()).intValue();
        double posY = ((Integer)this.y.get()).intValue();
        double radius = 0.0;
        double chipRadius = (double)chipHeight / 2.0;
        SettingColor accent = (SettingColor)this.accentColor.get();
        SettingColor bg = (SettingColor)this.backgroundColor.get();
        double badgeH = tr.getHeight() * (double)s + 8.0;
        double badgeW = Math.max(badgeH, tr.getWidth(countText) * (double)s + 18.0);
        double badgeX = posX + (double)panelWidth - (double)outerPadding - badgeW;
        double badgeY = posY + ((double)headerHeight - badgeH) / 2.0;
        double chipX = posX + (double)chipSideMargin;
        double chipWidth = panelWidth - chipSideMargin * 2;
        Renderer2D.COLOR.begin();
        this.roundedRect(posX - 12.0, posY - 10.0, panelWidth + 24, panelHeight + 24, radius + 10.0, new SettingColor(accent.r, accent.g, accent.b, 14));
        this.roundedRect(posX - 4.0, posY - 2.0, panelWidth + 8, panelHeight + 10, radius + 3.0, new SettingColor(0, 0, 0, 65));
        this.roundedRect(posX, posY, panelWidth, panelHeight, radius, new SettingColor(bg.r, bg.g, bg.b, bg.a));
        double liveDotSize = 5.0;
        double liveDotX = posX + (double)outerPadding;
        double liveDotY = posY + (double)outerPadding + (tr.getHeight() * (double)s - liveDotSize) / 2.0 + 1.0;
        Renderer2D.COLOR.quad(liveDotX - 2.0, liveDotY - 2.0, liveDotSize + 4.0, liveDotSize + 4.0, (Color)new SettingColor(accent.r, accent.g, accent.b, 45));
        Renderer2D.COLOR.quad(liveDotX, liveDotY, liveDotSize, liveDotSize, (Color)accent);
        this.roundedRect(badgeX, badgeY, badgeW, badgeH, badgeH / 2.0, new SettingColor(accent.r, accent.g, accent.b, 200));
        this.roundedRect(badgeX + 1.5, badgeY + 1.5, badgeW - 3.0, badgeH - 3.0, badgeH / 2.0 - 1.5, new SettingColor(bg.r, bg.g, bg.b, bg.a));
        double dividerY = posY + (double)headerHeight - 4.0;
        Renderer2D.COLOR.quad(posX + (double)outerPadding, dividerY, (double)(panelWidth - outerPadding * 2), 1.0, (Color)new SettingColor(accent.r, accent.g, accent.b, 90));
        Renderer2D.COLOR.quad(posX + (double)outerPadding, dividerY + 1.0, (double)(panelWidth - outerPadding * 2), 1.0, (Color)new SettingColor(accent.r, accent.g, accent.b, 35));
        if (!this.onlineStaff.isEmpty()) {
            int row = 0;
            for (String name : this.onlineStaff) {
                double chipY = posY + (double)headerHeight + (double)(row * (chipHeight + chipGap));
                this.roundedRect(chipX, chipY, chipWidth, chipHeight, chipRadius, new SettingColor(255, 255, 255, 10));
                double dotSize = 6.0;
                double dotX = chipX + chipRadius - dotSize / 2.0;
                double dotY = chipY + ((double)chipHeight - dotSize) / 2.0;
                Renderer2D.COLOR.quad(dotX - 3.0, dotY - 3.0, dotSize + 6.0, dotSize + 6.0, (Color)new SettingColor(95, 235, 145, 35));
                Renderer2D.COLOR.quad(dotX, dotY, dotSize, dotSize, (Color)new SettingColor(95, 235, 145, 255));
                ++row;
            }
        } else {
            this.roundedRect(chipX, posY + (double)headerHeight, chipWidth, chipHeight, chipRadius, new SettingColor(255, 255, 255, 6));
        }
        Renderer2D.COLOR.render();
        double headerTextY = posY + (double)outerPadding;
        tr.begin((double)s, false, true);
        tr.render("STAFF", liveDotX + liveDotSize + 8.0, headerTextY, (Color)new SettingColor(235, 235, 240), true);
        double countTextX = badgeX + (badgeW - tr.getWidth(countText) * (double)s) / 2.0;
        double countTextY = badgeY + (badgeH - tr.getHeight() * (double)s) / 2.0;
        tr.render(countText, countTextX, countTextY, (Color)new SettingColor(235, 235, 240), true);
        for (String string : this.onlineStaff) {
        }
        tr.end();
    }

    private void roundedRect(double x, double y, double w, double h, double radius, SettingColor color) {
        this.roundedRect(x, y, w, h, radius, color, true, true, true, true);
    }

    private void roundedRect(double x, double y, double w, double h, double radius, SettingColor color, boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        if ((radius = Math.max(0.0, Math.min(radius, Math.min(w, h) / 2.0))) < 0.5) {
            Renderer2D.COLOR.quad(x, y, w, h, (Color)color);
            return;
        }
        Renderer2D.COLOR.quad(x + radius, y, w - radius * 2.0, h, (Color)color);
        Renderer2D.COLOR.quad(x, y + radius, radius, h - radius * 2.0, (Color)color);
        Renderer2D.COLOR.quad(x + w - radius, y + radius, radius, h - radius * 2.0, (Color)color);
        if (topLeft) {
            this.fillCorner(x + radius, y + radius, radius, 180.0, 270.0, color);
        } else {
            Renderer2D.COLOR.quad(x, y, radius, radius, (Color)color);
        }
        if (topRight) {
            this.fillCorner(x + w - radius, y + radius, radius, 270.0, 360.0, color);
        } else {
            Renderer2D.COLOR.quad(x + w - radius, y, radius, radius, (Color)color);
        }
        if (bottomLeft) {
            this.fillCorner(x + radius, y + h - radius, radius, 90.0, 180.0, color);
        } else {
            Renderer2D.COLOR.quad(x, y + h - radius, radius, radius, (Color)color);
        }
        if (bottomRight) {
            this.fillCorner(x + w - radius, y + h - radius, radius, 0.0, 90.0, color);
        } else {
            Renderer2D.COLOR.quad(x + w - radius, y + h - radius, radius, radius, (Color)color);
        }
    }

    private void fillCorner(double cx, double cy, double radius, double startDeg, double endDeg, SettingColor color) {
        int segments = 24;
        double startRad = Math.toRadians(startDeg);
        double endRad = Math.toRadians(endDeg);
        double step = (endRad - startRad) / (double)segments;
        double prevX = cx + Math.cos(startRad) * radius;
        double prevY = cy + Math.sin(startRad) * radius;
        for (int n = 1; n <= segments; ++n) {
            double angle = startRad + step * (double)n;
            double px = cx + Math.cos(angle) * radius;
            double py = cy + Math.sin(angle) * radius;
            Renderer2D.COLOR.triangle(cx, cy, prevX, prevY, px, py, (Color)color);
            prevX = px;
            prevY = py;
        }
    }

    private static TreeSet<String> createWatchSet() {
        TreeSet<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        set.add("archivePedro");
        set.add("w1zox_");
        set.add("Bautiedgar");
        set.add("Pastagamer08");
        set.add("Fluffymaster07");
        set.add("Showered");
        set.add("Itszdeath");
        set.add("Drdonutt");
        set.add("frwost");
        set.add("0Gsummer");
        set.add("Munkerlich");
        set.add("_chaon");
        set.add("Dough4");
        return set;
    }

    private String getProfileName(PlayerListEntry entry) {
        Object r;
        Method m2;
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return "";
        }
        try {
            m2 = profile.getClass().getMethod("getName", new Class[0]);
            r = m2.invoke(profile, new Object[0]);
            if (r != null) {
                return r.toString();
            }
        }
        catch (Exception ignored) {
            // empty catch block
        }
        try {
            Field field = profile.getClass().getField("name");
            r = field.get(profile);
            if (r != null) {
                return r.toString();
            }
        }
        catch (Exception field) {
            // empty catch block
        }
        try {
            m2 = profile.getClass().getMethod("getId", new Class[0]);
            r = m2.invoke(profile, new Object[0]);
            if (r != null) {
                return r.toString();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "";
    }

    private String findWatchedName(String username, String displayName) {
        if (username != null && !username.isEmpty()) {
            for (String target : WATCH_SET) {
                if (!target.equalsIgnoreCase(username)) continue;
                return target;
            }
        }
        if (displayName != null && !displayName.isEmpty()) {
            String normalizedDisplay = this.normalizeName(displayName);
            for (String target : WATCH_SET) {
                if (displayName.equalsIgnoreCase(target)) {
                    return target;
                }
                if (displayName.toLowerCase().contains(target.toLowerCase())) {
                    return target;
                }
                if (normalizedDisplay.equalsIgnoreCase(this.normalizeName(target))) {
                    return target;
                }
                if (!normalizedDisplay.toLowerCase().contains(this.normalizeName(target).toLowerCase())) continue;
                return target;
            }
        }
        return null;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.replaceAll("[^A-Za-z0-9_]", "");
    }
}

