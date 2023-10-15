package xyz.flirora.caxton.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;

public class ShapeCommand {
    public static Either<Text, Text> run(CommandContext<?> ctx, MinecraftClient client) {
        CaxtonModClient.LOGGER.info("font: " + ctx.getArgument("font", Identifier.class));
        CaxtonModClient.LOGGER.info("text: " + ctx.getArgument("text", String.class));
        return Either.left(Text.literal("ResourceLocation!"));
    }
}
