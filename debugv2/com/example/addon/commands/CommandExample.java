package com.example.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
public class CommandExample extends Command {
   public void build(LiteralArgumentBuilder<CommandSource> a) {
      a.executes(ax -> {
         Object[] obj = new Object[0];
         boolean active = true;
         a.info("hi", obj);
         return 1;
      });
      a.then(literal("name").then(argument("nameArgument", StringArgumentType.word()).executes(ax -> {
         CommandExample commandExample = StringArgumentType.getString(ax, "nameArgument");
         String str = "hi, " + commandExample;
         Object[] obj = new Object[0];
         boolean active = true;
         a.info(str, obj);
         return 1;
      })));
   }

   public CommandExample() {
      String[] str = new String[0];
      boolean active = true;
      super("example", "Sends a message.", str);
   }
}
