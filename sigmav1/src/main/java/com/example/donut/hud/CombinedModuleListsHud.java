
package com.example.donut.hud;

import com.example.donut.DonutAddon;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class CombinedModuleListsHud
extends HudElement {
    public static final HudElementInfo<CombinedModuleListsHud> INFO = new HudElementInfo(DonutAddon.HUD_GROUP, "combined-module-lists", "Shows Donut and Help module lists side-by-side.", CombinedModuleListsHud::new);

    public CombinedModuleListsHud() {
        super(INFO);
    }

    public void render(HudRenderer renderer) {
        Collection<Module> all = Modules.get().getAll();
        ArrayList<String> left = new ArrayList<String>();
        ArrayList<String> right = new ArrayList<String>();
        for (Module m : all) {
            Category cat = this.getCategorySafe(m);
            String name = this.getNameSafe(m);
            String catName = this.getCategoryNameSafe(cat);
            if ("Donut Leaked by Dexter".equalsIgnoreCase(catName)) {
                left.add(name);
                continue;
            }
            if (cat != DonutAddon.CATEGORY && !"Help Leaked by Dexter".equalsIgnoreCase(catName)) continue;
            right.add(name);
        }
        int leftCount = Math.max(1, left.size());
        int rightCount = Math.max(1, right.size());
        String longestLeft = "";
        String longestRight = "";
        for (String s : left) {
            if (s.length() <= longestLeft.length()) continue;
            longestLeft = s;
        }
        for (String s : right) {
            if (s.length() <= longestRight.length()) continue;
            longestRight = s;
        }
        double pad = 6.0;
        double lw = renderer.textWidth(longestLeft, true);
        double rw = renderer.textWidth(longestRight, true);
        double colGap = 12.0;
        double colWidth = Math.max(lw, 50.0) + pad * 2.0;
        double rightColWidth = Math.max(rw, 50.0) + pad * 2.0;
        double totalW = colWidth + colGap + rightColWidth;
        double lineH = renderer.textHeight(true);
        double totalH = (double)Math.max(leftCount, rightCount) * lineH;
        this.setSize(totalW, totalH);
        renderer.quad((double)(this.x - 4), (double)(this.y - 4), (double)(this.getWidth() + 8), (double)(this.getHeight() + 8), new Color(20, 20, 22, 200));
        double dy = 0.0;
        if (left.isEmpty()) {
            renderer.text("No modules", (double)this.x + pad, (double)this.y + dy, Color.WHITE, true);
        } else {
            for (String s : left) {
                renderer.text(s, (double)this.x + pad, (double)this.y + dy, Color.WHITE, true);
                dy += lineH;
            }
        }
        dy = 0.0;
        double rightX = (double)this.x + colWidth + colGap;
        if (right.isEmpty()) {
            renderer.text("No modules", rightX + pad, (double)this.y + dy, Color.WHITE, true);
        } else {
            for (String s : right) {
                renderer.text(s, rightX + pad, (double)this.y + dy, Color.WHITE, true);
                dy += lineH;
            }
        }
    }

    private String getNameSafe(Module m) {
        Object r;
        try {
            Method mn = m.getClass().getMethod("getName", new Class[0]);
            r = mn.invoke(m, new Object[0]);
            if (r != null) {
                return r.toString();
            }
        }
        catch (Exception mn) {
            // empty catch block
        }
        try {
            Field field = m.getClass().getDeclaredField("name");
            field.setAccessible(true);
            r = field.get(m);
            if (r != null) {
                return r.toString();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return m.getClass().getSimpleName();
    }

    private Category getCategorySafe(Module m) {
        Object r;
        try {
            Method mc = m.getClass().getMethod("getCategory", new Class[0]);
            r = mc.invoke(m, new Object[0]);
            if (r instanceof Category) {
                return (Category)r;
            }
        }
        catch (Exception mc) {
            // empty catch block
        }
        try {
            Field field = m.getClass().getDeclaredField("category");
            field.setAccessible(true);
            r = field.get(m);
            if (r instanceof Category) {
                return (Category)r;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private String getCategoryNameSafe(Category c) {
        Object r;
        if (c == null) {
            return "";
        }
        try {
            Method mn = c.getClass().getMethod("getName", new Class[0]);
            r = mn.invoke(c, new Object[0]);
            if (r != null) {
                return r.toString();
            }
        }
        catch (Exception mn) {
            // empty catch block
        }
        try {
            Field field = c.getClass().getDeclaredField("name");
            field.setAccessible(true);
            r = field.get(c);
            if (r != null) {
                return r.toString();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            return c.toString();
        }
        catch (Exception exception) {
            return "";
        }
    }
}

