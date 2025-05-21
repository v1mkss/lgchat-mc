package dev.v1mkss.lgchat.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.v1mkss.lgchat.utils.{Lang, MessageKey}
import dev.v1mkss.lgchat.commands.ChatHandler
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager.{
  literal => mcLiteral,
  argument => mcArgument,
  RegistrationEnvironment
}
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.scoreboard.Team

object TeamChatCommand {

  def register(
      dispatcher: CommandDispatcher[ServerCommandSource],
      registryAccess: CommandRegistryAccess,
      environment: RegistrationEnvironment
  ): Unit = {
    dispatcher.register(
      mcLiteral("tchat")
        .requires(source =>
          source.isExecutedByPlayer
        ) // Command for players only
        .`then`(
          mcArgument(
            "message",
            StringArgumentType.greedyString()
          ) // Allows messages with spaces
            .executes(executeTeamChat)
        )
        .executes { context => // Handling the case where no message is provided
          context.getSource.sendError(
            Lang
              .get(MessageKey.CommandTchatUsage)
          ) // Using translation key for usage message
          0
        }
    )
  }

  private def executeTeamChat(
      context: CommandContext[ServerCommandSource]
  ): Int = {
    val source = context.getSource
    // getPlayerOrThrow will throw an error if the executor is not a player, but .requires already checks this
    val sender = source.getPlayerOrThrow()
    val messageContent = StringArgumentType.getString(context, "message")

    if (messageContent.trim.isEmpty) {
      source.sendError(
        Lang.get(MessageKey.ErrorEmptyMessage)
      ) // Key for empty message
      return 0
    }

    val senderTeamOpt: Option[Team] = Option(
      sender.getScoreboard.getPlayerTeam(sender.getName.getString)
    )

    senderTeamOpt match {
      case Some(team) =>
        // Calling the new method in ChatHandler to send the message to the team
        ChatHandler.handleTeamMessage(
          sender.getServer,
          sender,
          team,
          messageContent
        )
        1 // Command successful
      case None =>
        source.sendError(
          Lang.get(MessageKey.ErrorNotInTeam)
        ) // Key for "not in team"
        0 // Command failed
    }
  }
}
