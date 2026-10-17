package dev.dexter.kryptionians.modules;

import dev.dexter.kryptionians.KryptioniansAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.server.level.ClientInformation;

public class ChunkReloader extends Module {
  private static final long COOLDOWN_MS = 5000L;
  private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
  private final Setting<Integer> triggerY =
      this.sgGeneral.add(
          new IntSetting.Builder()
              .name("trigger-y")
              .defaultValue(-2)
              .range(-64, 320)
              .sliderRange(-64, 0)
              .build());
  private final Setting<Integer> mainRD =
      this.sgGeneral.add(
          new IntSetting.Builder()
              .name("main-render-distance")
              .defaultValue(16)
              .range(2, 32)
              .sliderRange(2, 32)
              .build());
  private final Setting<Integer> lowRD =
      this.sgGeneral.add(
          new IntSetting.Builder()
              .name("low-render-distance")
              .defaultValue(2)
              .range(2, 8)
              .sliderRange(2, 8)
              .build());
  private final Setting<Double> reloadDelay =
      this.sgGeneral.add(
          new DoubleSetting.Builder()
              .name("reload-delay")
              .defaultValue(0.5)
              .min(0.5)
              .max(5.0)
              .sliderMin(0.5)
              .sliderMax(5.0)
              .build());
  private ChunkReloader.Step step = ChunkReloader.Step.IDLE;
  private int waitTicks;
  private long lastReloadMs;
  private int restoreRD;
  private int dropRD;

  public ChunkReloader() {
    super(
        KryptioniansAddon.CATEGORY, "render distance changer", "Changes render distance at low Y.");
  }

  public void onActivate() {
    this.step = ChunkReloader.Step.IDLE;
    this.waitTicks = 0;
    this.lastReloadMs = 0L;
  }

  public void onDeactivate() {
    if (this.step != ChunkReloader.Step.IDLE) {
      this.sendViewDistance(this.mainRD.get());
    }

    this.step = ChunkReloader.Step.IDLE;
    this.waitTicks = 0;
  }

  @EventHandler
  private void onReceivePacket(Receive event) {
    if (this.step != ChunkReloader.Step.IDLE
        && event.packet instanceof ClientboundForgetLevelChunkPacket) {
      event.cancel();
    }
  }

  @EventHandler
  private void onTick(Post event) {
    if (this.mc.player != null && this.mc.level != null && this.mc.options != null) {
      switch (this.step) {
        case IDLE:
          if (this.mc.player.getY() > (double) this.triggerY.get() + Math.random() * 3.0) {
            return;
          }

          if (System.currentTimeMillis() - this.lastReloadMs < 5000L) {
            return;
          }

          this.restoreRD = this.mainRD.get();
          this.dropRD = this.lowRD.get() + (int) (Math.random() * 2.0);
          if (this.restoreRD == this.dropRD) {
            return;
          }

          this.sendViewDistance(this.dropRD);
          this.waitTicks = this.delayTicks() + (int) (Math.random() * 20.0);
          this.step = ChunkReloader.Step.WAITING;
          break;
        case WAITING:
          if (--this.waitTicks > 0) {
            return;
          }

          this.sendViewDistance(this.restoreRD);
          this.lastReloadMs = System.currentTimeMillis();
          this.step = ChunkReloader.Step.IDLE;
          ChatUtils.sendMsg(
              Component.literal("Changed Render Distance Successfully")
                  .withStyle(ChatFormatting.LIGHT_PURPLE));
      }
    }
  }

  private int delayTicks() {
    return Math.max(1, (int) Math.round(this.reloadDelay.get() * 20.0));
  }

  private void sendViewDistance(int distance) {
    if (this.mc.options != null && this.mc.player != null && this.mc.player.connection != null) {
      ClientInformation current = this.mc.options.buildPlayerInformation();
      ClientInformation spoofed =
          new ClientInformation(
              current.language(),
              distance,
              current.chatVisibility(),
              current.chatColors(),
              current.modelCustomisation(),
              current.mainHand(),
              current.textFilteringEnabled(),
              current.allowsListing(),
              current.particleStatus());
      this.mc.player.connection.send(new ServerboundClientInformationPacket(spoofed));
    }
  }

  private static enum Step {
    IDLE,
    WAITING;
  }
}
