
package com.example.help;

import com.example.help.commands.CommandExample;
import com.example.help.commands.StaffListDebugCommand;
import com.example.help.hud.CombinedModuleListsHud;
import com.example.help.hud.HudExample;
import com.example.help.hud.ModuleListHud;
import com.example.help.modules.Help.BedrockVoidEsp;
import com.example.help.modules.Help.HomeResetter;
import com.example.help.modules.Help.RegionMap;
import com.example.help.modules.Help.Relogger;
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

public class HelpAddon
extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Help");
    public static final HudGroup HUD_GROUP = new HudGroup("Help");

    public void onInitialize() {
        LOG.info("Initializing Help Leaked by Dexter");
        Modules.get().add((Module)new RegionMap());
        Modules.get().add((Module)new Relogger());
        Modules.get().add((Module)new HomeResetter());
        Modules.get().add((Module)new BedrockVoidEsp());
        Commands.add((Command)new CommandExample());
        Commands.add((Command)new StaffListDebugCommand());
        Hud.get().register(HudExample.INFO);
        Hud.get().register(ModuleListHud.INFO);
        Hud.get().register(CombinedModuleListsHud.INFO);
    }

    public void onRegisterCategories() {
        Modules.registerCategory((Category)CATEGORY);
    }

    public String getPackage() {
        return "com.example.help";
    }

    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }

    public String getName() {
        return "Sigma Addon Leaked";
    }
}
