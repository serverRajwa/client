package dev.dexter.kryptionians.modules;

import dev.dexter.kryptionians.KryptioniansAddon;
import dev.dexter.kryptionians.modules.autotool.HumanBehaviorEngine;
import dev.dexter.kryptionians.modules.autotool.HumanizedSwitcher;
import dev.dexter.kryptionians.modules.autotool.ToolSelector;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.player.AutoTool;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class HumanAutoTool extends Module {
  private static final boolean ON_MINE = true;
  private static final boolean ON_USE = false;
  private static final boolean SWITCH_BACK = false;
  private static final boolean SHUFFLE_SCAN = true;
  private static final int DELAY_MIN_MS = 55;
  private static final int DELAY_MAX_MS = 125;
  private static final int USE_DELAY_BONUS_MS = 18;
  private static final int HOLD_MIN_MS = 30;
  private static final int HOLD_MAX_MS = 75;
  private static final int SWITCH_BACK_MIN_MS = 220;
  private static final int SWITCH_BACK_MAX_MS = 480;
  private static final int COOLDOWN_MIN_MS = 95;
  private static final int COOLDOWN_MAX_MS = 220;
  private static final int MANUAL_GRACE_MS = 450;
  private static final double SKIP_CHANCE = 0.03;
  private static final double SWITCH_BACK_SKIP = 0.17;
  private static final double SCORE_MARGIN = 0.08;
  private static final double GOOD_ENOUGH = 0.96;
  private static final double NEAR_TIE_CHANCE = 0.12;
  private static final double NEAR_TIE_WINDOW = 0.08;
  private static final int STABLE_TICKS = 1;
  private static final int MAX_SWITCHES = 10;
  private static final int WINDOW_MS = 4000;
  private static final AutoTool.EnchantPreference PREFER = AutoTool.EnchantPreference.Fortune;
  private static final boolean SILK_TOUCH_ENDER_CHEST = true;
  private static final boolean FORTUNE_ORES = false;
  private final HumanBehaviorEngine engine = new HumanBehaviorEngine();
  private final HumanizedSwitcher switcher = new HumanizedSwitcher(this.engine);
  private BlockPos pendingBlock = null;
  private Block pendingBlockType = null;
  private int stableCount = 0;
  private int stableRequired = 1;
  private boolean pendingMining;
  private boolean switchScheduled;
  private long holdStartMs = 0L;
  private int requiredHoldMs = 40;
  private int lastBestSlot = -1;

  public HumanAutoTool() {
    super(KryptioniansAddon.CATEGORY, "auto-tool+", "Humanized Undetected Auto Tool");
  }

  public void onActivate() {
    this.engine.beginSession();
    this.syncEngineSettings();
    this.resetPending();
    this.switcher.clear();
    this.holdStartMs = 0L;
    this.lastBestSlot = -1;
  }

  public void onDeactivate() {
    this.resetPending();
    this.switcher.clear();
    this.holdStartMs = 0L;
    this.lastBestSlot = -1;
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    long heldMs;
    boolean targetChanged;
    boolean holding;
    if (this.mc.player == null || this.mc.level == null) {
      return;
    }
    if (this.mc.screen != null) {
      return;
    }
    this.syncEngineSettings();
    boolean attackHeld = this.mc.options.keyAttack.isDown();
    boolean useHeld = false;
    boolean bl = holding = attackHeld || useHeld;
    if (!holding) {
      if (this.holdStartMs != 0L) {
        this.resetPending();
        this.holdStartMs = 0L;
      }
      this.switcher.tick();
      return;
    }
    if (this.holdStartMs == 0L) {
      this.holdStartMs = System.currentTimeMillis();
      this.requiredHoldMs = this.engine.requiredHoldMs();
    }
    boolean mining = attackHeld;
    BlockPos targeted = this.getTargetedBlock();
    if (targeted == null) {
      this.resetPending();
      this.switcher.tick();
      return;
    }
    BlockState state = this.mc.level.getBlockState(targeted);
    Block type = state.getBlock();
    boolean bl2 =
        targetChanged =
            this.pendingBlock == null
                || !this.pendingBlock.equals((Object) targeted)
                || this.pendingMining != mining
                || this.pendingBlockType != type;
    if (targetChanged) {
      this.beginPending(targeted, type, mining);
      if (this.switcher.hasPendingSwitch()) {
        this.switcher.cancelPending();
      }
      if (System.currentTimeMillis() - this.holdStartMs >= (long) this.requiredHoldMs) {
        this.stableCount = this.stableRequired;
      }
    } else {
      ++this.stableCount;
    }
    this.switcher.tick();
    if (!this.switchScheduled
        && !this.switcher.hasPendingSwitch()
        && (heldMs = System.currentTimeMillis() - this.holdStartMs) >= (long) this.requiredHoldMs
        && this.stableCount >= this.stableRequired) {
      this.considerSwitch(targeted, mining);
    }
  }

  @EventHandler(priority = -100)
  private void onStartBreaking(StartBreakingBlockEvent event) {
    if (this.mc.player == null || this.mc.level == null) {
      return;
    }
    if (this.mc.player.isSpectator() || this.mc.screen != null) {
      return;
    }
    BlockState state = this.mc.level.getBlockState(event.blockPos);
    if (!BlockUtils.canBreak((BlockPos) event.blockPos, (BlockState) state)) {
      return;
    }
    if (this.holdStartMs == 0L) {
      this.holdStartMs = System.currentTimeMillis();
      this.requiredHoldMs = this.engine.requiredHoldMs();
    }
    this.beginPending(event.blockPos, state.getBlock(), true);
  }

  @EventHandler(priority = -100)
  private void onInteractBlock(InteractBlockEvent event) {}

  private void beginPending(BlockPos pos, Block type, boolean mining) {
    this.pendingBlock = pos.immutable();
    this.pendingBlockType = type;
    this.stableCount = 0;
    this.stableRequired = this.engine.stableTicksRequired();
    this.pendingMining = mining;
    this.switchScheduled = false;
  }

  private void considerSwitch(BlockPos pos, boolean mining) {
    if (this.mc.player == null || this.mc.level == null || this.mc.screen != null) {
      return;
    }
    if (this.switcher.hasPendingSwitch()) {
      return;
    }
    BlockState state = this.mc.level.getBlockState(pos);
    ToolSelector.SlotChoice choice =
        ToolSelector.findBestChoice(
            (Player) this.mc.player, state, true, false, PREFER, true, this.engine);
    if (choice.slot() == -1) {
      this.switchScheduled = true;
      return;
    }
    double currentScore =
        ToolSelector.scoreHeldItem((Player) this.mc.player, state, true, false, PREFER);
    int selected = this.mc.player.getInventory().getSelectedSlot();
    if (choice.slot() == selected) {
      this.lastBestSlot = choice.slot();
      this.switchScheduled = true;
      return;
    }
    if (this.engine.isGoodEnough(currentScore, choice.score())) {
      this.switchScheduled = true;
      return;
    }
    if (!this.engine.isWorthSwitch(currentScore, choice.score())) {
      this.switchScheduled = true;
      return;
    }
    if (choice.slot() == this.lastBestSlot && selected == this.lastBestSlot) {
      this.switchScheduled = true;
      return;
    }
    this.switcher.queueSwitch(choice.slot(), pos, mining, this.holdStartMs);
    this.lastBestSlot = choice.slot();
    this.switchScheduled = true;
  }

  private void syncEngineSettings() {
    this.engine.applySettings(
        false, 55, 125, 18, 220, 480, 95, 220, 0.03, 0.17, 0.08, 0.96, 0.12, 0.08, 10, 4000, 1, 450,
        30, 75);
  }

  private BlockPos getTargetedBlock() {
    HitResult hit = this.mc.hitResult;
    if (hit instanceof BlockHitResult) {
      BlockHitResult blockHit = (BlockHitResult) hit;
      if (hit.getType() == HitResult.Type.BLOCK) {
        return blockHit.getBlockPos();
      }
    }
    return null;
  }

  private void resetPending() {
    this.pendingBlock = null;
    this.pendingBlockType = null;
    this.stableCount = 0;
    this.stableRequired = 1;
    this.pendingMining = false;
    this.switchScheduled = false;
    this.requiredHoldMs = 40;
    this.lastBestSlot = -1;
  }
}
