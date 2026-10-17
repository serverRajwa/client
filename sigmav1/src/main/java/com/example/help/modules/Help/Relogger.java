
package com.example.help.modules.Help;

import com.example.help.HelpAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
public class Relogger
extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<Integer> yLevel;
    private final Setting<Integer> cooldownSeconds;
    private int cooldownTicks;
    private int joinGraceTicks;
    private boolean waitingForJoin;

    public Relogger() {
        super(HelpAddon.CATEGORY, "relogger", "Disconnects you when you fall below a Y level, then applies a cooldown.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.yLevel = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("y-level")).description("Disconnect when you drop below this Y level.")).defaultValue(-6)).sliderMin(-64).sliderMax(64).build());
        this.cooldownSeconds = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("cooldown-seconds")).description("Seconds after disconnecting or joining before this can trigger again.")).defaultValue(20)).min(0).sliderMin(0).sliderMax(120).build());
        this.cooldownTicks = 0;
        this.joinGraceTicks = 0;
        this.waitingForJoin = true;
    }

    public void onActivate() {
        this.cooldownTicks = 0;
        this.joinGraceTicks = (Integer)this.cooldownSeconds.get() * 20;
        this.waitingForJoin = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (this.waitingForJoin) {
            if (this.joinGraceTicks > 0) {
                --this.joinGraceTicks;
                return;
            }
            this.waitingForJoin = false;
        }
        if (this.cooldownTicks > 0) {
            --this.cooldownTicks;
            return;
        }
        if (mc.player.getY() < (double)((Integer)this.yLevel.get()).intValue()) {
            this.cooldownTicks = (Integer)this.cooldownSeconds.get() * 20;
            mc.execute(() -> {
                if (mc.player != null && mc.player.networkHandler != null) {
                    mc.player.networkHandler.getConnection().disconnect((Text)Text.literal((String)("Disconnecting: below Y " + String.valueOf(this.yLevel.get()))));
                } else {
                    mc.disconnect((Text)Text.literal((String)("Disconnecting: below Y " + String.valueOf(this.yLevel.get()))));
                }
            });
        }
    }
}

