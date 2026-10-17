package com.slova.autoorder.modules;

import dev.dexter.kryptionians.KryptioniansAddon;
import java.util.Locale;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoOrder extends Module {
  private static final Set<String> STAFF_LIST =
      Set.of(
          "archivepedro",
          "frwost",
          "w1zox_",
          "fluffymaster07",
          "bautiedgar",
          "showered",
          "pastagamer08",
          "itszdeath",
          "0gsummer",
          "aepmiro");
  private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
  private final SettingGroup sgDetection = this.settings.createGroup("Detection");
  private final SettingGroup sgOrder = this.settings.createGroup("Order");
  private final Setting<String> orderIgn =
      this.sgGeneral.add(
          new StringSetting.Builder()
              .name("order-ign")
              .description("The IGN used in /order. Leave empty to use the detected staff member.")
              .defaultValue("")
              .build());
  private final Setting<Item> item =
      this.sgGeneral.add(
          new ItemSetting.Builder()
              .name("item")
              .description("The item that must be in your inventory to order.")
              .defaultValue(Items.AIR)
              .build());
  private final Setting<Integer> minCount =
      this.sgGeneral.add(
          new IntSetting.Builder()
              .name("min-count")
              .description("Minimum amount of the selected item required in your inventory.")
              .defaultValue(1)
              .min(1)
              .sliderMax(64)
              .build());
  private final Setting<Integer> delay =
      this.sgOrder.add(
          new IntSetting.Builder()
              .name("delay")
              .description("Delay between orders in ticks.")
              .defaultValue(40)
              .min(0)
              .sliderMax(200)
              .build());
  private final Setting<Integer> range =
      this.sgDetection.add(
          new IntSetting.Builder()
              .name("range")
              .description("How close a staff member must be before ordering.")
              .defaultValue(64)
              .min(1)
              .sliderMax(128)
              .build());
  private final Setting<Boolean> swapHotbar =
      this.sgOrder.add(
          new BoolSetting.Builder()
              .name("swap-hotbar")
              .description("Swap to a hotbar slot containing the selected item before ordering.")
              .defaultValue(true)
              .build());
  private final Setting<Boolean> autoDeliver =
      this.sgOrder.add(
          new BoolSetting.Builder()
              .name("auto-deliver")
              .description("Automatically complete the order GUIs.")
              .defaultValue(true)
              .build());
  private final Setting<Integer> guiDelay =
      this.sgOrder.add(
          new IntSetting.Builder()
              .name("gui-delay")
              .description("Ticks to wait between GUI actions.")
              .defaultValue(4)
              .min(0)
              .sliderMax(40)
              .visible(this.autoDeliver::get)
              .build());
  private final Setting<Boolean> notify =
      this.sgGeneral.add(
          new BoolSetting.Builder()
              .name("notify")
              .description("Send a chat notification when an order is sent.")
              .defaultValue(true)
              .build());
  private int cooldown = 0;
  private int actionTicks = 0;
  private int stageTicks = 0;
  private AutoOrder.OrderStage orderStage = AutoOrder.OrderStage.IDLE;
  private String detectedStaff = "";

  public AutoOrder() {
    super(
        KryptioniansAddon.CATEGORY,
        "auto-order",
        "Automatically runs /order when a staff member is nearby.");
  }

  public void onActivate() {
    this.resetState();
  }

  public void onDeactivate() {
    if (this.mc.player != null && this.mc.screen != null) {
      this.mc.player.closeContainer();
    }

    this.resetState();
  }

  private void resetState() {
    this.cooldown = 0;
    this.actionTicks = 0;
    this.stageTicks = 0;
    this.orderStage = AutoOrder.OrderStage.IDLE;
    this.detectedStaff = "";
  }

  @EventHandler
  private void onTick(Pre event) {
    if (this.mc.player != null && this.mc.level != null) {
      if (this.orderStage != AutoOrder.OrderStage.IDLE
          && this.orderStage != AutoOrder.OrderStage.DONE) {
        this.handleOrderFlow();
      } else if (this.cooldown > 0) {
        this.cooldown--;
      } else {
        Item selectedItem = this.getSelectedItem();
        if (selectedItem != Items.AIR) {
          if (this.countItem(selectedItem) >= this.minCount.get()) {
            String detected = this.findNearbyStaff();
            if (detected != null) {
              this.detectedStaff = detected;
              if (this.swapHotbar.get()) {
                this.swapToItem(selectedItem);
              }

              String recipient =
                  ((String) this.orderIgn.get()).isBlank()
                      ? this.detectedStaff
                      : ((String) this.orderIgn.get()).trim();
              ChatUtils.sendPlayerMsg("/order " + recipient);
              if (this.notify.get()) {
                this.info(
                    "Staff detected (%s). Ordering %s to %s.",
                    new Object[] {
                      this.detectedStaff, selectedItem.getName().getString(), recipient
                    });
              }

              this.cooldown = this.delay.get();
              if (this.autoDeliver.get()) {
                this.orderStage = AutoOrder.OrderStage.WAIT_GUI;
                this.actionTicks = this.guiDelay.get();
                this.stageTicks = 0;
              }
            }
          }
        }
      }
    }
  }

  private void handleOrderFlow() {
    if (this.actionTicks > 0) {
      this.actionTicks--;
    } else {
      Item targetItem = this.getSelectedItem();
      if (targetItem == Items.AIR) {
        this.resetState();
      } else {
        this.stageTicks++;
        if (this.stageTicks > 400) {
          if (this.notify.get()) {
            this.warning("Order GUI timed out.");
          }

          this.closeScreen();
          this.resetState();
        } else if (this.mc.screen instanceof AbstractContainerScreen<?> screen) {
          AbstractContainerMenu menu = screen.getMenu();
          String title = this.stripText(screen.getTitle().getString());
          switch (this.orderStage) {
            case WAIT_GUI:
              if (this.isConfirmScreen(title)) {
                this.orderStage = AutoOrder.OrderStage.CONFIRM;
              } else if (this.isDeliverItemsScreen(title)) {
                this.orderStage = AutoOrder.OrderStage.SHIFT_TO_DELIVER;
              } else {
                this.orderStage = AutoOrder.OrderStage.SELECT_ITEM;
              }

              this.actionTicks = this.guiDelay.get();
              break;
            case SELECT_ITEM:
              if (this.isConfirmScreen(title)) {
                this.orderStage = AutoOrder.OrderStage.CONFIRM;
                this.actionTicks = this.guiDelay.get();
                return;
              }

              if (this.isDeliverItemsScreen(title)) {
                this.orderStage = AutoOrder.OrderStage.SHIFT_TO_DELIVER;
                this.actionTicks = this.guiDelay.get();
                return;
              }

              Slot itemSlot = this.findSlotWithItem(menu, targetItem, true);
              if (itemSlot != null) {
                this.click(menu, itemSlot.index, ClickType.PICKUP);
                this.orderStage = AutoOrder.OrderStage.WAIT_GUI;
                this.actionTicks = this.guiDelay.get();
              } else {
                this.actionTicks = 2;
              }
              break;
            case SHIFT_TO_DELIVER:
              if (this.isConfirmScreen(title)) {
                this.orderStage = AutoOrder.OrderStage.CONFIRM;
                this.actionTicks = this.guiDelay.get();
                return;
              }

              if (!this.isDeliverItemsScreen(title)) {
                this.orderStage = AutoOrder.OrderStage.CONFIRM;
                this.actionTicks = this.guiDelay.get();
                return;
              }

              if (this.isItemInContainer(menu, targetItem)) {
                this.orderStage = AutoOrder.OrderStage.CLOSE_DELIVER;
                this.actionTicks = this.guiDelay.get();
                return;
              }

              ItemStack cursor = menu.getCarried();
              if (!cursor.isEmpty() && cursor.getItem() == targetItem) {
                Slot empty = this.findEmptyContainerSlot(menu);
                if (empty != null) {
                  this.click(menu, empty.index, ClickType.PICKUP);
                  this.actionTicks = this.guiDelay.get();
                }

                return;
              }

              Slot playerSlot = this.findPlayerSlotWithItem(menu, targetItem);
              if (playerSlot != null) {
                this.click(menu, playerSlot.index, ClickType.QUICK_MOVE);
                this.actionTicks = this.guiDelay.get();
              } else {
                this.actionTicks = 2;
              }
              break;
            case CLOSE_DELIVER:
              this.closeScreen();
              this.orderStage = AutoOrder.OrderStage.CONFIRM;
              this.actionTicks = this.guiDelay.get();
              break;
            case CONFIRM:
              if (this.isDeliverItemsScreen(title)) {
                this.orderStage = AutoOrder.OrderStage.SHIFT_TO_DELIVER;
                this.actionTicks = this.guiDelay.get();
                return;
              }

              if (!this.isConfirmScreen(title)) {
                this.actionTicks = 2;
                return;
              }

              Slot confirm = this.findLimeConfirmSlot(menu);
              if (confirm != null) {
                this.click(menu, confirm.index, ClickType.PICKUP);
                this.orderStage = AutoOrder.OrderStage.DONE;
                this.actionTicks = this.guiDelay.get();
              } else {
                this.actionTicks = 2;
              }
              break;
            case DONE:
              this.resetState();
          }
        } else {
          if (this.orderStage == AutoOrder.OrderStage.CLOSE_DELIVER) {
            this.orderStage = AutoOrder.OrderStage.CONFIRM;
            this.actionTicks = this.guiDelay.get();
          } else if (this.orderStage != AutoOrder.OrderStage.WAIT_GUI
              && this.orderStage != AutoOrder.OrderStage.SELECT_ITEM) {
            this.actionTicks = 2;
          } else {
            this.actionTicks = 2;
          }
        }
      }
    }
  }

  private void click(AbstractContainerMenu handler, int slotId, ClickType type) {
    if (this.mc.gameMode != null && this.mc.player != null) {
      this.mc.gameMode.handleInventoryMouseClick(
          handler.containerId, slotId, 0, type, this.mc.player);
    }
  }

  private void closeScreen() {
    if (this.mc.player != null) {
      this.mc.player.closeContainer();
    }
  }

  private String stripText(String text) {
    return text.replaceAll("\u00a7.", "").toLowerCase(Locale.ROOT);
  }

  private int getContainerSlotCount(AbstractContainerMenu handler) {
    return Math.max(0, handler.slots.size() - 36);
  }

  private boolean isContainerSlot(AbstractContainerMenu handler, Slot slot) {
    return slot.index < this.getContainerSlotCount(handler);
  }

  private boolean isItemInContainer(AbstractContainerMenu handler, Item targetItem) {
    for (Slot slot : handler.slots) {
      if (this.isContainerSlot(handler, slot)) {
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty() && stack.getItem() == targetItem) {
          return true;
        }
      }
    }

    return false;
  }

  private Slot findEmptyContainerSlot(AbstractContainerMenu handler) {
    for (Slot slot : handler.slots) {
      if (this.isContainerSlot(handler, slot) && slot.getItem().isEmpty()) {
        return slot;
      }
    }

    return null;
  }

  private Slot findPlayerSlotWithItem(AbstractContainerMenu handler, Item targetItem) {
    for (Slot slot : handler.slots) {
      if (!this.isContainerSlot(handler, slot)) {
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty() && stack.getItem() == targetItem) {
          return slot;
        }
      }
    }

    return null;
  }

  private Slot findSlotWithItem(
      AbstractContainerMenu handler, Item targetItem, boolean containerOnly) {
    for (Slot slot : handler.slots) {
      if (!containerOnly || this.isContainerSlot(handler, slot)) {
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty() && stack.getItem() == targetItem) {
          return slot;
        }
      }
    }

    return null;
  }

  private boolean isDeliverItemsScreen(String title) {
    return title.contains("deliver") && !title.contains("confirm");
  }

  private boolean isConfirmScreen(String title) {
    return title.contains("confirm");
  }

  private Slot findLimeConfirmSlot(AbstractContainerMenu handler) {
    for (Slot slot : handler.slots) {
      if (this.isContainerSlot(handler, slot)) {
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty() && stack.getItem() == Items.LIME_STAINED_GLASS_PANE) {
          return slot;
        }
      }
    }

    for (Slot slotx : handler.slots) {
      if (this.isContainerSlot(handler, slotx)) {
        ItemStack stack = slotx.getItem();
        if (!stack.isEmpty()) {
          if (stack.getItem() == Items.GREEN_STAINED_GLASS_PANE) {
            return slotx;
          }

          String name = this.stripText(stack.getHoverName().getString());
          if (name.contains("confirm") || name.contains("accept") || name.contains("yes")) {
            return slotx;
          }
        }
      }
    }

    return null;
  }

  private boolean isStaff(String name) {
    return STAFF_LIST.contains(name.toLowerCase(Locale.ROOT));
  }

  private String findNearbyStaff() {
    for (Player player : this.mc.level.players()) {
      if (player != this.mc.player) {
        String name = player.getName().getString();
        if (this.isStaff(name) && this.mc.player.distanceTo(player) <= (float) this.range.get()) {
          return name;
        }
      }
    }

    return null;
  }

  private Item getSelectedItem() {
    Item selected = (Item) this.item.get();
    return selected == null ? Items.AIR : selected;
  }

  private int countItem(Item targetItem) {
    int count = 0;

    for (int i = 0; i < this.mc.player.getInventory().getContainerSize(); i++) {
      ItemStack stack = this.mc.player.getInventory().getItem(i);
      if (!stack.isEmpty() && stack.getItem() == targetItem) {
        count += stack.getCount();
      }
    }

    return count;
  }

  private void swapToItem(Item targetItem) {
    for (int i = 0; i < 9; i++) {
      ItemStack stack = this.mc.player.getInventory().getItem(i);
      if (!stack.isEmpty() && stack.getItem() == targetItem) {
        InvUtils.swap(i, false);
        return;
      }
    }

    for (int ix = 9; ix < this.mc.player.getInventory().getContainerSize(); ix++) {
      ItemStack stack = this.mc.player.getInventory().getItem(ix);
      if (!stack.isEmpty() && stack.getItem() == targetItem) {
        InvUtils.move().from(ix).toHotbar(0);
        InvUtils.swap(0, false);
        return;
      }
    }
  }

  private static enum OrderStage {
    IDLE,
    WAIT_GUI,
    SELECT_ITEM,
    SHIFT_TO_DELIVER,
    CLOSE_DELIVER,
    CONFIRM,
    DONE;
  }
}
