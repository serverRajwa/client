package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.Vec3d;
public class RegionMap extends Module {
   private final Setting<Boolean> setting ;
   private final <unrepresentable> fieldE ;
   private final Setting<SettingColor> J;
   private final <unrepresentable> fieldG ;
   private final SettingGroup M;
   private final Setting<Double> H;
   private final Setting<Boolean> E;
   private final Setting<Boolean> setting2 ;
   private final Setting<Integer> K;
   private final Setting<SettingColor> B;
   private final Setting<Boolean> A;
   private final Setting<Double> setting3 ;
   private final SettingGroup settingGroup ;
   private final Setting<Integer> F;
   private final <unrepresentable> fieldI ;
   private final Setting<SettingColor> D;
   private final Setting<Integer> C;
   private final SettingGroup I;
   private final SettingGroup settingGroup2;

   public RegionMap() {
      super(AddonTemplate.C, "region-map", "DonutSMP region map and location tracker");
      a.M = a.settings.createGroup("Display");
      a.I = a.settings.createGroup("Position");
      a.c = a.settings.createGroup("Visual");
      a.settingGroup2 = a.settings.createGroup("Theme");
      a.F = a.I.add(((Builder)((Builder)new Builder().name("position-x")).defaultValue15).min0.sliderMax1920.build());
      a.K = a.I.add(((Builder)((Builder)new Builder().name("position-y")).defaultValue15).min0.sliderMax1080.build());
      a.C = a.I
         .add(((Builder)((Builder)new Builder().name("cell-size")).defaultValue22).range(12, 50).sliderRange(12, 50).build());
      a.A = a.M
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("enable-coordinates"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.b = a.M
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("enable-region-labels"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.a = a.M
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("enable-grid"))
                  .defaultValue(Boolean.valueOf((boolean)1)))
               .build()
         );
      a.E = a.M
         .add(
            ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("enable-player-indicator"))
                  .defaultValue(Boolean.valueOf((boolean)(1))))
               .build()
         );
      a.H = a.c
         .add(
            ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("transparency"))
               .defaultValue(0.75)
               .range(0.1, 1.0)
               .build()
         );
      a.l = a.c
         .add(
            ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("label-size"))
               .defaultValue(0.9)
               .range(0.4, 2.5)
               .build()
         );
      a.B = a.settingGroup2
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("background-color"))
               .defaultValue(new SettingColor(25, 25, 25, 180))
               .build()
         );
      a.D = a.settingGroup2
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("cursor-color"))
               .defaultValue(new SettingColor(255, 0, 0, 255))
               .build()
         );
      a.J = a.settingGroup2
         .add(
            ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("grid-color"))
               .defaultValue(new SettingColor(15, 15, 15, 255))
               .build()
         );
      a.e = new Object() {
         private final String[] C;
         private final Map<Integer, <unrepresentable>> I;
         private final Color[] colors;

         public String[] getNames() {
            return a.C;
         }

         {
            var temp = a;
            int value = 6;
            a.I = new HashMap<>();
            String[] text = new String[temp];
            boolean active = true;
            text[0] = "EU Central";
            text[1] = "EU West";
            text[2] = "NA East";
            text[3] = "NA West";
            text[4] = "Asia";
            text[5] = "Oceania";
            a.C = text;
            Color[] color = new Color[6];
            active = true;
            color[0] = new Color(159, 206, 99);
            color[1] = new Color(0, 166, 99);
            color[2] = new Color(79, 173, 234);
            color[3] = new Color(47, 110, 186);
            color[4] = new Color(245, 194, 66);
            color[5] = new Color(252, 136, 3);
            a.settingGroup2 = color;
            int[][] value = new int[81][];
            int value2 = 1;
            int[] value3 = new int[2];
            boolean active2 = true;
            int value4 = 0;
            value3[0] = 82;
            value3[1] = 5;
            value[value4] = value3;
            int[] value5 = new int[2];
            active2 = true;
            value5[0] = 100;
            value4 = 1;
            value5[1] = 3;
            value[value4] = value5;
            value2 = 2;
            int[] value6 = new int[2];
            boolean active3 = true;
            value6[0] = 101;
            value6[1] = 3;
            value[value2] = value6;
            value2 = 3;
            int[] value7 = new int[2];
            active3 = true;
            value7[0] = 102;
            value7[1] = 3;
            value[value2] = value7;
            int[] value8 = new int[2];
            active3 = true;
            value8[0] = 103;
            value8[1] = 2;
            value[4] = value8;
            int[] value9 = new int[2];
            active3 = true;
            value9[0] = 104;
            value9[1] = 2;
            value[5] = value9;
            int[] value10 = new int[2];
            active3 = true;
            value10[0] = 105;
            value10[1] = 2;
            value[6] = value10;
            int[] value11 = new int[2];
            active3 = true;
            value11[0] = 106;
            value11[1] = 2;
            value[7] = value11;
            int[] value12 = new int[2];
            active3 = true;
            value12[0] = 91;
            value12[1] = 2;
            value[8] = value12;
            int[] value13 = new int[2];
            active3 = true;
            value13[0] = 83;
            value13[1] = 5;
            value[9] = value13;
            int[] value14 = new int[2];
            active3 = true;
            value14[0] = 44;
            value14[1] = 3;
            value[10] = value14;
            int[] value15 = new int[2];
            active3 = true;
            value15[0] = 75;
            value15[1] = 3;
            value[11] = value15;
            int[] value16 = new int[2];
            active3 = true;
            value16[0] = 42;
            value16[1] = 3;
            value[12] = value16;
            int[] value17 = new int[2];
            active3 = true;
            value17[0] = 41;
            value17[1] = 2;
            value[13] = value17;
            int[] value18 = new int[2];
            active3 = true;
            value18[0] = 40;
            value18[1] = 2;
            value[14] = value18;
            int[] value19 = new int[2];
            active3 = true;
            value19[0] = 39;
            value19[1] = 2;
            value[15] = value19;
            int[] value20 = new int[2];
            active3 = true;
            value20[0] = 38;
            value20[1] = 2;
            value[16] = value20;
            int[] value21 = new int[2];
            active3 = true;
            value21[0] = 92;
            value21[1] = 2;
            value[17] = value21;
            int[] value22 = new int[2];
            active3 = true;
            value22[0] = 84;
            value22[1] = 5;
            value[18] = value22;
            int[] value23 = new int[2];
            active3 = true;
            value23[0] = 45;
            value23[1] = 3;
            value[19] = value23;
            int[] value24 = new int[2];
            active3 = true;
            value24[0] = 14;
            value24[1] = 3;
            value[20] = value24;
            int[] value25 = new int[2];
            active3 = true;
            value25[0] = 13;
            value25[1] = 3;
            value[21] = value25;
            int[] value26 = new int[2];
            active3 = true;
            value26[0] = 12;
            value26[1] = 2;
            value[22] = value26;
            int[] value27 = new int[2];
            active3 = true;
            value27[0] = 11;
            value27[1] = 2;
            value[23] = value27;
            int[] value28 = new int[2];
            active3 = true;
            value28[0] = 10;
            value28[1] = 2;
            value[24] = value28;
            int[] value29 = new int[2];
            active3 = true;
            value29[0] = 37;
            value29[1] = 2;
            value[25] = value29;
            int[] value30 = new int[2];
            active3 = true;
            value30[0] = 93;
            value30[1] = 2;
            value[26] = value30;
            int[] value31 = new int[2];
            active3 = true;
            value31[0] = 85;
            value31[1] = 5;
            value[27] = value31;
            int[] value32 = new int[2];
            active3 = true;
            value32[0] = 46;
            value32[1] = 5;
            value[28] = value32;
            int[] value33 = new int[2];
            active3 = true;
            value33[0] = 74;
            value33[1] = 5;
            value[29] = value33;
            int[] value34 = new int[2];
            active3 = true;
            value34[0] = 3;
            value34[1] = 3;
            value[30] = value34;
            int[] value35 = new int[2];
            active3 = true;
            value35[0] = 2;
            value35[1] = 2;
            value[31] = value35;
            value[32] = new int[]{1, 2};
            int[] value = new int[2];
            temp2 = true;
            value[0] = 25;
            value[1] = 2;
            temp[33] = value;
            int[] value2 = new int[2];
            temp2 = true;
            value2[0] = 36;
            value2[1] = 2;
            temp[34] = value2;
            int[] value3 = new int[2];
            temp2 = true;
            value3[0] = 94;
            value3[1] = 2;
            temp[35] = value3;
            int[] value4 = new int[2];
            temp2 = true;
            value4[0] = 86;
            value4[1] = 4;
            temp[36] = value4;
            int[] value5 = new int[2];
            temp2 = true;
            value5[0] = 47;
            value5[1] = 4;
            temp[37] = value5;
            int[] value6 = new int[2];
            temp2 = true;
            value6[0] = 72;
            value6[1] = 4;
            temp[38] = value6;
            int[] value7 = new int[2];
            temp2 = true;
            value7[0] = 71;
            value7[1] = 4;
            temp[39] = value7;
            int[] value8 = new int[2];
            temp2 = true;
            value8[0] = 5;
            value8[1] = 2;
            temp[40] = value8;
            int[] value9 = new int[2];
            temp2 = true;
            value9[0] = 4;
            value9[1] = 2;
            temp[41] = value9;
            int[] value10 = new int[2];
            temp2 = true;
            value10[0] = 24;
            value10[1] = 2;
            temp[42] = value10;
            int[] value11 = new int[2];
            temp2 = true;
            value11[0] = 35;
            value11[1] = 2;
            temp[43] = value11;
            int[] value12 = new int[2];
            temp2 = true;
            value12[0] = 95;
            value12[1] = 2;
            temp[44] = value12;
            int[] value13 = new int[2];
            temp2 = true;
            value13[0] = 87;
            value13[1] = 4;
            temp[45] = value13;
            int[] value14 = new int[2];
            temp2 = true;
            value14[0] = 51;
            value14[1] = 1;
            temp[46] = value14;
            int[] value15 = new int[2];
            temp2 = true;
            value15[0] = 17;
            value15[1] = 1;
            temp[47] = value15;
            int[] value16 = new int[2];
            temp2 = true;
            value16[0] = 9;
            value16[1] = 0;
            temp[48] = value16;
            int[] value17 = new int[2];
            temp2 = true;
            value17[0] = 8;
            value17[1] = 0;
            temp[49] = value17;
            int[] value18 = new int[2];
            temp2 = true;
            value18[0] = 7;
            value18[1] = 0;
            temp[50] = value18;
            int[] value19 = new int[2];
            temp2 = true;
            value19[0] = 23;
            value19[1] = 0;
            temp[51] = value19;
            int[] value20 = new int[2];
            temp2 = true;
            value20[0] = 34;
            value20[1] = 0;
            temp[52] = value20;
            int[] value21 = new int[2];
            temp2 = true;
            value21[0] = 96;
            value21[1] = 2;
            temp[53] = value21;
            int[] value22 = new int[2];
            temp2 = true;
            value22[0] = 88;
            value22[1] = 4;
            temp[54] = value22;
            int[] value23 = new int[2];
            temp2 = true;
            value23[0] = 54;
            value23[1] = 1;
            temp[55] = value23;
            int[] value24 = new int[2];
            temp2 = true;
            value24[0] = 18;
            value24[1] = 1;
            temp[56] = value24;
            int[] value25 = new int[2];
            temp2 = true;
            value25[0] = 61;
            value25[1] = 0;
            temp[57] = value25;
            int[] value26 = new int[2];
            temp2 = true;
            value26[0] = 62;
            value26[1] = 0;
            temp[58] = value26;
            int[] value27 = new int[2];
            temp2 = true;
            value27[0] = 21;
            value27[1] = 0;
            temp[59] = value27;
            int[] value28 = new int[2];
            temp2 = true;
            value28[0] = 22;
            value28[1] = 0;
            temp[60] = value28;
            int[] value29 = new int[2];
            temp2 = true;
            value29[0] = 33;
            value29[1] = 0;
            temp[61] = value29;
            int[] value30 = new int[2];
            temp2 = true;
            value30[0] = 97;
            value30[1] = 0;
            temp[62] = value30;
            int[] value31 = new int[2];
            temp2 = true;
            value31[0] = 89;
            value31[1] = 0;
            temp[63] = value31;
            int[] value32 = new int[2];
            temp2 = true;
            value32[0] = 26;
            value32[1] = 1;
            temp[64] = value32;
            int[] value33 = new int[2];
            temp2 = true;
            value33[0] = 27;
            value33[1] = 0;
            temp[65] = value33;
            int[] value34 = new int[2];
            temp2 = true;
            value34[0] = 28;
            value34[1] = 0;
            temp[66] = value34;
            int[] value35 = new int[2];
            temp2 = true;
            value35[0] = 29;
            value35[1] = 0;
            temp[67] = value35;
            int[] value36 = new int[2];
            temp2 = true;
            value36[0] = 30;
            value36[1] = 0;
            temp[68] = value36;
            int[] value37 = new int[2];
            temp2 = true;
            value37[0] = 59;
            value37[1] = 0;
            temp[69] = value37;
            int[] value38 = new int[2];
            temp2 = true;
            value38[0] = 32;
            value38[1] = 0;
            temp[70] = value38;
            int[] value39 = new int[2];
            temp2 = true;
            value39[0] = 98;
            value39[1] = 0;
            temp[71] = value39;
            int[] value40 = new int[2];
            temp2 = true;
            value40[0] = 90;
            value40[1] = 0;
            temp[72] = value40;
            int[] value41 = new int[2];
            temp2 = true;
            value41[0] = 107;
            value41[1] = 1;
            temp[73] = value41;
            int[] value42 = new int[2];
            temp2 = true;
            value42[0] = 108;
            value42[1] = 1;
            temp[74] = value42;
            int[] value43 = new int[2];
            temp2 = true;
            value43[0] = 109;
            value43[1] = 1;
            temp[75] = value43;
            int[] value44 = new int[2];
            temp2 = true;
            value44[0] = 110;
            value44[1] = 1;
            temp[76] = value44;
            int[] value45 = new int[2];
            temp2 = true;
            value45[0] = 111;
            value45[1] = 1;
            temp[77] = value45;
            int[] value46 = new int[2];
            temp2 = true;
            value46[0] = 112;
            value46[1] = 1;
            temp[78] = value46;
            int[] value47 = new int[2];
            temp2 = true;
            value47[0] = 113;
            value47[1] = 1;
            temp[79] = value47;
            int[] value48 = new int[2];
            temp2 = true;
            value48[0] = 99;
            value48[1] = 0;
            temp[80] = value48;
            int[][] i2 = temp;

            for (int i3 = temp4 = 0; i3 < i2.length; i3 = temp4) {
               temp3.I.put(temp4, (<unrepresentable>)(new Object(temp2[temp][0], temp2[temp][1], temp / 9, temp++ % 9) {
                  public final int value;
                  public final int C;
                  public final int value3;
                  public final int value4;

                  {
                     a.settingGroup2 = a;
                     a.value3 = a;
                     a.C = a;
                     a.value = a;
                  }
               }));
            }
         }

         public Color getColor(int ax) {
            return a >= 0 && a < a.settingGroup2.length ? a.settingGroup2[a] : Color.WHITE;
         }

         public Color[] getColors() {
            return a.settingGroup2;
         }

         public int indexAt(double axx, double ax) {
            double d = (int)((a + 225000.0) / 50000.0);
            int value = (int)((a + 225000.0) / 50000.0);
            if (d >= 0 && d < 9 && value >= 0 && value < 9) {
               var tmp;
               return (tmp = (<unrepresentable>)a.I.get(value * 9 + d)) != null ? tmp.settingGroup2 : -1;
            } else {
               return -1;
            }
         }

         public <unrepresentable> handler2(int ax) {
            return a.I.get(a);
         }
      };
      a.g = new Object(a) {
         private final RegionMap regionMap;

         void render2(<unrepresentable> axxx, <unrepresentable> axx) {
            var tmp = a;
            Renderer2D.COLOR.begin();

            int i;
            for (int i2 = i = 0; i2 < 81; i2 = i) {
               var tmp2 = a.getColor(i);
               Color color;
               (color = new Color(a.getColor(tmp2.I))).a = (int)(tmp.C * 255.0);
               double d = (double)(tmp.I + tmp2.D * tmp.settingGroup2 + (1));
               double score = (double)(tmp.D + tmp2.C * tmp.settingGroup2 + 1);
               double d2 = (double)(tmp.settingGroup2 - 2);
               int value = tmp.settingGroup2 - 2;
               i++;
               Renderer2D.COLOR.quad(d, score, d2, (double)value, color);
            }

            Renderer2D.COLOR.render();
         }

         void renderLabels(<unrepresentable> axxxx, <unrepresentable> axxx, double axx) {
            var tmp = a;
            TextRenderer.get().begin(a, (boolean)(0), (boolean)(1));

            int i;
            for (int i2 = i = 0; i2 < 81; i2 = i) {
               var tmp2;
               String str = String.valueOf((tmp2 = a.getColor(i)).settingGroup2);
               TextRenderer textRenderer = TextRenderer.get();
               double d = (double)(tmp.I + tmp2.D * tmp.settingGroup2) + ((double)tmp.settingGroup2 - TextRenderer.get().getWidth(str) * a) / 2.0;
               double score = (double)(tmp.D + tmp2.C * tmp.settingGroup2) + ((double)tmp.settingGroup2 - TextRenderer.get().getHeight() * a) / 2.0;
               i++;
               textRenderer.render(str, d, score, Color.WHITE, (boolean)0);
            }

            TextRenderer.get().end();
         }

         {
            ax.settingGroup2 = a;
         }

         void render3(<unrepresentable> axx) {
            Color color;
            (color = new Color((Color)ax.settingGroup2.B.get())).a = (int)(a.C * 255.0);
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad((double)a.I, (double)a.D, (double)a.m(), (double)a.getNames(), color);
            Renderer2D.COLOR.render();
         }

         void render4(<unrepresentable> axxx, SettingColor axx) {
            var tmp = a;
            Renderer2D.COLOR.begin();

            int i;
            for (int i2 = i = 0; i2 <= 9; i2 = i) {
               Renderer2D.COLOR.quad((double)(tmp.I + i * tmp.settingGroup2), (double)tmp.D, 1.0, (double)tmp.getNames(), a);
               Renderer2D renderer2D = Renderer2D.COLOR;
               double d = (double)tmp.I;
               double score = (double)(tmp.D + i * tmp.settingGroup2);
               double d2 = (double)tmp.m();
               i++;
               renderer2D.quad(d, score, d2, 1.0, a);
            }

            Renderer2D.COLOR.render();
         }
      };
      a.i = new Object(a) {
         private final RegionMap regionMap2;

         {
            ax.settingGroup2 = a;
         }

         void handler3(<unrepresentable> axxxxx, Vec3d axxxx, float axxx, SettingColor axx) {
            double x = (a.x + 225000.0) / 50000.0;
            double z = (a.z + 225000.0) / 50000.0;
            if (x >= 0.0 && x < 9.0 && z >= 0.0 && z < 9.0) {
               ax.getColor(
                  (int)((double)a.I + x * (double)a.settingGroup2), (int)((double)a.D + z * (double)a.settingGroup2), Math.toRadians((double)(-a)), a
               );
            }
         }
      };
   }

   public void handler4() {
   }
}
