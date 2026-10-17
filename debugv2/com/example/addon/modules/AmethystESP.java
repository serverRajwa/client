package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
public final class AmethystESP extends Module {
   private final Setting<Integer> G;
   private final Set<ChunkPos> seen ;
   private final Setting<Boolean> setting ;
   private final SettingGroup settingGroup ;
   private Field J;
   private final Setting<SettingColor> color ;
   private final Setting<Integer> M;
   private final Set<ChunkPos> H;
   private final Set<ChunkPos> E;
   private final Setting<Boolean> setting2 ;
   private final Setting<SettingColor> K;
   private final Set<ChunkPos> B;
   private final SettingGroup A;
   private final Setting<Boolean> setting3 ;
   private final Setting<Integer> setting4 ;
   private final Map<ChunkPos, Set<BlockPos>> F;
   private final Setting<SettingColor> color2 ;
   private final Setting<Integer> D;
   private final Setting<Integer> C;
   private final Queue<ChunkPos> I;
   private final Setting<Integer> integerSetting;

   @EventHandler
   public void onA(ChunkDataEvent a) {
      AmethystESP amethystESP = a;
      if (a.mc.world != null && a.mc.player != null && a != null) {
         WorldChunk chunk = null;

         try {
            if (amethystESP.J != null) {
               chunk = (WorldChunk)amethystESP.J.get(a);
            } else {
               Class cls = a.getClass();
               Class[] superType = new Class[0];
               boolean active = true;
               Method method = cls.getMethod("getChunk", superType);
               Object[] obj = new Object[0];
               active = true;
               chunk = (WorldChunk)method.invoke(a, obj);
            }
         } catch (Exception ex) {
            return;
         }

         if (chunk != null) {
            AmethystESP amethystESP2 = chunk.getPos();
            ChunkDataEvent chunkPos = a.mc.player.getChunkPos();
            if (Math.abs(amethystESP2.x - chunkPos.x) <= (Integer)a.integerSetting.get()
               && Math.abs(amethystESP2.z - chunkPos.z) <= (Integer)a.integerSetting.get()
               && !a.d.contains(amethystESP2)
               && !a.I.contains(amethystESP2)) {
               a.I.add(amethystESP2);
            }
         }
      }
   }

