package dev.dexter.kryptionians.modules.autotool;

import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.systems.modules.player.AutoTool;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ToolSelector {
  public record SlotChoice(int slot, double score) {}

  private ToolSelector() {}

  public static SlotChoice findBestChoice(
      Player player,
      BlockState state,
      boolean silkTouchEnderChest,
      boolean fortuneOres,
      AutoTool.EnchantPreference prefer,
      boolean shuffleScan,
      HumanBehaviorEngine engine) {
    double bestScore = -1;
    int bestSlot = -1;
    double secondScore = -1;
    int secondSlot = -1;

    int[] order = hotbarOrder(shuffleScan);
    for (int slot : order) {
      ItemStack stack = player.getInventory().getItem(slot);
      double score = score(stack, state, silkTouchEnderChest, fortuneOres, prefer);
      if (score < 0) continue;

      if (score > bestScore) {
        secondScore = bestScore;
        secondSlot = bestSlot;
        bestScore = score;
        bestSlot = slot;
      } else if (score > secondScore) {
        secondScore = score;
        secondSlot = slot;
      }
    }

    if (bestSlot == -1) return new SlotChoice(-1, -1);

    if (secondSlot != -1 && engine.pickNearTie(bestScore, secondScore)) {
      return new SlotChoice(secondSlot, secondScore);
    }

    return new SlotChoice(bestSlot, bestScore);
  }

  public static double scoreHeldItem(
      Player player,
      BlockState state,
      boolean silkTouchEnderChest,
      boolean fortuneOres,
      AutoTool.EnchantPreference prefer) {
    return score(player.getMainHandItem(), state, silkTouchEnderChest, fortuneOres, prefer);
  }

  private static double score(
      ItemStack stack,
      BlockState state,
      boolean silkTouchEnderChest,
      boolean fortuneOres,
      AutoTool.EnchantPreference prefer) {
    return AutoTool.getScore(
        stack, state, silkTouchEnderChest, fortuneOres, prefer, itemStack -> true);
  }

  private static int[] hotbarOrder(boolean shuffle) {
    int[] order = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    if (!shuffle) return order;

    ThreadLocalRandom rng = ThreadLocalRandom.current();
    for (int i = order.length - 1; i > 0; i--) {
      int j = rng.nextInt(i + 1);
      int tmp = order[i];
      order[i] = order[j];
      order[j] = tmp;
    }
    return order;
  }
}
