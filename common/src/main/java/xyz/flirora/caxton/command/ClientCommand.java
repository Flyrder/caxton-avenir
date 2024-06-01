// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface ClientCommand {
    // This takes `context` and `source` separately because you can’t replace
    // the source of a command context with one of a different type so easily.
    int run(CommandContext<?> context, ClientCommandSource source) throws CommandSyntaxException;
}
