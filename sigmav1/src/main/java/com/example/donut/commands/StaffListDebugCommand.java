
package com.example.donut.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.client.network.PlayerListEntry;
public class StaffListDebugCommand
extends Command {
    public StaffListDebugCommand() {
        super("staffdebug", "Prints raw tab list display name character codes.", new String[0]);
    }

    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.getNetworkHandler() == null) {
                this.info("Not connected to a server.", new Object[0]);
                return 1;
            }
            int count = 0;
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                Text displayName = entry.getDisplayName();
                String profileName = this.getProfileName(entry);
                String plain = displayName != null ? displayName.getString() : profileName;
                StringBuilder codes = new StringBuilder();
                for (int n = 0; n < plain.length(); ++n) {
                    char c = plain.charAt(n);
                    codes.append(String.format("U+%04X", c));
                    if (n >= plain.length() - 1) continue;
                    codes.append(" ");
                }
                this.info(profileName + " | raw=\"" + plain + "\" | codes: " + String.valueOf(codes), new Object[0]);
                if (++count < 40) continue;
                this.info("... stopped after 40 players to avoid chat spam.", new Object[0]);
                break;
            }
            if (count == 0) {
                this.info("No players found in tab list.", new Object[0]);
            }
            return 1;
        });
    }

    private String getProfileName(PlayerListEntry entry) {
        Object r;
        Method m2;
        GameProfile profile = entry.getProfile();
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
        return "Unknown";
    }
}

