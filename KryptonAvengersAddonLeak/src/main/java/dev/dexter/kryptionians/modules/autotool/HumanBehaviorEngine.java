package dev.dexter.kryptionians.modules.autotool;

import java.util.concurrent.ThreadLocalRandom;

public final class HumanBehaviorEngine {
  private static final int ABSOLUTE_MIN_REACTION_MS = 45;
  private static final int ABSOLUTE_MIN_HOLD_MS = 25;
  private final ThreadLocalRandom rng = ThreadLocalRandom.current();
  private double delayMultiplier = 1.0;
  private double cooldownMultiplier = 1.0;
  private double extraSkipBias = 0.0;
  private long lastSwitchMs = 0L;
  private long windowStartMs = 0L;
  private int switchesInWindow = 0;
  private int sessionSwitches = 0;
  private int lastObservedSlot = -1;
  private long lastManualSlotMs = 0L;
  private boolean causedLastSwitch = false;
  private boolean switchBackEnabled = true;
  private int delayMinMs = 55;
  private int delayMaxMs = 125;
  private int useDelayBonusMs = 18;
  private int switchBackMinMs = 220;
  private int switchBackMaxMs = 480;
  private int cooldownMinMs = 95;
  private int cooldownMaxMs = 220;
  private double skipChance = 0.03;
  private double switchBackSkipChance = 0.17;
  private double scoreMargin = 0.08;
  private double goodEnoughRatio = 0.96;
  private double nearTieChance = 0.12;
  private double nearTieWindow = 0.08;
  private int maxSwitchesPerWindow = 10;
  private int windowMs = 4000;
  private int stableTicksRequired = 1;
  private int manualGraceMs = 450;
  private int holdMinMs = 30;
  private int holdMaxMs = 75;

  public void beginSession() {
    this.delayMultiplier = 0.92 + this.rng.nextDouble() * 0.22;
    this.cooldownMultiplier = 0.92 + this.rng.nextDouble() * 0.22;
    this.extraSkipBias = this.rng.nextDouble() * 0.015;
    this.lastSwitchMs = 0L;
    this.windowStartMs = 0L;
    this.switchesInWindow = 0;
    this.sessionSwitches = 0;
    this.lastObservedSlot = -1;
    this.lastManualSlotMs = 0L;
    this.causedLastSwitch = false;
  }

  public void applySettings(
      boolean switchBackEnabled,
      int delayMinMs,
      int delayMaxMs,
      int useDelayBonusMs,
      int switchBackMinMs,
      int switchBackMaxMs,
      int cooldownMinMs,
      int cooldownMaxMs,
      double skipChance,
      double switchBackSkipChance,
      double scoreMargin,
      double goodEnoughRatio,
      double nearTieChance,
      double nearTieWindow,
      int maxSwitchesPerWindow,
      int windowMs,
      int stableTicksRequired,
      int manualGraceMs,
      int holdMinMs,
      int holdMaxMs) {
    this.switchBackEnabled = switchBackEnabled;
    this.delayMinMs = delayMinMs;
    this.delayMaxMs = Math.max(delayMinMs, delayMaxMs);
    this.useDelayBonusMs = useDelayBonusMs;
    this.switchBackMinMs = switchBackMinMs;
    this.switchBackMaxMs = Math.max(switchBackMinMs, switchBackMaxMs);
    this.cooldownMinMs = cooldownMinMs;
    this.cooldownMaxMs = Math.max(cooldownMinMs, cooldownMaxMs);
    this.skipChance = skipChance;
    this.switchBackSkipChance = switchBackSkipChance;
    this.scoreMargin = scoreMargin;
    this.goodEnoughRatio = goodEnoughRatio;
    this.nearTieChance = nearTieChance;
    this.nearTieWindow = nearTieWindow;
    this.maxSwitchesPerWindow = maxSwitchesPerWindow;
    this.windowMs = windowMs;
    this.stableTicksRequired = stableTicksRequired;
    this.manualGraceMs = manualGraceMs;
    this.holdMinMs = holdMinMs;
    this.holdMaxMs = Math.max(holdMinMs, holdMaxMs);
  }

  public int stableTicksRequired() {
    return Math.max(1, this.stableTicksRequired);
  }

