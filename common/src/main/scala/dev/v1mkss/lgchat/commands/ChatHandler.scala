package dev.v1mkss.lgchat.commands

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.message.{MessageType, SignedMessage}
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.{
  ServerPlayNetworkHandler,
  ServerPlayerEntity
}
import net.minecraft.text.{MutableText, Text, Style, TranslatableTextContent}
import net.minecraft.world.World
import dev.v1mkss.lgchat.utils.Lang

import scala.jdk.CollectionConverters._
import dev.v1mkss.lgchat.LGChat
import dev.v1mkss.lgchat.utils.MessageKey
import net.minecraft.scoreboard.Team
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.util.Formatting

object ChatHandler {

  private val TEAM_CHAT_PREFIX: String = "%" // Define team chat prefix

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
    // Handle chat messages manually
    ServerMessageEvents.ALLOW_CHAT_MESSAGE.register {
      (message, sender, params) => // params here IS MessageType.Parameters
        onChatMessage(message, sender, params)
        false // Prevent the default chat message handling
    }

    // Suppress default join/leave messages
    ServerMessageEvents.GAME_MESSAGE.register {
      (server, message, overlay) => // overlay here IS Boolean
        onGameMessage(server, message, overlay) // Pass overlay
    }

    // Register player join event handler (sends custom message)
    ServerPlayConnectionEvents.JOIN.register {
      (handler, packetSender, server) =>
        onPlayerJoin(handler, packetSender, server)
    }

