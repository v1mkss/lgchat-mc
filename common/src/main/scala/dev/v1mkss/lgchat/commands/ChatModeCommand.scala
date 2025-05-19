package dev.v1mkss.lgchat.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.v1mkss.lgchat.utils.{Lang, Language, MessageKey}
import dev.v1mkss.lgchat.LGChat

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

object ChatModeCommand {

  def register(
      dispatcher: CommandDispatcher[ServerCommandSource],
      registryAccess: CommandRegistryAccess,
      environment: RegistrationEnvironment
  ): Unit = {
    // Create the root builder first
    val rootCommandBuilder = mcLiteral("lgchat")

    // Apply .then and .executes to this val
    rootCommandBuilder
      .`then`(
        mcLiteral("local").`then`(
          mcLiteral("!").executes(setModeLocal)
        )
      )
      .`then`(
        mcLiteral("global").`then`(
          mcLiteral("!").executes(setModeGlobal)
        )
      )
      .`then`(
        mcLiteral("lang")
          .`then`(
            mcArgument("language", StringArgumentType.word())
              .suggests { (ctx, builder) =>
                Language.values
                  .foreach(langEnum => builder.suggest(langEnum.code))
                builder.buildFuture()
              }
              .executes(setLanguage)
          )
          .executes(showCurrentLanguage)
      )
      .executes(showCurrentMode)

    dispatcher.register(rootCommandBuilder)
  }

  private def executePlayerCommand(
      context: CommandContext[ServerCommandSource]
  )(action: ServerPlayerEntity => Int): Int = {
    val source = context.getSource
    source.getEntity match {
      case player: ServerPlayerEntity =>
        action(player)
      case _ =>
        source.sendError(Lang.get(MessageKey.CommandErrorPlayerOnly))
        0 // Command failure
    }
  }

  private def setModeLocal(context: CommandContext[ServerCommandSource]): Int =
    executePlayerCommand(context) { player =>
      LGChat.playerChatModePref.put(player.getUuid, false)
      context.getSource.sendFeedback(
        () => Lang.get(MessageKey.CommandSetPrefixLocal),
        false
      )
      1
    }

  private def setModeGlobal(context: CommandContext[ServerCommandSource]): Int =
    executePlayerCommand(context) { player =>
      LGChat.playerChatModePref.put(player.getUuid, true)
      context.getSource.sendFeedback(
        () => Lang.get(MessageKey.CommandSetPrefixGlobal),
        false
      )
      1
    }

  private def showCurrentMode(
      context: CommandContext[ServerCommandSource]
  ): Int =
    executePlayerCommand(context) { player =>
      val prefersPrefixForGlobal =
        LGChat.getPlayerPrefersPrefixForGlobal(player.getUuid)
      val feedbackKey =
        if (prefersPrefixForGlobal) MessageKey.CommandCurrentPrefixGlobal
        else MessageKey.CommandCurrentPrefixLocal
      context.getSource.sendFeedback(() => Lang.get(feedbackKey), false)
      1
    }

  private def setLanguage(context: CommandContext[ServerCommandSource]): Int = {
    val langArg = StringArgumentType.getString(context, "language")
    val source = context.getSource
    if (Lang.setLang(langArg)) {
      val langDisplayName = Language
        .fromCode(langArg)
        .map(_.displayName)
        .getOrElse(langArg.toUpperCase)
      val feedback = Lang
        .getMutable(MessageKey.CommandLangSet)
        .append(
          Text.literal(langDisplayName).formatted(Formatting.AQUA)
        )
      source.sendFeedback(() => feedback, true)
      1
    } else {
      source.sendError(Lang.get(MessageKey.CommandLangInvalid))
      0
    }
  }

  private def showCurrentLanguage(
      context: CommandContext[ServerCommandSource]
  ): Int = {
    val source = context.getSource
    val currentLangDisplay = Lang.getCurrentLanguage.displayName
    val feedback = Lang
      .getMutable(MessageKey.CommandLangCurrent)
      .append(
        Text.literal(currentLangDisplay).formatted(Formatting.AQUA)
      )
    source.sendFeedback(() => feedback, false)
    1
  }
}
