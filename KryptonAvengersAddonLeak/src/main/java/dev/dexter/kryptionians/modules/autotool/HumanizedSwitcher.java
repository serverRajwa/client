package dev.dexter.kryptionians.modules.autotool;

import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class HumanizedSwitcher {
  private static final Minecraft MC = Minecraft.getInstance();
  private static final int ABSOLUTE_FLOOR_MS = 45;

  private final HumanBehaviorEngine engine;

  private long switchAtMs = 0L;
  private long switchBackAtMs = 0L;
  private long queuedAtMs = 0L;
  private int pendingSlot = -1;
  private BlockPos pendingBlock = null;
  private boolean pendingMine;
  private boolean attackHeldLastTick;

  public HumanizedSwitcher(HumanBehaviorEngine engine) {
    this.engine = engine;
  }

  public void clear() {
    switchAtMs = 0L;
    switchBackAtMs = 0L;
    queuedAtMs = 0L;
    pendingSlot = -1;
    pendingBlock = null;
    pendingMine = false;
  }

  public boolean hasPendingSwitch() {
    return switchAtMs > 0L && pendingSlot >= 0;
  }

  public void cancelPending() {
    clearPendingSwitch();
  }

  public void queueSwitch(int slot, BlockPos block, boolean mining, long actionStartMs) {
    if (slot < 0 || slot > 8 || MC.player == null) return;
    if (MC.screen != null) return;

    if (engine.shouldSkipSwitch()) {
      clearPendingSwitch();
      return;
    }

    long now = System.currentTimeMillis();
    if (now - actionStartMs < engine.requiredHoldMs()) return;

    pendingSlot = slot;
    pendingBlock = block == null ? null : block.immutable();
    pendingMine = mining;
    queuedAtMs = now;

    long delay = engine.nextSwitchDelayMs(!mining);
    switchAtMs = now + delay;
  }

  public void queueSwitchBack() {
    if (!engine.switchBackEnabled() || InvUtils.previousSlot == -1 || MC.player == null) return;
    if (engine.shouldSkipSwitchBack()) return;
    switchBackAtMs = System.currentTimeMillis() + engine.nextSwitchBackDelayMs();
  }

  public void tick() {
    if (MC.player == null || MC.level == null) return;
    if (MC.screen != null) {
      clearPendingSwitch();
      return;
    }

    engine.observeSelectedSlot(MC.player.getInventory().getSelectedSlot());

    long now = System.currentTimeMillis();

    if (switchAtMs > 0L && now >= switchAtMs) {
      executePendingSwitch();
    }

    boolean attackHeld = MC.options.keyAttack.isDown();
    if (engine.switchBackEnabled()
        && attackHeldLastTick
        && !attackHeld
        && InvUtils.previousSlot != -1) {
      queueSwitchBack();
    }
    attackHeldLastTick = attackHeld;

    if (switchBackAtMs > 0L && now >= switchBackAtMs) {
      if (InvUtils.previousSlot != -1 && !MC.options.keyAttack.isDown() && engine.canSwitchNow()) {
        InvUtils.swapBack();
        engine.recordSwitch();
      }
      switchBackAtMs = 0L;
    }
  }

  private void executePendingSwitch() {
    switchAtMs = 0L;

    if (pendingSlot < 0 || MC.player == null) return;
    if (MC.player.getInventory().getSelectedSlot() == pendingSlot) {
      clearPendingSwitch();
      return;
    }

    if (pendingBlock != null && !stillTargeting(pendingBlock, pendingMine)) {
      clearPendingSwitch();
      return;
    }

    if (!engine.canSwitchNow()) {
      switchAtMs =
          System.currentTimeMillis() + Math.max(20, engine.nextSwitchDelayMs(!pendingMine) / 3);
      return;
    }

    long elapsed = System.currentTimeMillis() - queuedAtMs;
    if (elapsed < ABSOLUTE_FLOOR_MS) {
      switchAtMs = System.currentTimeMillis() + (ABSOLUTE_FLOOR_MS - elapsed);
      return;
    }

    InvUtils.swap(pendingSlot, engine.switchBackEnabled());
    engine.recordSwitch();
    clearPendingSwitch();
  }

  private boolean stillTargeting(BlockPos block, boolean mining) {
    HitResult hit = MC.hitResult;
    boolean holding = mining ? MC.options.keyAttack.isDown() : MC.options.keyUse.isDown();
    if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
      return holding;
    }

    return holding || blockHit.getBlockPos().equals(block);
  }

  private void clearPendingSwitch() {
    switchAtMs = 0L;
    queuedAtMs = 0L;
    pendingSlot = -1;
    pendingBlock = null;
    pendingMine = false;
  }
}
