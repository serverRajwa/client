package com.example.addon.modules;

import net.minecraft.class_1923;

record ChunkHit() {
   private final int F;
   private final class_1923 i;
   private final boolean D;
   private final boolean C;
   private final int I;
   private final class_1923 chunkPos;

   public class_1923 m() {
      return a.i;
   }

   public boolean m() {
      return a.D;
   }

   public int m() {
      return a.I;
   }

   public int scaledSize() {
      return a.F;
   }

   public boolean validate2() {
      return a.C;
   }
}
