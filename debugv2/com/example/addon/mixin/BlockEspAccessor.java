package com.example.addon.mixin;

import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.BlockESP;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BlockESP.class})
public interface BlockEspAccessor {
   @Accessor("blocks")
   Setting<List<Block>> snow$blocks();

   @Accessor("defaultBlockConfig")
   Setting<ESPBlockData> snow$defaultBlockConfig();

   @Accessor("blockConfigs")
   Setting<Map<Block, ESPBlockData>> snow$blockConfigs();

   @Accessor("tracers")
   Setting<Boolean> snow$tracers();
}
