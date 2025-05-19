package dev.v1mkss.lgchat

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import dev.v1mkss.lgchat.utils.Lang
import dev.v1mkss.lgchat.LGChat.LOGGER
import java.util.UUID

object LGChat extends ModInitializer {
  val MOD_ID: String = "LGChat"
  def LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
  val LOCAL_CHAT_RADIUS: Double = 100
  val LOCAL_CHAT_RADIUS_SQUARED: Double = LOCAL_CHAT_RADIUS * LOCAL_CHAT_RADIUS

  val playerChatModePref: java.util.Map[UUID, java.lang.Boolean] =
    new java.util.HashMap[UUID, java.lang.Boolean]()

  def getPlayerPrefersPrefixForGlobal(playerUuid: java.util.UUID): Boolean =
    playerChatModePref.getOrDefault(playerUuid, true)


  override def onInitialize(): Unit = {
    LOGGER.info("LGChat is initializing...")
    Lang.initialize()
  }
}
