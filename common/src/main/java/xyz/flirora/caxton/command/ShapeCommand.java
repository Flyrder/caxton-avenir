package xyz.flirora.caxton.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.CaxtonFontLoader;

public class ShapeCommand {
    private static final DynamicCommandExceptionType FONT_NOT_FOUND = new DynamicCommandExceptionType(id -> Text.translatable("caxton.command.caxtonshape.fontNotFound", id));

    public static int run(CommandContext<?> context, ClientCommandSource source) throws CommandSyntaxException {
        Identifier fontId = context.getArgument("font", Identifier.class);
        String text = context.getArgument("text", String.class);
        CaxtonFont font = CaxtonFontLoader.getFontById(fontId);
        if (font == null) {
            throw FONT_NOT_FOUND.create(fontId);
        }

        DebugShapeInfo dsi = font.shapeForDebug(text);
        source.sendFeedback(() -> Text.translatable("caxton.command.caxtonshape.shapingBuffer", dsi.buffer));
        source.sendFeedback(() -> Text.translatable("caxton.command.caxtonshape.language", dsi.inferredLanguage));
        source.sendFeedback(() -> Text.translatable("caxton.command.caxtonshape.script", dsi.inferredScript));
        source.sendFeedback(() -> Text.translatable("caxton.command.caxtonshape.direction", dsi.inferredDirection));
        return 1;
    }
}
