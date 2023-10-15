package xyz.flirora.caxton.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import xyz.flirora.caxton.CaxtonModClient;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.*;

public class CaxtonCommands {
    public static void register(ClientCommandRegistrar registrar) {
        CaxtonModClient.LOGGER.info("Registering client commands");
        registrar.registerClientCommand(CaxtonCommands::caxtonDumpAtlas, CommonDumpAtlasCommand::run);
    }

    // We have to define these methods because Java doesn’t allow lambdas for
    // generic methods.

    private static <S> LiteralArgumentBuilder<S> caxtonDumpAtlas() {
        return literal("caxtondumpatlas");
    }
}
