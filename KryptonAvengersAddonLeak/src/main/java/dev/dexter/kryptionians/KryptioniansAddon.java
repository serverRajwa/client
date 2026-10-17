package dev.dexter.kryptionians;

import com.slova.autoorder.modules.AutoOrder;
import com.vrzt.zinc.modules.esp.RegionMap;
import dev.dexter.kryptionians.modules.AdminDetector;
import dev.dexter.kryptionians.modules.BaseESP;
import dev.dexter.kryptionians.modules.ChunkReloader;
import dev.dexter.kryptionians.modules.GeodeEsp;
import dev.dexter.kryptionians.modules.HumanAutoTool;
import dev.dexter.kryptionians.modules.SpawnerNotifier;
import dev.dexter.kryptionians.modules.UdRelog;
import dev.dexter.kryptionians.modules.map.RtpMap;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.listeners.LambdaListener;

public class KryptioniansAddon extends MeteorAddon {
  public static final Category CATEGORY = new Category("Krypton Avengers");

  @Override
  public void onInitialize() {
    OrbitFactory factory = new OrbitFactory();
    MeteorClient.EVENT_BUS.registerLambdaFactory("dev.dexter.kryptionians", factory);
    MeteorClient.EVENT_BUS.registerLambdaFactory("com.vrzt.zinc", factory);
    MeteorClient.EVENT_BUS.registerLambdaFactory("com.slova.autoorder", factory);

    Modules modules = Modules.get();
    modules.add(new AdminDetector());
    modules.add(new BaseESP());
    modules.add(new ChunkReloader());
    modules.add(new GeodeEsp());
    modules.add(new SpawnerNotifier());
    modules.add(new UdRelog());
    modules.add(new HumanAutoTool());
    modules.add(new RtpMap());
    modules.add(new RegionMap());
    modules.add(new AutoOrder());
  }

  @Override
  public void onRegisterCategories() {
    Modules.registerCategory(CATEGORY);
  }

  @Override
  public String getPackage() {
    return "dev.dexter.kryptionians";
  }

  @Override
  public String getWebsite() {
    return "https://discord.gg/PYfRxkeR7C";
  }

  private static final class OrbitFactory implements LambdaListener.Factory {
    @Override
    public MethodHandles.Lookup create(Method lookupInMethod, Class<?> klass)
        throws InvocationTargetException, IllegalAccessException {
      return (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup());
    }
  }
}
