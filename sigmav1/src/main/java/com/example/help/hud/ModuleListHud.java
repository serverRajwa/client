
package com.example.help.hud;

import com.example.help.HelpAddon;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class ModuleListHud
extends HudElement {
    public static final HudElementInfo<ModuleListHud> INFO = new HudElementInfo(HelpAddon.HUD_GROUP, "module-list", "Shows modules for the Help category.", ModuleListHud::new);

    public ModuleListHud() {
        super(INFO);
    }

    public void render(HudRenderer renderer) {
        Collection<Module> modules = Modules.get().getAll();
        String longest = "";
        int count = 0;
        for (Module m : modules) {
            Category cat = this.getCategorySafe(m);
            if (cat != HelpAddon.CATEGORY) continue;
            String name = this.getNameSafe(m);
            if (name.length() > longest.length()) {
                longest = name;
            }
            ++count;
        }
        if (count == 0) {
            this.setSize(renderer.textWidth("No modules", true), renderer.textHeight(true));
            renderer.text("No modules", (double)this.x, (double)this.y, Color.WHITE, true);
            return;
        }
        double w = renderer.textWidth(longest, true) + 8.0;
        double h = renderer.textHeight(true) * (double)count;
        this.setSize(w, h);
        double dy = 0.0;
        for (Module m : modules) {
            Category cat = this.getCategorySafe(m);
            if (cat != HelpAddon.CATEGORY) continue;
            renderer.text(this.getNameSafe(m), (double)this.x, (double)this.y + dy, Color.WHITE, true);
            dy += renderer.textHeight(true);
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
}

