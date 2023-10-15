package xyz.flirora.caxton.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;

public class ShapeCommand {
    public static int run(CommandContext<?> context, ClientCommandSource source) throws CommandSyntaxException {
        CaxtonModClient.LOGGER.info("font: " + context.getArgument("font", Identifier.class));
        CaxtonModClient.LOGGER.info("text: " + context.getArgument("text", String.class));
        source.sendError(Text.literal("ResourceLocation!"));
        return 1;
    }
}
