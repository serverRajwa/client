
package com.example.donut.mixin;

import com.example.donut.DonutAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MinecraftClient.class})
public abstract class ExampleMixin {
    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void onGameLoaded(RunArgs args, CallbackInfo ci) {
        DonutAddon.LOG.info("Hello from ExampleMixin!");
    }
}

