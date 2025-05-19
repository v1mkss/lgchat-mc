package dev.v1mkss.lgchat.commands

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.message.{MessageType, SignedMessage}
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.{
  ServerPlayNetworkHandler,
  ServerPlayerEntity
}
import net.minecraft.text.{MutableText, Text, Style}
import net.minecraft.util.Formatting
import net.minecraft.world.World
import dev.v1mkss.lgchat.utils.Lang

import scala.jdk.CollectionConverters._
import dev.v1mkss.lgchat.LGChat
import dev.v1mkss.lgchat.utils.MessageKey
import net.minecraft.scoreboard.Team
import net.fabricmc.fabric.api.networking.v1.PacketSender

object ChatHandler {

  private def getPlayerNameColor(
      player: ServerPlayerEntity,
      defaultColor: Formatting
  ): Formatting = {
    val scoreboard = player.getScoreboard
    val teamOption: Option[Team] = Option(
      scoreboard.getPlayerTeam(player.getName().getString())
    )

    teamOption
      .map(_.getColor)
      .filter(color => color != Formatting.RESET && color.isColor)
      .getOrElse(defaultColor)
  }

  def register(): Unit = {
    ServerMessageEvents.ALLOW_CHAT_MESSAGE.register {
      (message, sender, params) =>
        onChatMessage(message, sender, params)
        false
    }
  }

  private def onChatMessage(
      message: SignedMessage,
      sender: ServerPlayerEntity,
      params: MessageType.Parameters
  ): Unit = {
    val rawMessage = message.getContent.getString

    val server: MinecraftServer = sender.getServer
    if (server == null) {
      LGChat.LOGGER.warn(
        s"Server was null for sender ${sender.getName.getString}, cannot process chat message."
      )
      return
    }

    val hasPrefix = rawMessage.startsWith("!")
    val prefersPrefixForGlobal =
      LGChat.getPlayerPrefersPrefixForGlobal(sender.getUuid)

    val (isEffectivelyGlobal: Boolean, finalMessageContent: String) = {
      if (hasPrefix) {
        (prefersPrefixForGlobal, rawMessage.substring(1).trim())
      } else {
        (!prefersPrefixForGlobal, rawMessage.trim())
      }
    }

    if (finalMessageContent.isEmpty) {
      return
    }

    if (isEffectivelyGlobal) {
      handleGlobalMessage(server, sender, finalMessageContent)
    } else {
      handleLocalMessage(server, sender, finalMessageContent)
    }

    LGChat.LOGGER.info(f"<${sender.getName.getString}%s> ${rawMessage}%s")
  }

  private def handleGlobalMessage(
      server: MinecraftServer,
      sender: ServerPlayerEntity,
      messageContent: String
  ): Unit = {
    val prefixColor = Formatting.YELLOW
    val defaultNameColor = Formatting.YELLOW
    val msgColor = Formatting.WHITE

    // Використовуємо Lang.get для отримання тексту символу чату
    val symbolText: Text = Lang.get(MessageKey.SymbolLangGlobalChat)

    val actualNameColor = getPlayerNameColor(sender, defaultNameColor)

    val formattedMessage: MutableText = Text
      .empty() // Починаємо з порожнього тексту
      .append(Text.literal("[").formatted(prefixColor)) // Відкриваюча дужка
      .append(
        symbolText.copy().setStyle(Style.EMPTY.withFormatting(prefixColor))
      ) // Символ чату з кольором префіксу
      .append(
        Text.literal("] ").formatted(prefixColor)
      ) // Закриваюча дужка і пробіл
      .append(
        sender.getDisplayName.copy().formatted(actualNameColor)
      ) // Ім'я гравця
      .append(Text.literal(": ").formatted(prefixColor)) // Розділювач
      .append(Text.literal(messageContent).formatted(msgColor)) // Повідомлення

    server.getPlayerManager.broadcast(
      formattedMessage,
      false
    )
  }

  private def handleLocalMessage(
      server: MinecraftServer,
      sender: ServerPlayerEntity,
      messageContent: String
  ): Unit = {
    val prefixColor = Formatting.GREEN
    val defaultNameColor = Formatting.GREEN
    val msgColor = Formatting.GRAY

    val symbolText: Text = Lang.get(MessageKey.SymbolLangLocalChat)

    val radiusSquared = LGChat.LOCAL_CHAT_RADIUS_SQUARED
    val actualNameColor = getPlayerNameColor(sender, defaultNameColor)

    val formattedMessage: MutableText = Text
      .empty()
      .append(Text.literal("[").formatted(prefixColor))
      .append(
        symbolText.copy().setStyle(Style.EMPTY.withFormatting(prefixColor))
      )
      .append(Text.literal("] ").formatted(prefixColor))
      .append(sender.getDisplayName.copy().formatted(actualNameColor))
      .append(Text.literal(": ").formatted(prefixColor))
      .append(Text.literal(messageContent).formatted(msgColor))

    val senderWorld: World = sender.getWorld
    val allPlayers =
      server.getPlayerManager.getPlayerList.asScala

    sender.sendMessage(formattedMessage, false)

    allPlayers
      .filterNot(_ == sender)
      .filter { recipient =>
        recipient.getWorld == senderWorld &&
        sender.squaredDistanceTo(recipient) <= radiusSquared
      }
      .foreach { recipient =>
        recipient.sendMessage(formattedMessage, false)
      }
  }

  private def onPlayerJoin(
      handler: ServerPlayNetworkHandler,
      sender: PacketSender,
      server: MinecraftServer
  ): Unit = {
    val player = handler.getPlayer
    val prefixColor = Formatting.GREEN
    val defaultNameColor =
      Formatting.YELLOW // Колір імені за замовчуванням для повідомлень про вхід/вихід

    val playerNameText = player.getDisplayName
      .copy()
      .formatted(getPlayerNameColor(player, defaultNameColor))

    val joinMessage: MutableText = Text
      .literal("[+] ")
      .formatted(prefixColor)
      .append(playerNameText)

    server.getPlayerManager.broadcast(
      joinMessage,
      false
    ) // false - не надсилати в action bar
    // За замовчуванням, ванільне повідомлення про вхід також буде показано.
    // Якщо ви хочете його прибрати, потрібні додаткові кроки (наприклад, через міксіни або `/gamerule announceAdvancements false`,
    // але останнє вимкне і інші системні повідомлення).
  }

  // Обробник відключення гравця
  private def onPlayerDisconnect(
      handler: ServerPlayNetworkHandler,
      server: MinecraftServer
  ): Unit = {
    val player = handler.getPlayer
    val prefixColor = Formatting.RED
    val defaultNameColor =
      Formatting.GREEN // Колір імені за замовчуванням для повідомлень про вхід/вихід

    val playerNameText = player.getDisplayName
      .copy()
      .formatted(getPlayerNameColor(player, defaultNameColor))

    val leaveMessage: MutableText = Text
      .literal("[-] ")
      .formatted(prefixColor)
      .append(playerNameText)

    server.getPlayerManager.broadcast(leaveMessage, false)
    // Аналогічно до onPlayerJoin, ванільне повідомлення про вихід також буде показано.
  }
}