    // Register player disconnect event handler (sends custom message)
    ServerPlayConnectionEvents.DISCONNECT.register { (handler, server) =>
      onPlayerDisconnect(handler, server)
    }
  }

  // Intercept game messages (including join/leave)
  private def onGameMessage(
      server: MinecraftServer,
      message: Text,
      overlay: Boolean // Changed from MessageType.Parameters to Boolean
  ): Text = {
    // Suppress default join and leave messages by checking their content.
    // The 'overlay' parameter indicates if it's an action bar message.
    // Vanilla join/leave messages are not overlay messages.

    message.getContent match {
      case ttc: TranslatableTextContent =>
        val key = ttc.getKey
        // Check for standard vanilla join/leave message keys
        if (
          key == "multiplayer.player.joined" ||
          key == "multiplayer.player.left" ||
          key == "multiplayer.player.joined.renamed"
        ) {
          // This is a default join/leave message, suppress it by returning null.
          return null
        }
      case _ =>
      // The message content is not TranslatableTextContent,
      // or it's a TranslatableTextContent with a different key.
      // Do nothing, allow the message.
    }
    // Allow other game messages that don't match the suppression criteria.
    message
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

    if (rawMessage.trim.isEmpty) {
      // Don't process empty messages
      return
    }

    // Check if the sender is in a team and the message starts with the team chat prefix
    val senderTeamOption: Option[Team] = Option(
      sender.getScoreboard.getPlayerTeam(sender.getName.getString)
    )

    if (rawMessage.startsWith(TEAM_CHAT_PREFIX) && senderTeamOption.isDefined) {
      val teamMessageContent =
        rawMessage.substring(TEAM_CHAT_PREFIX.length).trim
      if (teamMessageContent.nonEmpty) {
        handleTeamMessage(
          server,
          sender,
          senderTeamOption.get,
          teamMessageContent
        )
      }
      // Regardless of whether content was empty, consume the message if it started with team prefix
      return
    }

    // If not team chat, proceed with global/local logic
    val forceGlobalPrefix = "!" // Existing prefix for forcing global
    val hasForceGlobalPrefix = rawMessage.startsWith(forceGlobalPrefix)
    val prefersPrefixForGlobal =
      LGChat.getPlayerPrefersPrefixForGlobal(sender.getUuid)

    val (isEffectivelyGlobal: Boolean, finalMessageContent: String) = {
      if (hasForceGlobalPrefix) {
        // If force global prefix is used, it's global, remove prefix
        (true, rawMessage.substring(forceGlobalPrefix.length).trim())
      } else {
        // No force global prefix, use player preference
        (prefersPrefixForGlobal, rawMessage.trim())
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

    // Log the original raw message to the server console
    // Note: Team messages are logged separately in handleTeamMessage
    if (!rawMessage.startsWith(TEAM_CHAT_PREFIX)) { // Only log non-team messages here
      LGChat.LOGGER.info(f"<${sender.getName.getString}%s> ${rawMessage}%s")
    }
  }

  private def handleGlobalMessage(
      server: MinecraftServer,
      sender: ServerPlayerEntity,
      messageContent: String
  ): Unit = {
    val prefixColor = Formatting.YELLOW
    val defaultNameColor = Formatting.YELLOW
    val msgColor = Formatting.WHITE

    val symbolText: Text = Lang.get(MessageKey.SymbolLangGlobalChat)
    val actualNameColor = getPlayerNameColor(sender, defaultNameColor)

    val formattedMessage: MutableText = Text
      .empty()
      .append(Text.literal("[").formatted(prefixColor))
      .append(
        symbolText.copy().setStyle(Style.EMPTY.withFormatting(prefixColor))
      )
      .append(Text.literal("] ").formatted(prefixColor))
      // Explicitly set style for player name to prevent color bleeding
      .append(
        sender.getDisplayName
          .copy()
          .setStyle(Style.EMPTY.withFormatting(actualNameColor))
      )
      .append(Text.literal(": ").formatted(prefixColor))
      .append(Text.literal(messageContent).formatted(msgColor))

    server.getPlayerManager.broadcast(formattedMessage, false)
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
      // Explicitly set style for player name to prevent color bleeding
      .append(
        sender.getDisplayName
          .copy()
          .setStyle(Style.EMPTY.withFormatting(actualNameColor))
      )
      .append(Text.literal(": ").formatted(prefixColor))
      .append(Text.literal(messageContent).formatted(msgColor))

    val senderWorld: World = sender.getWorld
    val allPlayers = server.getPlayerManager.getPlayerList.asScala

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

  def handleTeamMessage(
      server: MinecraftServer,
      sender: ServerPlayerEntity,
      senderTeam: Team,
      messageContent: String
  ): Unit = {
    val teamChatColorConfig = senderTeam.getColor
    // Use the team's color for the prefix, or the default color if the team color is not set
    val prefixActualColor =
      if (
        teamChatColorConfig != Formatting.RESET && teamChatColorConfig.isColor
      )
        teamChatColorConfig
      else
        Formatting.AQUA // Default color for team chat (can be changed)

    val defaultNameColor =
      prefixActualColor // Default name color can also be based on the team color
    val msgColor = Formatting.WHITE

    val symbolText: Text = Lang.get(MessageKey.SymbolTeamChat)

    // getPlayerNameColor will determine the player's actual name color, taking into account their team's color
    val actualNameColor = getPlayerNameColor(sender, defaultNameColor)

    val formattedMessage: MutableText = Text
      .empty()
      .append(Text.literal("[").formatted(prefixActualColor))
      .append(
        symbolText
          .copy()
          .setStyle(Style.EMPTY.withFormatting(prefixActualColor))
      )
      .append(Text.literal("] ").formatted(prefixActualColor))
      // Explicitly set style for player name to prevent color bleeding
      .append(
        sender.getDisplayName
          .copy()
          .setStyle(Style.EMPTY.withFormatting(actualNameColor))
      )
      .append(Text.literal(": ").formatted(prefixActualColor))
      .append(Text.literal(messageContent).formatted(msgColor))

    val teamName = senderTeam.getName // For filtering
    val teamMembers = server.getPlayerManager.getPlayerList.asScala
      .filter { player =>
        Option(player.getScoreboard.getPlayerTeam(player.getName.getString))
          .exists(
            _.getName == teamName
          ) // Check if the player belongs to the same team
      }

    teamMembers.foreach { member =>
      member.sendMessage(formattedMessage, false)
    }

    // Log team chat to the server console
    LGChat.LOGGER.info(
      f"[TEAMCHAT:${teamName}] <${sender.getName.getString}%s> ${messageContent}%s"
    )
  }

  private def onPlayerJoin(
      handler: ServerPlayNetworkHandler,
      packetSender: PacketSender,
      server: MinecraftServer
  ): Unit = {
    val player = handler.getPlayer
    val prefixColor = Formatting.GREEN
    val defaultNameColor = Formatting.YELLOW

    val playerNameText = player.getDisplayName
      .copy()
      .formatted(getPlayerNameColor(player, defaultNameColor))

    val joinMessage: MutableText = Text
      .literal("[+] ")
      .formatted(prefixColor)
      .append(playerNameText)

    server.getPlayerManager.broadcast(joinMessage, false)
  }

  private def onPlayerDisconnect(
      handler: ServerPlayNetworkHandler,
      server: MinecraftServer
  ): Unit = {
    val player = handler.getPlayer
    val prefixColor = Formatting.RED
    val defaultNameColor = Formatting.YELLOW

    val playerNameText = player.getDisplayName
      .copy()
      .formatted(getPlayerNameColor(player, defaultNameColor))

    val leaveMessage: MutableText = Text
      .literal("[-] ")
      .formatted(prefixColor)
      .append(playerNameText)

    server.getPlayerManager.broadcast(leaveMessage, false)
  }
}