   public AmethystESP() {
      super(AddonTemplate.F, "amethyst-esp", "Generates a fully connected continuous flood-fill zone of base chunks from the dense epicenter.");
      a.e = a.settings.getDefaultGroup();
      a.A = a.settings.createGroup("Base Detection Options");
      a.integerSetting = a.e
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("sim-distance")).description("Search radius in chunks.")).defaultValue8)
               .min1
               .max32
               .build()
         );
      a.c = a.e
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("min-cluster-size")).description("Minimum number of blocks for a normal geode."))
                  .defaultValue(3))
               .min(1)
               .max20
               .build()
         );
      a.C = a.e
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("chunk-y-level")).description("Y-level for the chunk marker.")).defaultValue55)
               .range(-41 & -24, 320)
               .build()
         );
      a.G = a.e
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("chunks-per-tick")).description("How many chunks to scan per tick.")).defaultValue(3))
               .min1
               .max10
               .build()
         );
      a.a = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("block-esp"))
                     .description("Renders boxes around amethyst blocks."))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.b = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("chunk-mark"))
                     .description("Marks single geode chunks."))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.l = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("chat-alert"))
                     .description("Sends a chat message when found."))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.i = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("esp-color"))
               .defaultValue(new SettingColor(180, 100, 255, 100))
               .build()
         );
      a.K = a.e
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("chunk-color"))
               .defaultValue(new SettingColor(180, 100, 255, 150))
               .build()
         );
      a.M = a.A
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("search-radius-blocks")).description("Radius in blocks to analyze cluster density."))
                  .defaultValue90)
               .min16
               .max(256)
               .build()
         );
      a.D = a.A
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("min-buds-for-base")).description("Amount of found buds within the radius to trigger base alert."))
                  .defaultValue(200))
               .min(10)
               .max(1000)
               .build()
         );
      a.g = a.A
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("base-chunks-color"))
                  .description("Color for the flood-filled connected base zone."))
               .defaultValue(new SettingColor(255, 0, 0, 140))
               .build()
         );
      a.F = new ConcurrentHashMap<>();
      a.B = ConcurrentHashMap.newKeySet();
      a.d = ConcurrentHashMap.newKeySet();
      a.E = ConcurrentHashMap.newKeySet();
      a.H = ConcurrentHashMap.newKeySet();
      a.I = new ConcurrentLinkedQueue<>();
      a.J = null;

      try {
         a.J = ChunkDataEvent.class.getDeclaredField("chunk");
         a.J.setAccessible((boolean)(1));
      } catch (Exception ex) {
      }
   }

   @EventHandler
   public void onA(Render3DEvent a) {
      AmethystESP amethystESP = a;
      if (a.mc.world != null) {
         Color color = (Color)a.K.get();
         Color color3 = (Color)a.i.get();
         Color color6 = (Color)a.g.get();
         if (color != null && color3 != null && color6 != null) {
            double d = ((Integer)a.C.get()).doubleValue();
            if (!a.F.isEmpty()) {
               Iterator it = a.F.entrySet().iterator();

               label58:
               while (true) {
                  Iterator it2 = it;

                  while (true) {
                     if (!it2.hasNext()) {
                        break label58;
                     }

                     Entry entry;
                     AmethystESP amethystESP2 = (ChunkPos)(entry = (Entry)it.next()).getKey();
                     Set set;
                     if (!(set = (Set)entry.getValue()).isEmpty()) {
                        if ((Boolean)amethystESP.b.get() && !amethystESP.E.contains(amethystESP2)) {
                           a.renderer
                              .box(
                                 (double)amethystESP2.getStartX(),
                                 d,
                                 (double)amethystESP2.getStartZ(),
                                 (double)(amethystESP2.getEndX() + 1),
                                 d + 0.05,
                                 (double)(amethystESP2.getEndZ() + (1)),
                                 color,
                                 color,
                                 ShapeMode.Both,
                                 0
                              );
                        }

                        if ((Boolean)amethystESP.a.get()) {
                           Iterator it3;
                           it2 = it3 = set.iterator();

                           while (it2.hasNext()) {
                              BlockPos pos = (BlockPos)it3.next();
                              it2 = it3;
                              a.renderer
                                 .box(
                                    (double)pos.getX(),
                                    (double)pos.getY(),
                                    (double)pos.getZ(),
                                    (double)(pos.getX() + 1),
                                    (double)(pos.getY() + 1),
                                    (double)(pos.getZ() + 1),
                                    color3,
                                    color3,
                                    ShapeMode.Both,
                                    0
                                 );
                           }
                        }
                        break;
                     }

                     it2 = it;
                  }
               }
            }

            if (!amethystESP.E.isEmpty()) {
               Iterator it4;
               Iterator it5 = it4 = amethystESP.E.iterator();

               while (it5.hasNext()) {
                  ChunkPos chunkPos = (ChunkPos)it4.next();
                  it5 = it4;
                  a.renderer
                     .box(
                        (double)chunkPos.getStartX(),
                        d,
                        (double)chunkPos.getStartZ(),
                        (double)(chunkPos.getEndX() + (1)),
                        d + 0.05,
                        (double)(chunkPos.getEndZ() + 1),
                        color6,
                        color6,
                        ShapeMode.Both,
                        0
                     );
               }
            }
         }
      }
   }

   @EventHandler
   public void onA(Post a) {
      AmethystESP amethystESP = a;
      if (a.mc.world != null && a.mc.player != null) {
         Post chunkPos = a.mc.player.getChunkPos();
         AmethystESP amethystESP2 = (Integer)a.integerSetting.get() + 4;
         a.d.removeIf(ax -> Math.abs(ax.x - chunkPos.x) > amethystESP2 || Math.abs(ax.z - chunkPos.z) > amethystESP2);
         a.F.keySet().removeIf(ax -> Math.abs(ax.x - chunkPos.x) > amethystESP2 || Math.abs(ax.z - chunkPos.z) > amethystESP2);
         a.B.removeIf(ax -> Math.abs(ax.x - chunkPos.x) > amethystESP2 || Math.abs(ax.z - chunkPos.z) > amethystESP2);
         a.E.removeIf(ax -> Math.abs(ax.x - chunkPos.x) > amethystESP2 || Math.abs(ax.z - chunkPos.z) > amethystESP2);
         a.H.removeIf(ax -> Math.abs(ax.x - chunkPos.x) > amethystESP2 || Math.abs(ax.z - chunkPos.z) > amethystESP2);
         if (!a.I.isEmpty()) {
            Post post = 0;
            AmethystESP amethystESP3 = 0;

            label37:
            while (true) {
               AmethystESP amethystESP4 = amethystESP;

               while (!amethystESP4.I.isEmpty() && post < amethystESP.G.get()) {
                  ChunkPos chunkPos2;
                  if ((chunkPos2 = amethystESP.I.poll()) == null) {
                     continue label37;
                  }

                  if (amethystESP.d.contains(chunkPos2)) {
                     amethystESP4 = amethystESP;
                  } else {
                     WorldChunk chunk;
                     if ((chunk = amethystESP.mc.world.getChunkManager().getWorldChunk(chunkPos2.x, chunkPos2.z, (boolean)0)) == null) {
                        continue label37;
                     }

                     if (chunk.isEmpty()) {
                        amethystESP4 = amethystESP;
                     } else {
                        amethystESP4 = amethystESP;
                        int value = 1;
                        post++;
                        amethystESP.handler2(chunk);
                        amethystESP3 = value;
                     }
                  }
               }

               if (amethystESP3 != 0) {
                  amethystESP.b();
               }
               break;
            }
         }
      }
   }

   public void onTick() {
      a.k();
   }

   public void handler2() {
      a.k();
      a.m();
   }
}
