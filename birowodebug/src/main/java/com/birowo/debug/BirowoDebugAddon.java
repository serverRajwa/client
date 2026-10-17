package com.birowo.debug;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class BirowoDebugAddon extends MeteorAddon {
    public static final Logger LOG = org.slf4j.LoggerFactory.getLogger("BirowoDebug");
    public static final String VERSION = "1.0";

    public static final Category CATEGORY_GENERAL = new Category("BirowoDebug");

    @Override
    public void onInitialize() {
        LOG.info("BirowoDebug v{} initialized.", VERSION);
        Modules.registerCategory(CATEGORY_GENERAL);
    }

    @Override
    public String getPackage() {
        return "com.birowo.debug";
    }
}
