package dev.dexter.kryptionians.modules;

import dev.dexter.kryptionians.KryptioniansAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class UdRelog extends Module {
  private static final int HOME_DELAY_TICKS = 5;
  private static final int RTP_DELAY_TICKS = 10;
  private static final int RTP_DONE_MOVE_SQ = 64;
  private static final int RTP_TIMEOUT_TICKS = 600;
  private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
  private final Setting<Integer> triggerY =
      this.sgGeneral.add(
          new IntSetting.Builder()
              .name("trigger-y")
              .defaultValue(-2)
              .range(-64, -2)
              .sliderRange(-64, -2)
              .build());
  private final Setting<Integer> homeSlot =
      this.sgGeneral.add(
          new IntSetting.Builder()
              .name("home-slot")
              .defaultValue(1)
              .range(1, 3)
              .sliderRange(1, 3)
              .build());
  private final Setting<Integer> redoDelay =
      this.sgGeneral.add(
          new IntSetting.Builder()
              .name("redo-delay")
              .defaultValue(10)
              .range(1, 120)
              .sliderRange(1, 60)
              .build());
  private UdRelog.Step step = UdRelog.Step.WAITING_FOR_Y;
  private int delayTicks;
  private int cooldownTicks;
  private int rtpWaitTicks;
  private double rtpOriginX;
  private double rtpOriginY;
  private double rtpOriginZ;
  private boolean rtpFinished;

  public UdRelog() {
    super(KryptioniansAddon.CATEGORY, "ud relog", "Underground relog sequence.");
  }

  public void onActivate() {
    this.resetState();
  }

  public void onDeactivate() {
    this.resetState();
  }

  private void resetState() {
    this.step = UdRelog.Step.WAITING_FOR_Y;
    this.delayTicks = 0;
    this.cooldownTicks = 0;
    this.rtpWaitTicks = 0;
    this.rtpFinished = false;
  }

  @EventHandler
  private void onTick(Post event) {
    if (this.mc.player != null && this.mc.level != null) {
      if (this.cooldownTicks > 0) {
        this.cooldownTicks--;
      } else {
        switch (this.step) {
          case WAITING_FOR_Y:
            if (this.mc.player.getY() <= (double) this.triggerY.get()) {
              this.sendCommand("delhome " + this.homeSlot.get());
              this.delayTicks = 5;
              this.step = UdRelog.Step.WAIT_SET_HOME;
            }
            break;
          case WAIT_SET_HOME:
            if (this.tickDelayDone()) {
              this.sendCommand("sethome " + this.homeSlot.get());
              this.delayTicks = 10;
              this.step = UdRelog.Step.WAIT_RTP;
            }
            break;
          case WAIT_RTP:
            if (this.tickDelayDone()) {
              this.sendCommand("rtp");
              this.rtpOriginX = this.mc.player.getX();
              this.rtpOriginY = this.mc.player.getY();
              this.rtpOriginZ = this.mc.player.getZ();
              this.rtpWaitTicks = 0;
              this.rtpFinished = false;
              this.step = UdRelog.Step.WAIT_RTP_DONE;
            }
            break;
          case WAIT_RTP_DONE:
            this.rtpWaitTicks++;
            if (this.rtpFinished || this.rtpMoved() || this.rtpWaitTicks >= 600) {
              this.delayTicks = 5;
              this.step = UdRelog.Step.WAIT_HOME;
            }
            break;
          case WAIT_HOME:
            if (this.tickDelayDone()) {
              this.sendCommand("home " + this.homeSlot.get());
              this.cooldownTicks = this.redoDelay.get() * 20;
              this.step = UdRelog.Step.WAITING_FOR_Y;
            }
        }
      }
    }
  }

  @EventHandler
  private void onReceiveMessage(ReceiveMessageEvent event) {
    if (this.step == UdRelog.Step.WAIT_RTP_DONE) {
      String message = event.getMessage().getString().toLowerCase();
      if (message.contains("random location") || message.contains("teleported")) {
        this.rtpFinished = true;
      }
    }
  }

  private boolean rtpMoved() {
    double dx = this.mc.player.getX() - this.rtpOriginX;
    double dy = this.mc.player.getY() - this.rtpOriginY;
    double dz = this.mc.player.getZ() - this.rtpOriginZ;
    return dx * dx + dy * dy + dz * dz >= 64.0;
  }

  private boolean tickDelayDone() {
    if (this.delayTicks > 0) {
      this.delayTicks--;
      return false;
    } else {
      return true;
    }
  }

  private void sendCommand(String command) {
    if (this.mc.player != null && this.mc.player.connection != null) {
      this.mc.player.connection.sendCommand(command);
    }
  }

  private static enum Step {
    WAITING_FOR_Y,
    WAIT_SET_HOME,
    WAIT_RTP,
    WAIT_RTP_DONE,
    WAIT_HOME;
  }
}
