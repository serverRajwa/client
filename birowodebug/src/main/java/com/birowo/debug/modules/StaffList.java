package com.birowo.debug.modules;

import com.birowo.debug.BirowoDebugAddon;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.TreeSet;

public class StaffList extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> posX = sgGeneral.add(
        new IntSetting.Builder()
            .name("pos-x")
            .description("X position of the staff list.")
            .defaultValue(15)
            .min(0)
            .max(3000)
            .sliderMax(2400)
            .build()
    );

    private final Setting<Integer> posY = sgGeneral.add(
        new IntSetting.Builder()
            .name("pos-y")
            .description("Y position of the staff list.")
            .defaultValue(15)
            .min(0)
            .max(3000)
            .sliderMax(1080)
            .build()
    );

    private final Setting<SettingColor> accentColor = sgRender.add(
        new SettingColor.Builder()
            .name("accent-color")
            .description("Accent color for the panel.")
            .defaultValue(190, 110, 255)
            .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(
        new SettingColor.Builder()
            .name("name-color")
            .description("Color of staff names.")
            .defaultValue(235, 235, 240)
            .build()
    );

    private final Setting<SettingColor> backgroundColor = sgRender.add(
        new SettingColor.Builder()
            .name("background-color")
            .description("Background color.")
            .defaultValue(15, 13, 20, 235)
            .build()
    );

    private final TreeSet<String> WATCH_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final TreeSet<String> onlineStaff = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private long nextRefreshTime = 0;
    private static final long REFRESH_INTERVAL_MS = 60000L;

    public StaffList() {
        super(BirowoDebugAddon.CATEGORY_INFO, "staff-list", "Shows a list of watched staff currently online.");

        // Default staff list - customize as needed
        WATCH_SET.add("Staff1");
        WATCH_SET.add("Staff2");
        WATCH_SET.add("Admin1");
    }

    @Override
    public void onActivate() {
        onlineStaff.clear();
        nextRefreshTime = 0;
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        if (mc.getNetworkHandler() == null) return;

        long now = System.currentTimeMillis();
        if (now >= nextRefreshTime) {
            TreeSet<String> foundThisFrame = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry == null || entry.getProfile() == null) continue;

                String username = getProfileName(entry);
                String displayName = entry.getDisplayName() != null ? entry.getDisplayName().getString() : null;
                String match = findWatchedName(username, displayName);

                if (match != null) {
                    foundThisFrame.add(match);
                }
            }

            onlineStaff.clear();
            onlineStaff.addAll(foundThisFrame);
            nextRefreshTime = now + REFRESH_INTERVAL_MS;
        }

        renderPanel(event);
    }

    private void renderPanel(Render2DEvent event) {
        TextRenderer tr = TextRenderer.get();
        double scale = 1.0;
        int padding = 12;
        int headerHeight = (int)(tr.getHeight() * scale + 22);
        int chipHeight = (int)(tr.getHeight() * scale + 12);
        int chipGap = 6;
        int chipSideMargin = 8;

        String headerText = "STAFF";
        String countText = String.valueOf(onlineStaff.size());

        double widest = Math.max(
            tr.getWidth(headerText) + tr.getWidth(countText) + 60,
            tr.getWidth("No staff online") + 60
        );

        for (String name : onlineStaff) {
            widest = Math.max(widest, tr.getWidth(name) + 44);
        }

        int panelWidth = (int)(widest * scale) + padding * 2;
        int chipCount = onlineStaff.isEmpty() ? 1 : onlineStaff.size();
        int bodyHeight = chipHeight * chipCount + chipGap * Math.max(0, chipCount - 1);
        int panelHeight = headerHeight + bodyHeight + padding * 2;

        double x = posX.get();
        double y = posY.get();

        SettingColor accent = accentColor.get();
        SettingColor bg = backgroundColor.get();

        // Draw rounded rect background
        drawRoundedRect(x - 4, y - 2, panelWidth + 8, panelHeight + 10, 13, new SettingColor(0, 0, 0, 65));
        drawRoundedRect(x, y, panelWidth, panelHeight, 10, bg);

        // Draw header divider
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x + padding, y + headerHeight - 4, panelWidth - padding * 2, 1, new SettingColor(accent.r, accent.g, accent.b, 90));
        Renderer2D.COLOR.render();

        // Draw header text
        tr.begin(scale, false, true);
        tr.render(headerText, x + padding + 13, y + padding, nameColor.get(), true);
        tr.end();

        // Draw staff names
        int row = 0;
        for (String name : onlineStaff) {
            double chipY = y + headerHeight + (row * (chipHeight + chipGap));
            double dotX = x + chipSideMargin + 3;
            double dotY = chipY + (chipHeight - 6) / 2;

            // Green dot
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(dotX, dotY, 6, 6, new SettingColor(95, 235, 145, 255));
            Renderer2D.COLOR.render();

            // Name text
            tr.begin(scale, false, true);
            tr.render(name, dotX + 14, chipY + (chipHeight - tr.getHeight()) / 2, nameColor.get(), true);
            tr.end();

            row++;
        }

        if (onlineStaff.isEmpty()) {
            double chipY = y + headerHeight;
            tr.begin(scale, false, true);
            tr.render("No staff online", x + chipSideMargin, chipY + (chipHeight - tr.getHeight()) / 2, new SettingColor(150, 150, 150), true);
            tr.end();
        }
    }

    private void drawRoundedRect(double x, double y, double w, double h, double r, SettingColor color) {
        if (r < 0.5) {
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x, y, w, h, color);
            Renderer2D.COLOR.render();
            return;
        }

        Renderer2D.COLOR.begin();
        // Top and bottom
        Renderer2D.COLOR.quad(x + r, y, w - r * 2, h, color);
        // Left and right
        Renderer2D.COLOR.quad(x, y + r, w, h - r * 2, color);
        Renderer2D.COLOR.render();
    }

    private String getProfileName(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) return "";

        try {
            Method m = profile.getClass().getMethod("getName");
            Object result = m.invoke(profile);
            if (result != null) return result.toString();
        } catch (Exception ignored) {}

        try {
            Field field = profile.getClass().getField("name");
            Object result = field.get(profile);
            if (result != null) return result.toString();
        } catch (Exception ignored) {}

        return profile.getName();
    }

    private String findWatchedName(String username, String displayName) {
        if (username != null && !username.isEmpty()) {
            for (String target : WATCH_SET) {
                if (target.equalsIgnoreCase(username)) {
                    return target;
                }
            }
        }

        if (displayName != null && !displayName.isEmpty()) {
            String normalized = normalizeName(displayName);
            for (String target : WATCH_SET) {
                if (displayName.equalsIgnoreCase(target) ||
                    displayName.toLowerCase().contains(target.toLowerCase()) ||
                    normalized.equalsIgnoreCase(normalizeName(target))) {
                    return target;
                }
            }
        }

        return null;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.replaceAll("[^A-Za-z0-9_]", "");
    }
}
