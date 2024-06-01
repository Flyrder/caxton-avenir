// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.command;

/** Hooks for platform-independent registration of client commands. */
public interface ClientCommandRegistrar {
  /**
   * Registers a new client command.
   *
   * @param builder A {@link GenericArgumentBuilderFactory} that creates a literal argument builder
   *     with the correct type parameters.
   * @param command A {@link ClientCommand} that executes the command.
   */
  void registerClientCommand(GenericArgumentBuilderFactory builder, ClientCommand command);
}
