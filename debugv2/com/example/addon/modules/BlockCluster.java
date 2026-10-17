package com.example.addon.modules;

import java.util.Iterator;
import java.util.List;
import net.minecraft.class_2338;

class BlockCluster {
   private static final String DEXTER_MARK_2 = "Discord.gg/leakestan";
   final int I;
   final List<class_2338> blocks;

   class_2338 getBlockPos() {
      long l = 0L;
      long l4 = 0L;
      long l5 = 0L;

      Iterator it;
      for (Iterator it2 = it = a.blocks.iterator(); it2.hasNext(); it2 = it) {
         BlockCluster blockCluster = (class_2338)it.next();
         l += (long)blockCluster.method_10263();
         l4 += (long)blockCluster.method_10264();
         l5 += (long)blockCluster.method_10260();
      }

      return new class_2338((int)(l / (long)a.blocks.size()), (int)(l4 / (long)a.blocks.size()), (int)(l5 / (long)a.blocks.size()));
   }

   BlockCluster(List<class_2338> a) {
      a.blocks = a;
      a.I = a.size();
   }
}
