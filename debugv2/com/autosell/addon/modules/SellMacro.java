package com.autosell.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
public class SellMacro extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTiming = this.settings.createGroup("Timing");
   private final SettingGroup sgAntiCheat = this.settings.createGroup("Anti-Cheat");
   private final SettingGroup sgDisplay = this.settings.createGroup("Display");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<String> menuTitle = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("menu-title")).description("Expected title of the sell GUI. Case-insensitive.")).defaultValue("Sell"))
            .build()
      );
   private final Setting<Integer> confirmSlotIndex = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("confirm-slot-index"))
                  .description("Slot index of the green glass confirm button. -1 auto-detects green glass."))
               .defaultValue(-1))
            .range(-1, 53)
            .sliderRange(-1, 53)
            .build()
      );
   private final Setting<Boolean> onlyWhenInventoryHasItems = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("only-when-has-items"))
                  .description("Only run the sell cycle if your inventory has items."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> maxSellRetries = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("max-sell-retries"))
                  .description("Max times to reopen /sell when the sell button is not found."))
               .defaultValue(3))
            .range(1, 10)
            .sliderRange(1, 5)
            .build()
      );
   private final Setting<Integer> minCycleDelay = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("min-cycle-delay"))
                  .description("Minimum ticks between sell cycles."))
               .defaultValue(1))
            .range(0, 600)
            .sliderRange(0, 60)
            .build()
      );
   private final Setting<Integer> maxCycleDelay = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("max-cycle-delay"))
                  .description("Maximum ticks between sell cycles."))
               .defaultValue(3))
            .range(0, 1200)
            .sliderRange(0, 60)
            .build()
      );
   private final Setting<Integer> minClickDelay = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("min-click-delay"))
                  .description("Minimum ticks between slot clicks."))
               .defaultValue(1))
            .range(0, 20)
            .sliderRange(0, 10)
            .build()
      );
   private final Setting<Integer> maxClickDelay = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("max-click-delay"))
                  .description("Maximum ticks between slot clicks."))
               .defaultValue(3))
            .range(0, 40)
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Integer> menuOpenWait = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("menu-open-wait"))
                  .description("Ticks to wait after sending /sell."))
               .defaultValue(3))
            .range(1, 60)
            .sliderRange(1, 20)
            .build()
      );
   private final Setting<Integer> postFillWait = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("post-fill-wait"))
                  .description("Ticks to wait after moving items before clicking sell."))
               .defaultValue(1))
            .range(0, 40)
            .sliderRange(0, 10)
            .build()
      );
   private final Setting<Integer> postSellWait = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("post-sell-wait"))
                  .description("Ticks to wait after clicking sell."))
               .defaultValue(1))
            .range(0, 40)
            .sliderRange(0, 10)
            .build()
      );
   private final Setting<Boolean> randomizeDelays = this.sgAntiCheat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("randomize-delays"))
                  .description("Add random jitter to delays."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> jitterRange;
   private final Setting<Boolean> skipRandomCycles = this.sgAntiCheat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("skip-random-cycles"))
                  .description("Occasionally skip a sell cycle."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> skipChance;
   private final Setting<Boolean> closeMenuAfterSell = this.sgAntiCheat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("close-menu-after-sell"))
                  .description("Close the sell menu after each cycle."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> showMessages = this.sgDisplay
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("show-messages"))
                  .description("Show status messages in chat."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showStats = this.sgDisplay
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("show-stats"))
                  .description("Show sell cycle statistics."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> forceBackgroundTick = this.sgBackground
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("force-background-tick"))
                  .description("Disables pause on lost focus while Sell Macro is active."))
               .defaultValue(true))
            .build()
      );
   private SellMacro.State state;
   private int tickCounter;
   private int currentDelay;
   private final List<Integer> slotsToFill;
   private int sellRetryCount;
   private int totalCycles;
   private int totalItemsSold;
   private long sessionStartTime;
   private boolean originalPauseOnLostFocus;

   public SellMacro() {
      super(AddonTemplate.C, "sell-macro", "Automatically runs /sell macro for supported sell menus.");
      Setting<Boolean> randomizeRef = this.randomizeDelays;
      this.jitterRange = this.sgAntiCheat
         .add(
            ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                           .name("jitter-range"))
                        .description("Max random ticks added or subtracted from delays."))
                     .defaultValue(2))
                  .range(0, 60)
                  .sliderRange(0, 10)
                  .visible(randomizeRef::get))
               .build()
         );
      Setting<Boolean> skipRef = this.skipRandomCycles;
      this.skipChance = this.sgAntiCheat
         .add(
            ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                           .name("skip-chance"))
                        .description("Percentage chance to skip a cycle."))
                     .defaultValue(8))
                  .range(0, 50)
                  .sliderRange(0, 25)
                  .visible(skipRef::get))
               .build()
         );
      this.state = SellMacro.State.IDLE;
      this.currentDelay = 1;
      this.slotsToFill = new ArrayList<>();
      this.originalPauseOnLostFocus = true;
   }

   public void onActivate() {
      this.state = SellMacro.State.IDLE;
      this.tickCounter = 0;
      this.currentDelay = 1;
      this.slotsToFill.clear();
      this.sellRetryCount = 0;
      this.totalCycles = 0;
      this.totalItemsSold = 0;
      this.sessionStartTime = System.currentTimeMillis();
      if ((Boolean)this.forceBackgroundTick.get() && this.mc.options != null) {
         this.originalPauseOnLostFocus = this.mc.options.pauseOnLostFocus;
         this.mc.options.pauseOnLostFocus = false;
      }

      if ((Boolean)this.showMessages.get()) {
         this.sendMsg("§8[§a$§8] §aSell Macro §7activated. First cycle in §e" + this.currentDelay + " §7ticks.");
         if ((Boolean)this.forceBackgroundTick.get()) {
            this.sendMsg("§8[§a$§8] §7Background mode §aenabled§7.");
         }
      }
   }

   public void onDeactivate() {
      if ((Boolean)this.forceBackgroundTick.get() && this.mc.options != null) {
         this.mc.options.pauseOnLostFocus = this.originalPauseOnLostFocus;
      }

      if ((Boolean)this.showMessages.get() && (Boolean)this.showStats.get()) {
         long elapsed = (System.currentTimeMillis() - this.sessionStartTime) / 1000L;
         this.sendMsg(
            "§8[§a$§8] §cSell Macro §7deactivated. §eCycles: §a"
               + this.totalCycles
               + " §8| §eItems: §a"
               + this.totalItemsSold
               + " §8| §eTime: §a"
               + this.formatTime(elapsed)
         );
      }

      this.state = SellMacro.State.IDLE;
      this.tickCounter = 0;
      this.sellRetryCount = 0;
   }

   @EventHandler
   private void on(Post event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.tickCounter++;
         switch (this.state) {
            case IDLE:
               this.handleIdle();
               break;
            case SEND_COMMAND:
               this.handleSendCommand();
               break;
            case WAIT_MENU_OPEN:
               this.handleWaitMenuOpen();
               break;
            case FILLING_ITEMS:
               this.handleFillingItems();
               break;
            case POST_FILL_WAIT:
               this.handlePostFillWait();
               break;
            case CLICK_SELL:
               this.handleClickSell();
               break;
            case POST_SELL_WAIT:
               this.handlePostSellWait();
               break;
            case CLOSE_MENU:
               this.handleCloseMenu();
               break;
            case COOLDOWN:
               this.handleCooldown();
               break;
            case REOPEN_MENU:
               this.handleReopenMenu();
         }
      }
   }

   private void handleIdle() {
      if (this.tickCounter >= this.currentDelay) {
         if ((Boolean)this.skipRandomCycles.get() && ThreadLocalRandom.current().nextInt(100) < (Integer)this.skipChance.get()) {
            if ((Boolean)this.showMessages.get()) {
               this.sendMsg("§8[§a$§8] §7Skipping cycle.");
            }

            this.tickCounter = 0;
            this.currentDelay = this.randomDelay((Integer)this.minCycleDelay.get(), (Integer)this.maxCycleDelay.get());
         } else if (!this.isInSellMenu()) {
            if ((Boolean)this.onlyWhenInventoryHasItems.get() && !this.hasInventoryItems()) {
               this.tickCounter = 0;
               this.currentDelay = this.randomDelay((Integer)this.minCycleDelay.get(), (Integer)this.maxCycleDelay.get());
            } else {
               this.sellRetryCount = 0;
               this.state = SellMacro.State.SEND_COMMAND;
               this.tickCounter = 0;
            }
         } else {
            if (!this.hasInventoryItems() && !this.hasContainerItems()) {
               this.tickCounter = 0;
               this.currentDelay = this.randomDelay((Integer)this.minCycleDelay.get(), (Integer)this.maxCycleDelay.get());
            } else {
               this.state = SellMacro.State.FILLING_ITEMS;
               this.tickCounter = 0;
               this.prepareFillSlots();
            }
         }
      }
   }

   private void handleSendCommand() {
      if (this.mc.player != null) {
         if (this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
            this.tickCounter = 0;
            this.currentDelay = this.jitter(2);
            this.state = SellMacro.State.IDLE;
         } else {
            this.mc.player.networkHandler.sendChatCommand("sell");
            this.state = SellMacro.State.WAIT_MENU_OPEN;
            this.tickCounter = 0;
            this.currentDelay = this.jitter((Integer)this.menuOpenWait.get());
            if ((Boolean)this.showMessages.get()) {
               this.sendMsg("§8[§a$§8] §7Sent §e/sell §7command.");
            }
         }
      }
   }

   private void handleWaitMenuOpen() {
      if (this.isInSellMenu()) {
         this.state = SellMacro.State.FILLING_ITEMS;
         this.tickCounter = 0;
         this.prepareFillSlots();
      } else if (this.tickCounter >= this.currentDelay + 20) {
         if ((Boolean)this.showMessages.get()) {
            this.sendMsg("§8[§a$§8] §cMenu did not open, retrying.");
         }

         this.state = SellMacro.State.IDLE;
         this.tickCounter = 0;
         this.currentDelay = this.jitter(3);
      }
   }

   private void handleFillingItems() {
      if (!this.isInSellMenu()) {
         this.state = SellMacro.State.IDLE;
         this.tickCounter = 0;
         this.currentDelay = this.jitter(2);
      } else {
         GenericContainerScreenHandler handler = this.getContainerHandler();
         if (handler == null) {
            this.state = SellMacro.State.IDLE;
            this.tickCounter = 0;
         } else {
            if (this.slotsToFill.isEmpty()) {
               this.prepareFillSlots();
            }

            if (this.slotsToFill.isEmpty()) {
               this.state = SellMacro.State.POST_FILL_WAIT;
               this.tickCounter = 0;
               this.currentDelay = this.jitter((Integer)this.postFillWait.get());
            } else {
               int moved = 0;

               for (int slotId : this.slotsToFill) {
                  Slot slot = handler.getSlot(slotId);
                  if (slot != null && slot.hasStack() && !slot.getStack().isEmpty()) {
                     this.mc.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                     this.totalItemsSold++;
                     moved++;
                  }
               }

               if ((Boolean)this.showMessages.get() && moved > 0) {
                  this.sendMsg("§8[§a$§8] §7Moved §e" + moved + " §7stacks into sell menu.");
               }

               this.state = SellMacro.State.POST_FILL_WAIT;
               this.tickCounter = 0;
               this.currentDelay = this.jitter((Integer)this.postFillWait.get());
            }
         }
      }
   }

   private void handlePostFillWait() {
      if (this.tickCounter >= this.currentDelay) {
         this.state = SellMacro.State.CLICK_SELL;
         this.tickCounter = 0;
      }
   }

   private void handleClickSell() {
      if (!this.isInSellMenu()) {
         this.state = SellMacro.State.IDLE;
         this.tickCounter = 0;
         this.currentDelay = this.jitter(2);
      } else {
         GenericContainerScreenHandler handler = this.getContainerHandler();
         if (handler == null) {
            this.state = SellMacro.State.IDLE;
            this.tickCounter = 0;
         } else {
            int sellSlot = this.findSellButton(handler);
            if (sellSlot == -1) {
               this.sellRetryCount++;
               if (this.sellRetryCount > (Integer)this.maxSellRetries.get()) {
                  if ((Boolean)this.showMessages.get()) {
                     this.sendMsg("§8[§a$§8] §cSell button not found after §e" + this.maxSellRetries.get() + " §cretries.");
                  }

                  this.sellRetryCount = 0;
                  if (this.mc.player != null && this.mc.currentScreen != null) {
                     this.mc.player.closeHandledScreen();
                  }

                  this.state = SellMacro.State.COOLDOWN;
                  this.tickCounter = 0;
                  this.currentDelay = this.randomDelay((Integer)this.minCycleDelay.get(), (Integer)this.maxCycleDelay.get());
               } else {
                  if ((Boolean)this.showMessages.get()) {
                     this.sendMsg("§8[§a$§8] §eSell button not found, reopening menu. §7Retry §e" + this.sellRetryCount + "§7/§e" + this.maxSellRetries.get());
                  }

                  if (this.mc.player != null && this.mc.currentScreen != null) {
                     this.mc.player.closeHandledScreen();
                  }

                  this.state = SellMacro.State.REOPEN_MENU;
                  this.tickCounter = 0;
                  this.currentDelay = this.jitter(3);
               }
            } else {
               this.sellRetryCount = 0;
               this.mc.interactionManager.clickSlot(handler.syncId, sellSlot, 0, SlotActionType.PICKUP, this.mc.player);
               this.totalCycles++;
               if ((Boolean)this.showMessages.get()) {
                  this.sendMsg("§8[§a$§8] §aSold! §7Cycle §e#" + this.totalCycles);
               }

               this.state = SellMacro.State.POST_SELL_WAIT;
               this.tickCounter = 0;
               this.currentDelay = this.jitter((Integer)this.postSellWait.get());
            }
         }
      }
   }

   private void handlePostSellWait() {
      if (this.tickCounter >= this.currentDelay) {
         if ((Boolean)this.closeMenuAfterSell.get()) {
            this.state = SellMacro.State.CLOSE_MENU;
            this.tickCounter = 0;
         } else {
            if (this.isInSellMenu()) {
               if (!this.hasInventoryItems() && !this.hasContainerItems()) {
                  this.state = SellMacro.State.IDLE;
                  this.tickCounter = 0;
                  this.currentDelay = this.randomDelay((Integer)this.minCycleDelay.get(), (Integer)this.maxCycleDelay.get());
               } else {
                  this.state = SellMacro.State.FILLING_ITEMS;
                  this.tickCounter = 0;
                  this.prepareFillSlots();
               }
            } else {
               this.state = SellMacro.State.IDLE;
               this.tickCounter = 0;
               this.currentDelay = this.jitter(1);
            }
         }
      }
   }

   private void handleCloseMenu() {
      if (this.mc.player != null && this.mc.currentScreen != null) {
         this.mc.player.closeHandledScreen();
      }

      this.state = SellMacro.State.COOLDOWN;
      this.tickCounter = 0;
      this.currentDelay = this.randomDelay((Integer)this.minCycleDelay.get(), (Integer)this.maxCycleDelay.get());
   }

   private void handleCooldown() {
      if (this.tickCounter >= this.currentDelay) {
         this.state = SellMacro.State.IDLE;
         this.tickCounter = 0;
         this.currentDelay = this.randomDelay((Integer)this.minCycleDelay.get(), (Integer)this.maxCycleDelay.get());
      }
   }

   private void handleReopenMenu() {
      if (this.tickCounter >= this.currentDelay) {
         this.state = SellMacro.State.SEND_COMMAND;
         this.tickCounter = 0;
      }
   }

   private boolean isInSellMenu() {
      return !(this.mc.currentScreen instanceof GenericContainerScreen)
         ? false
         : this.mc.currentScreen.getTitle().getString().trim().equalsIgnoreCase(((String)this.menuTitle.get()).trim());
   }

   private GenericContainerScreenHandler getContainerHandler() {
      return this.mc.currentScreen instanceof GenericContainerScreen cs ? (GenericContainerScreenHandler)cs.getScreenHandler() : null;
   }

   private void prepareFillSlots() {
      this.slotsToFill.clear();
      GenericContainerScreenHandler handler = this.getContainerHandler();
      if (handler != null) {
         int containerSlots = handler.getRows() * 9;

         for (int slot2 = containerSlots; slot2 < handler.slots.size(); slot2++) {
            Slot slot = handler.getSlot(slot2);
            if (slot != null && slot.hasStack() && !slot.getStack().isEmpty()) {
               this.slotsToFill.add(slot2);
            }
         }

         if ((Boolean)this.showMessages.get() && !this.slotsToFill.isEmpty()) {
            this.sendMsg("§8[§a$§8] §7Filling §e" + this.slotsToFill.size() + " §7items into sell menu.");
         }
      }
   }

   private int findSellButton(GenericContainerScreenHandler handler) {
      if ((Integer)this.confirmSlotIndex.get() >= 0) {
         return (Integer)this.confirmSlotIndex.get();
      } else {
         int lastGreenGlass = -1;

         for (int i = 0; i < handler.getRows() * 9; i++) {
            Slot slot = handler.getSlot(i);
            if (slot != null && slot.hasStack()) {
               ItemStack stack = slot.getStack();
               if (stack.getItem() == Items.LIME_STAINED_GLASS_PANE || stack.getItem() == Items.GREEN_STAINED_GLASS_PANE) {
                  lastGreenGlass = i;
               }
            }
         }

         return lastGreenGlass;
      }
   }

   private boolean hasContainerItems() {
      GenericContainerScreenHandler handler = this.getContainerHandler();
      if (handler == null) {
         return false;
      } else {
         for (int i = 0; i < handler.getRows() * 9; i++) {
            Slot slot = handler.getSlot(i);
            if (slot != null && slot.hasStack()) {
               ItemStack stack = slot.getStack();
               if (stack.getItem() != Items.LIME_STAINED_GLASS_PANE
                  && stack.getItem() != Items.GREEN_STAINED_GLASS_PANE
                  && stack.getItem() != Items.GRAY_STAINED_GLASS_PANE
                  && stack.getItem() != Items.BLACK_STAINED_GLASS_PANE
                  && stack.getItem() != Items.RED_STAINED_GLASS_PANE
                  && stack.getItem() != Items.AIR) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean hasInventoryItems() {
      if (this.mc.player == null) {
         return false;
      } else {
         for (int i = 0; i < 36; i++) {
            ItemStack hotbarStack = this.mc.player.getInventory().getStack(i);
            if (hotbarStack != null && !hotbarStack.isEmpty()) {
               return true;
            }
         }

         return false;
      }
   }

   private int randomDelay(int min, int max) {
      int low = Math.min(min, max);
      int high = Math.max(min, max);
      int base = low == high ? low : ThreadLocalRandom.current().nextInt(low, high + 1);
      if ((Boolean)this.randomizeDelays.get()) {
         base += ThreadLocalRandom.current().nextInt(-(Integer)this.jitterRange.get(), (Integer)this.jitterRange.get() + 1);
      }

      return Math.max(0, base);
   }

   private int jitter(int base) {
      if (!(Boolean)this.randomizeDelays.get()) {
         return Math.max(0, base);
      } else {
         int h = (Integer)this.jitterRange.get() / 2;
         return Math.max(0, base + ThreadLocalRandom.current().nextInt(-h, h + 1));
      }
   }

   private void sendMsg(String msg) {
      if (this.mc.player != null) {
         ChatUtils.sendMsg(Text.literal(msg));
      }
   }

   private String formatTime(long seconds) {
      long h = seconds / 3600L;
      long m = seconds % 3600L / 60L;
      long s = seconds % 60L;
      if (BlockPosRef > 0L) {
         return h + "h " + m + "m " + s + "s";
      } else {
         return m > 0L ? m + "m " + s + "s" : s + "s";
      }
   }

   private static enum State {
      IDLE,
      SEND_COMMAND,
      WAIT_MENU_OPEN,
      FILLING_ITEMS,
      POST_FILL_WAIT,
      CLICK_SELL,
      POST_SELL_WAIT,
      CLOSE_MENU,
      COOLDOWN,
      REOPEN_MENU;
   }
}
