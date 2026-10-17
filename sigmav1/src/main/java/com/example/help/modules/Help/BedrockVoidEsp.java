
package com.example.help.modules.Help;

import com.example.help.HelpAddon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
public class BedrockVoidEsp
extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgRender;
    private final SettingGroup sgPerformance;
    private final Setting<Integer> minVoidSize;
    private final Setting<Boolean> showEsp;
    private final Setting<Boolean> showTracers;
    private final Setting<Boolean> chatFeedback;
    private final Setting<Integer> maxMessagesPerMinute;
    private final Setting<SettingColor> espColor;
    private final Setting<ShapeMode> shapeMode;
    private final Setting<SettingColor> tracerColor;
    private final Setting<Boolean> useThreading;
    private final Setting<Integer> threadPoolSize;
    private static final List<Integer> OVERWORLD_Y_LEVELS = List.of(Integer.valueOf(-64), Integer.valueOf(-63), Integer.valueOf(-62), Integer.valueOf(-61), Integer.valueOf(-60));
    private static final List<Integer> NETHER_FLOOR_Y_LEVELS = List.of(Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4));
    private static final List<Integer> NETHER_ROOF_Y_LEVELS = List.of(Integer.valueOf(123), Integer.valueOf(124), Integer.valueOf(125), Integer.valueOf(126), Integer.valueOf(127));
    private String currentDimension;
    private final Set<BlockPos> voidBlocks;
    private ExecutorService threadPool;
    private long lastMinuteStart;
    private int messagesThisMinute;

    public BedrockVoidEsp() {
        super(HelpAddon.CATEGORY, "bedrock-void-esp", "Finds voids in bedrock layers. Useful for indicating places where spawners may be un-raidable if located there.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgRender = this.settings.createGroup("Render");
        this.sgPerformance = this.settings.createGroup("Performance");
        this.minVoidSize = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("min-void-size")).description("Minimum number of blocks to consider an area a void.")).defaultValue(2)).min(1).sliderMax(50).onChanged(this::onSettingChanged)).build());
        this.showEsp = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("show-esp")).description("Show the void ESP.")).defaultValue(true)).build());
        this.showTracers = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("show-tracers")).description("Show tracers to the voids.")).defaultValue(false)).build());
        this.chatFeedback = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("chat-feedback")).description("Announce voids in chat")).defaultValue(true)).build());
        this.maxMessagesPerMinute = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("max-messages-per-minute")).description("Maximum void messages per minute (0 = unlimited)")).defaultValue(10)).min(0).max(60).sliderRange(0, 60).visible(() -> this.chatFeedback.get())).build());
        this.espColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("esp-color")).description("Color of the void ESP.")).defaultValue(new SettingColor(240, 85, 80, 128)).visible(() -> this.showEsp.get())).build());
        this.shapeMode = this.sgRender.add((Setting)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode")).description("Shape mode of the void ESP.")).defaultValue(ShapeMode.Both)).visible(() -> this.showEsp.get())).build());
        this.tracerColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("tracer-color")).description("Color of the void tracers.")).defaultValue(new SettingColor(255, 0, 0, 255)).visible(() -> this.showTracers.get())).build());
        this.useThreading = this.sgPerformance.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("enable-threading")).description("Use multi-threading for chunk scanning (better performance)")).defaultValue(true)).build());
        this.threadPoolSize = this.sgPerformance.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("thread-pool-size")).description("Number of threads to use for scanning")).defaultValue(4)).min(1).max(8).sliderRange(1, 8).visible(() -> this.useThreading.get())).build());
        this.voidBlocks = ConcurrentHashMap.newKeySet();
        this.lastMinuteStart = 0L;
        this.messagesThisMinute = 0;
    }

    public void onActivate() {
        if (this.mc.world == null) {
            return;
        }
        this.currentDimension = this.mc.world.getRegistryKey().getValue().toString();
        if (((Boolean)this.useThreading.get()).booleanValue()) {
            this.threadPool = Executors.newFixedThreadPool((Integer)this.threadPoolSize.get());
        }
        this.voidBlocks.clear();
        this.lastMinuteStart = 0L;
        this.messagesThisMinute = 0;
        for (Chunk chunk : Utils.chunks()) {
            if (!(chunk instanceof WorldChunk)) continue;
            WorldChunk worldChunk = (WorldChunk)chunk;
            if (((Boolean)this.useThreading.get()).booleanValue() && this.threadPool != null && !this.threadPool.isShutdown()) {
                this.threadPool.submit(() -> this.scanChunk(worldChunk));
                continue;
            }
            this.scanChunk(worldChunk);
        }
    }

    public void onDeactivate() {
        if (this.threadPool != null && !this.threadPool.isShutdown()) {
            this.threadPool.shutdownNow();
            this.threadPool = null;
        }
        this.voidBlocks.clear();
    }

    @EventHandler
    private void onChunkLoad(ChunkDataEvent event) {
        WorldChunk loadedChunk = event.chunk();
        if (loadedChunk instanceof WorldChunk) {
            WorldChunk worldChunk = loadedChunk;
            if (((Boolean)this.useThreading.get()).booleanValue() && this.threadPool != null && !this.threadPool.isShutdown()) {
                this.threadPool.submit(() -> this.scanChunk(worldChunk));
            } else {
                this.scanChunk(worldChunk);
            }
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        BlockPos pos = event.pos;
        List<Integer> yLevels = this.getYLevelsForDimension();
        if (yLevels.contains(pos.getY()) && this.mc.world != null) {
            ChunkPos chunkPos = new ChunkPos(pos);
            WorldChunk worldChunk = this.mc.world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);
            if (worldChunk != null) {
                if (((Boolean)this.useThreading.get()).booleanValue() && this.threadPool != null && !this.threadPool.isShutdown()) {
                    this.threadPool.submit(() -> this.scanChunk(worldChunk));
                } else {
                    this.scanChunk(worldChunk);
                }
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        Packet receivedPacket = event.packet;
        if (receivedPacket instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket packet = (ChunkDataS2CPacket)receivedPacket;
            ChunkPos chunkPos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
            this.voidBlocks.removeIf(blockPos -> new ChunkPos(blockPos).equals(chunkPos));
        }
    }

    private void scanChunk(WorldChunk chunk) {
        if (this.mc.world == null || chunk == null) {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        this.voidBlocks.removeIf(blockPos -> new ChunkPos(blockPos).equals(chunkPos));
        List<Integer> yLevels = this.getYLevelsForDimension();
        if (!yLevels.isEmpty()) {
            this.findVoidsInChunk(chunk, yLevels);
        }
    }

    private List<Integer> getYLevelsForDimension() {
        return switch (this.currentDimension) {
            case "minecraft:overworld" -> OVERWORLD_Y_LEVELS;
            case "minecraft:the_nether" -> {
                ArrayList<Integer> levels = new ArrayList<Integer>();
                levels.addAll(NETHER_FLOOR_Y_LEVELS);
                levels.addAll(NETHER_ROOF_Y_LEVELS);
                yield levels;
            }
            default -> Collections.emptyList();
        };
    }

    private void findVoidsInChunk(WorldChunk chunk, List<Integer> yLevels) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        HashSet<BlockPos> processed = new HashSet<BlockPos>();
        for (int y : yLevels) {
            for (int dx = 0; dx < 16; ++dx) {
                for (int dz = 0; dz < 16; ++dz) {
                    List<BlockPos> group;
                    BlockPos pos = new BlockPos(startX + dx, y, startZ + dz);
                    if (processed.contains(pos) || BedrockVoidEsp.isBedrock(this.getBlockState(pos)) || (group = this.floodFillVoid(pos, yLevels, processed)).size() < (Integer)this.minVoidSize.get() || !this.isVoidEnclosed(group)) continue;
                    this.voidBlocks.addAll(group);
                    if (group.isEmpty()) continue;
                    BlockPos firstBlock = group.get(0);
                    this.sendVoidMessage("\u00a75[\u00a7dBedrockVoidESP\u00a75] \u00a7bVoid found\u00a75: \u00a7b" + group.size() + " blocks at " + firstBlock.toShortString());
                }
            }
        }
    }

    private List<BlockPos> floodFillVoid(BlockPos start, List<Integer> yLevels, Set<BlockPos> processed) {
        ArrayList<BlockPos> group = new ArrayList<BlockPos>();
        LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
        queue.offer(start);
        while (!queue.isEmpty() && group.size() < 200) {
            BlockPos current = (BlockPos)queue.poll();
            if (current == null || processed.contains(current) || BedrockVoidEsp.isBedrock(this.getBlockState(current)) || !yLevels.contains(current.getY())) continue;
            processed.add(current);
            group.add(current);
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);
                if (processed.contains(neighbor)) continue;
                queue.offer(neighbor);
            }
        }
        return group;
    }

    private boolean isVoidEnclosed(List<BlockPos> group) {
        for (BlockPos pos : group) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.offset(dir);
                if (group.contains(neighbor) || BedrockVoidEsp.isBedrock(this.getBlockState(neighbor))) continue;
                return false;
            }
        }
        return true;
    }

    private BlockState getBlockState(BlockPos pos) {
        return this.mc.world == null ? Blocks.AIR.getDefaultState() : this.mc.world.getBlockState(pos);
    }

    private void onSettingChanged(Integer value) {
        if (this.isActive() && this.mc.world != null) {
            this.voidBlocks.clear();
            for (Chunk chunk : Utils.chunks()) {
                if (!(chunk instanceof WorldChunk)) continue;
                WorldChunk worldChunk = (WorldChunk)chunk;
                if (((Boolean)this.useThreading.get()).booleanValue() && this.threadPool != null && !this.threadPool.isShutdown()) {
                    this.threadPool.submit(() -> this.scanChunk(worldChunk));
                    continue;
                }
                this.scanChunk(worldChunk);
            }
        }
    }

    private static boolean isBedrock(BlockState state) {
        return state.isOf(Blocks.BEDROCK);
    }

    private void sendVoidMessage(String message) {
        if (((Boolean)this.chatFeedback.get()).booleanValue()) {
            int maxMessages;
            long currentTime = System.currentTimeMillis();
            long currentMinute = currentTime / 60000L;
            if (currentMinute != this.lastMinuteStart) {
                this.lastMinuteStart = currentMinute;
                this.messagesThisMinute = 0;
            }
            if ((maxMessages = ((Integer)this.maxMessagesPerMinute.get()).intValue()) == 0 || this.messagesThisMinute < maxMessages) {
                this.info(message, new Object[0]);
                ++this.messagesThisMinute;
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        Color color;
        if (((Boolean)this.showEsp.get()).booleanValue()) {
            color = (Color)this.espColor.get();
            for (BlockPos pos : this.voidBlocks) {
                event.renderer.box(pos, color, color, (ShapeMode)this.shapeMode.get(), 0);
            }
        }
        if (((Boolean)this.showTracers.get()).booleanValue()) {
            color = (Color)this.tracerColor.get();
            Vec3d camera = this.mc.player != null ? this.mc.player.getCameraPosVec(event.tickDelta) : Vec3d.ZERO;
            for (BlockPos pos : this.voidBlocks) {
                Vec3d blockCenter = Vec3d.ofCenter((Vec3i)pos);
                event.renderer.line(camera.x, camera.y, camera.z, blockCenter.x, blockCenter.y, blockCenter.z, color);
            }
        }
    }
}
