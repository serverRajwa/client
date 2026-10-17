
package com.example.donut;

import com.example.donut.commands.CommandExample;
import com.example.donut.commands.StaffListDebugCommand;
import com.example.donut.hud.CombinedModuleListsHud;
import com.example.donut.hud.HudExample;
import com.example.donut.hud.ModuleListHud;
import com.example.donut.modules.Donut.LightBypasser;
import com.example.donut.modules.Donut.SigmaBaseFinder;
import com.example.donut.modules.Donut.SpawnerNotifier;
import com.example.donut.modules.Donut.StaffList;
import com.example.help.HelpAddon;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class DonutAddon
extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Donut");
    public static final HudGroup HUD_GROUP = new HudGroup("Donut");
    private final HelpAddon helpAddon = new HelpAddon();

    public void onInitialize() {
        LOG.info("Initializing Donut Leaked by Dexter");
        Modules.get().add((Module)new SigmaBaseFinder());
        Modules.get().add((Module)new SpawnerNotifier());
        Modules.get().add((Module)new StaffList());
        Modules.get().add((Module)new LightBypasser());
        Commands.add((Command)new CommandExample());
        Commands.add((Command)new StaffListDebugCommand());
        Hud.get().register(HudExample.INFO);
        Hud.get().register(ModuleListHud.INFO);
        Hud.get().register(CombinedModuleListsHud.INFO);
        this.helpAddon.onInitialize();
    }

    public void onRegisterCategories() {
        Modules.registerCategory((Category)CATEGORY);
        this.helpAddon.onRegisterCategories();
    }

    public String getPackage() {
        return "com.example.donut";
    }

    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }

    public String getName() {
        return "Sigma Addon Leaked";
    }
}
