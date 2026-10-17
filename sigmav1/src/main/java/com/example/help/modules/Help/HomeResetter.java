
package com.example.help.modules.Help;

import com.example.help.HelpAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

public class HomeResetter
extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<String> homeName;
    private final Setting<Integer> commandDelay;
    private final Setting<Integer> additionalDelay;
    private int step;
    private int timer;

    public HomeResetter() {
        super(HelpAddon.CATEGORY, "home-resetter", "Deletes, sets, rtps to, then teleports home to a chosen home name.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.homeName = this.sgGeneral.add((Setting)((StringSetting.Builder)((StringSetting.Builder)((StringSetting.Builder)new StringSetting.Builder().name("home-name")).description("The home name used for /delhome, /sethome and /home.")).defaultValue("home")).build());
        this.commandDelay = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("command-delay-ticks")).description("Ticks to wait between delhome, sethome and rtp.")).defaultValue(20)).min(0).sliderMin(0).sliderMax(200).build());
        this.additionalDelay = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("additional-command-delay-ticks")).description("Additional ticks to wait after each command (added to the main command delay).")).defaultValue(0)).min(0).sliderMin(0).sliderMax(200).build());
    }

    public void onActivate() {
        this.step = 0;
        this.timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (this.timer > 0) {
            --this.timer;
            return;
        }
        String name = (String)this.homeName.get();
        switch (this.step) {
            case 0: {
                ChatUtils.sendPlayerMsg((String)("/delhome " + name));
                this.timer = (Integer)this.commandDelay.get() + (Integer)this.additionalDelay.get();
                ++this.step;
                break;
            }
            case 1: {
                ChatUtils.sendPlayerMsg((String)("/sethome " + name));
                this.timer = (Integer)this.commandDelay.get() + (Integer)this.additionalDelay.get();
                ++this.step;
                break;
            }
            case 2: {
                ChatUtils.sendPlayerMsg((String)"/rtp");
                this.timer = (Integer)this.commandDelay.get() + (Integer)this.additionalDelay.get();
                ++this.step;
                break;
            }
            case 3: {
                ChatUtils.sendPlayerMsg((String)("/home " + name));
                this.timer = (Integer)this.commandDelay.get() + (Integer)this.additionalDelay.get();
                ++this.step;
                this.toggle();
                break;
            }
            default: {
                this.toggle();
            }
        }
    }
}

