package xyz.flirora.caxton.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;

public class CaxtonCommands {
    public static void register(ClientCommandRegistrar registrar) {
        CaxtonModClient.LOGGER.info("Registering client commands");
        registrar.registerClientCommand(CaxtonCommands::caxtonDumpAtlas, DumpAtlasCommand::run);
        registrar.registerClientCommand(CaxtonCommands::caxtonShape, ShapeCommand::run);
    }

    // We have to define these methods because Java doesn’t allow lambdas for
    // generic methods.

    private static <S> LiteralArgumentBuilder<S> caxtonDumpAtlas(Command<S> command) {
        return LiteralArgumentBuilder.<S>literal("caxtondumpatlas").executes(command);
    }

    private static <S> LiteralArgumentBuilder<S> caxtonShape(Command<S> command) {
        return LiteralArgumentBuilder.<S>literal("caxtonshape")
                .then(RequiredArgumentBuilder.<S, Identifier>argument("font", IdentifierArgumentType.identifier())
                        .suggests((SuggestionProvider<S>) CaxtonSuggestionProviders.CAXTON_FONTS)
                        .then(RequiredArgumentBuilder.<S, String>argument("text", StringArgumentType.greedyString())
                                .executes(command)));
    }
}