  public int requiredHoldMs() {
    return (int) Math.max(25L, this.jitter(this.holdMinMs, this.holdMaxMs));
  }

  public boolean switchBackEnabled() {
    return this.switchBackEnabled;
  }

  public boolean shouldSkipSwitch() {
    return this.rng.nextDouble() < this.skipChance + this.extraSkipBias;
  }

  public boolean shouldSkipSwitchBack() {
    return this.rng.nextDouble() < this.switchBackSkipChance;
  }

  public boolean isGoodEnough(double currentScore, double bestScore) {
    if (bestScore <= 0.0) {
      return true;
    }
    if (currentScore <= 0.0) {
      return false;
    }
    return currentScore >= bestScore * this.goodEnoughRatio;
  }

  public boolean isWorthSwitch(double currentScore, double bestScore) {
    if (bestScore <= 0.0) {
      return false;
    }
    if (currentScore <= 0.0) {
      return true;
    }
    return bestScore >= currentScore * (1.0 + this.scoreMargin);
  }

  public boolean pickNearTie(double bestScore, double secondScore) {
    if (secondScore <= 0.0 || bestScore <= 0.0) {
      return false;
    }
    if (secondScore < bestScore * (1.0 - this.nearTieWindow)) {
      return false;
    }
    return this.rng.nextDouble() < this.nearTieChance;
  }

  public long nextSwitchDelayMs(boolean rightClick) {
    long base = this.jitter(this.delayMinMs, this.delayMaxMs);
    if (rightClick && this.useDelayBonusMs > 0) {
      int bonus = Math.max(1, this.useDelayBonusMs);
      base += (long) this.rng.nextInt(Math.max(1, bonus / 2), bonus + 1);
    }
    base = Math.round((double) base * this.delayMultiplier);
    return Math.max(45L, base += this.fatigueBonusMs());
  }

  public long nextSwitchBackDelayMs() {
    long base = this.jitter(this.switchBackMinMs, this.switchBackMaxMs);
    base = Math.round((double) base * this.delayMultiplier);
    return Math.max(45L, base);
  }

  public boolean canSwitchNow() {
    long now = System.currentTimeMillis();
    if (this.windowStartMs == 0L || now - this.windowStartMs > (long) this.windowMs) {
      this.windowStartMs = now;
      this.switchesInWindow = 0;
    }
    if (this.switchesInWindow >= this.maxSwitchesPerWindow) {
      return false;
    }
    if (this.lastManualSlotMs > 0L && now - this.lastManualSlotMs < (long) this.manualGraceMs) {
      return false;
    }
    if (this.lastSwitchMs == 0L) {
      return true;
    }
    long cooldown =
        Math.round(
            (double) this.jitter(this.cooldownMinMs, this.cooldownMaxMs) * this.cooldownMultiplier);
    return now - this.lastSwitchMs >= (cooldown += this.fatigueBonusMs() / 2L);
  }

  public void recordSwitch() {
    this.lastSwitchMs = System.currentTimeMillis();
    ++this.switchesInWindow;
    ++this.sessionSwitches;
    this.causedLastSwitch = true;
  }

  public void observeSelectedSlot(int slot) {
    if (this.lastObservedSlot == -1) {
      this.lastObservedSlot = slot;
      return;
    }
    if (slot != this.lastObservedSlot) {
      if (!this.causedLastSwitch) {
        this.lastManualSlotMs = System.currentTimeMillis();
      }
      this.causedLastSwitch = false;
      this.lastObservedSlot = slot;
    } else if (this.causedLastSwitch) {
      this.causedLastSwitch = false;
    }
  }

  private long fatigueBonusMs() {
    if (this.sessionSwitches < 14) {
      return 0L;
    }
    return Math.min(60L, (long) (this.sessionSwitches - 13) * 2L);
  }

  private long jitter(int min, int max) {
    if (min >= max) {
      return min + this.rng.nextInt(0, 12);
    }
    double mean = (double) (min + max) / 2.0;
    double spread = (double) (max - min) / 3.5;
    long value = Math.round(mean + this.rng.nextGaussian() * spread);
    return Math.max((long) min, Math.min((long) max, value));
  }
}
