package xyz.flirora.caxton.forge;

import net.minecraft.resource.*;
import net.minecraft.text.Text;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.locating.IModFile;
import xyz.flirora.caxton.CaxtonModClient;

import java.nio.file.Path;

@Mod(CaxtonModClient.MOD_ID)
public class CaxtonModResourceLocationFlavor {
    public CaxtonModResourceLocationFlavor() {
        // Submit our event bus to let architectury register our content on the right time
//        EventBuses.registerModEventBus(CaxtonModClient.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.register(this);
        CaxtonModClient.init(new PlatformHooksResourceLocationFlavor());
    }

    private static final String[] BUILTIN_PACKS = {"inter", "opensans"};

    @SubscribeEvent
    public void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
            IModFile file = ModList.get().getModFileById(CaxtonModClient.MOD_ID).getFile();
            for (String id : BUILTIN_PACKS) {
                Path sourcePath = file.findResource("resourcepacks/" + id);
                var pack = ResourcePackProfile.create(
                        "builtin/font-" + id,
                        Text.translatable("caxton.resourcePack." + id),
                        false,
                        path -> new DirectoryResourcePack(path, sourcePath, false),
                        ResourceType.CLIENT_RESOURCES,
                        ResourcePackProfile.InsertionPosition.TOP,
                        ResourcePackSource.BUILTIN);
                event.addRepositorySource(profileAdder -> profileAdder.accept(pack));
            }
        }
    }
}
