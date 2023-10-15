package xyz.flirora.caxton.forge;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.locating.IModFile;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.command.CaxtonCommands;
import xyz.flirora.caxton.command.CommonDumpAtlasCommand;

import java.nio.file.Path;

@Mod(CaxtonModClient.MOD_ID)
public class CaxtonModResourceLocationFlavor {
    public CaxtonModResourceLocationFlavor() {
        MinecraftForge.EVENT_BUS.addListener(CaxtonModResourceLocationFlavor::registerCommands);
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::addPackFinders);
        CaxtonModClient.init(new PlatformHooksResourceLocationFlavor());
    }

    public void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
            IModFile file = ModList.get().getModFileById(CaxtonModClient.MOD_ID).getFile();
            for (String id : CaxtonModClient.BUILTIN_PACKS) {
                Path sourcePath = file.findResource("resourcepacks/" + id);
                var pack = ResourcePackProfile.create(
                        CaxtonModClient.MOD_ID + ":" + id,
                        Text.translatable("caxton.resourcePack." + id),
                        false,
                        new ResourcePackProfile.PackFactory() {
                            @Override
                            public ResourcePack open(String name) {
                                return new DirectoryResourcePack(name, sourcePath, false);
                            }

                            @Override
                            public ResourcePack openWithOverlays(String name, ResourcePackProfile.Metadata metadata) {
                                return new DirectoryResourcePack(name, sourcePath, false);
                            }
                        },
                        ResourceType.CLIENT_RESOURCES,
                        ResourcePackProfile.InsertionPosition.TOP,
                        ResourcePackSource.BUILTIN);
                event.addRepositorySource(profileAdder -> profileAdder.accept(pack));
            }
        }
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        CaxtonCommands.register(new ClientCommandRegistrarResourceLocationFlavor(event.getDispatcher()));
    }
}
