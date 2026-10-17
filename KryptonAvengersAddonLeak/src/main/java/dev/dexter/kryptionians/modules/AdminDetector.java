package dev.dexter.kryptionians.modules;

import dev.dexter.kryptionians.KryptioniansAddon;
import dev.dexter.kryptionians.ui.AdminHeadTextures;
import dev.dexter.kryptionians.ui.AdminListHud;
import dev.dexter.kryptionians.ui.AdminToastStack;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class AdminDetector extends Module {
  private static final int POLL_TICKS = 10;
  private static final int SCAN_INTERVAL_MS = 90000;
  private static final int SCAN_JITTER_MS = 15000;
  private static final int TRIGGER_Y = -10;
  private static final int Y_DROP_BLOCKS = 15;
  private static final int TRIGGER_COOLDOWN_SEC = 30;
  private static final int JOIN_TICKS = 60;

  private static final Set<String> ADMIN_KEYS =
      Set.of(
          "archivepedro",
          "frwost",
          "w1zox_",
          "fluffymaster07",
          "bautiedgar",
          "showered",
          "pastagamer08",
          "itszdeath",
          "0gsummer");

  private final SettingGroup sgGeneral = settings.getDefaultGroup();
  private final SettingGroup sgHud = settings.createGroup("HUD");

  private final Setting<Boolean> alerts =
      sgGeneral.add(
          new BoolSetting.Builder()
              .name("alerts")
              .description("Toast and sound on join or leave.")
              .defaultValue(true)
              .build());

  private final Setting<Boolean> adminList =
      sgGeneral.add(
          new BoolSetting.Builder()
              .name("admin-list")
              .description("Show the Online Admin List panel.")
              .defaultValue(true)
              .build());

  private final Setting<Integer> hudX =
      sgHud.add(
          new IntSetting.Builder()
              .name("hud-x")
              .description("Panel X position (-1 for default).")
              .defaultValue(-1)
              .min(-1)
              .sliderMax(4000)
              .build());

  private final Setting<Integer> hudY =
      sgHud.add(
          new IntSetting.Builder()
              .name("hud-y")
              .description("Panel Y position (-1 for default).")
              .defaultValue(-1)
              .min(-1)
              .sliderMax(4000)
              .build());

  private final Setting<Integer> listSize =
      sgHud.add(
          new IntSetting.Builder()
              .name("admin-list-size")
              .description("Scale of the Online Admin List panel, heads, and text.")
              .defaultValue(100)
              .range(70, 150)
              .sliderRange(70, 150)
              .build());

  private final Set<String> previousOnline = new HashSet<>();
  private final AdminListHud listHud = new AdminListHud();
  private final AdminToastStack toastStack = new AdminToastStack();
  private long nextTimedScan;
  private long lastTriggerScan;
  private double highestRecentY = -999.0;
  private boolean joinScanned;
  private boolean seeded;
  private int tickCounter;

  public AdminDetector() {
    super(
        KryptioniansAddon.CATEGORY,
        "admin detector",
        "Alerts when a Donut admin joins or leaves tab.");
  }

  @Override
  public void onActivate() {
    resetState();
  }

  @Override
  public void onDeactivate() {
    resetState();
  }

  @Override
  public String getInfoString() {
    if (previousOnline.isEmpty()) return "clear";
    return previousOnline.size() + " online";
  }

  private void resetState() {
    previousOnline.clear();
    listHud.clear();
    toastStack.clear();
    nextTimedScan = System.currentTimeMillis() + 3000L;
    lastTriggerScan = 0L;
    highestRecentY = -999.0;
    joinScanned = false;
    seeded = false;
    tickCounter = 0;
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

    tickCounter++;
    boolean poll = tickCounter % POLL_TICKS == 0;
    if (!poll && !shouldScan()) return;

    Set<String> current = scanTab();
    if (!seeded) {
      previousOnline.clear();
      previousOnline.addAll(current);
      listHud.sync(current, Set.of(), Set.of(), true);
      seeded = true;
      return;
    }

    HashSet<String> joined = new HashSet<>(current);
    joined.removeAll(previousOnline);
    HashSet<String> left = new HashSet<>(previousOnline);
    left.removeAll(current);
    listHud.sync(current, joined, left, false);

    if ((!joined.isEmpty() || !left.isEmpty()) && alerts.get()) {
      for (String key : joined) toastStack.push(key, true, true);
      for (String key : left) toastStack.push(key, false, true);
    }

    previousOnline.clear();
    previousOnline.addAll(current);
  }

  @EventHandler
  private void onRender2D(Render2DEvent event) {
    if (adminList.get()) {
      listHud.handleDrag(hudX, hudY, listSize);
      listHud.render(event.drawContext, hudX, hudY, listSize);
    }
    toastStack.render(event.drawContext);
  }

  private boolean shouldScan() {
    long now = System.currentTimeMillis();
    double y = mc.player.getY();
    if (now >= nextTimedScan) {
      scheduleTimedScan();
      return true;
    }
    if (y <= TRIGGER_Y && triggerReady()) {
      markTrigger();
      return true;
    }
    if (y > highestRecentY) highestRecentY = y;
    if (highestRecentY - y >= Y_DROP_BLOCKS && triggerReady()) {
      markTrigger();
      highestRecentY = y;
      return true;
    }
    if (!joinScanned && mc.player.tickCount > JOIN_TICKS) {
      joinScanned = true;
      return true;
    }
    return false;
  }

  private void scheduleTimedScan() {
    nextTimedScan =
        System.currentTimeMillis()
            + SCAN_INTERVAL_MS
            - SCAN_JITTER_MS
            + (long) (Math.random() * SCAN_JITTER_MS * 2.0);
  }

  private boolean triggerReady() {
    return System.currentTimeMillis() - lastTriggerScan >= TRIGGER_COOLDOWN_SEC * 1000L;
  }

  private void markTrigger() {
    lastTriggerScan = System.currentTimeMillis();
  }

  private Set<String> scanTab() {
    HashSet<String> found = new HashSet<>();
    ClientPacketListener handler = mc.getConnection();
    if (handler == null) return found;

    for (PlayerInfo entry : handler.getOnlinePlayers()) {
      String key = matchAdmin(entry);
      if (key != null) found.add(key);
    }
    for (String key : ADMIN_KEYS) {
      String display = AdminHeadTextures.displayName(key);
      if (handler.getPlayerInfoIgnoreCase(display) != null) found.add(key);
      if (handler.getPlayerInfoIgnoreCase(key) != null) found.add(key);
    }
    return found;
  }

  private String matchAdmin(PlayerInfo entry) {
    String profile = entry.getProfile().name();
    if (profile != null) {
      String key = profile.toLowerCase(Locale.ROOT);
      if (ADMIN_KEYS.contains(key)) return key;
    }
    Component tabName = entry.getTabListDisplayName();
    if (tabName != null) {
      String display = strip(tabName.getString());
      if (ADMIN_KEYS.contains(display)) {
        if (profile != null && !profile.isEmpty()) return profile.toLowerCase(Locale.ROOT);
        return display;
      }
    }
    return null;
  }

  private static String strip(String text) {
    return text.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);
  }
}
