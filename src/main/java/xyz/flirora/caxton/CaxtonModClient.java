package xyz.flirora.caxton;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import xyz.flirora.caxton.command.DumpAtlasCommand;
import xyz.flirora.caxton.dll.LibraryLoading;

import java.util.Calendar;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class CaxtonModClient implements ClientModInitializer {
    public static final String MOD_ID = "caxton";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean APRIL_FOOLS = isAprilFools();
    public static CaxtonConfig CONFIG = null;

    private static boolean isAprilFools() {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        return calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) == 1;
    }

    // This is meant to be called from a mixin method injecting into a Minecraft method.
    public static void onBrokenMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        LOGGER.warn("Use of {}.{} detected.", stackTrace[3].getClassName(), stackTrace[3].getMethodName());
        for (int i = 3; i < stackTrace.length; ++i) {
            LOGGER.warn("    at " + stackTrace[i].toString());
        }
        LOGGER.warn("Do not use this method; its API is fundamentally broken.");
        if (CONFIG.fatalOnBrokenMethodCall) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info(APRIL_FOOLS ? "Ufhed Gaadn ghap Muzsar!" : "Initializing mod...");

        CONFIG = CaxtonConfig.readFromFile();

        LibraryLoading.loadNativeLibrary(LOGGER);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            DumpAtlasCommand.register(dispatcher, registryAccess);
        });

        LOGGER.info("Registering built-in resource packs");

        ModContainer modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).get();

        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "inter"), modContainer, Text.translatable("caxton.resourcePack.inter"), ResourcePackActivationType.NORMAL);
        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "opensans"), modContainer, Text.translatable("caxton.resourcePack.opensans"), ResourcePackActivationType.NORMAL);
    }
}
