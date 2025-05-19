package dev.v1mkss.lgchat

import dev.v1mkss.lgchat.commands.ChatModeCommand
import dev.v1mkss.lgchat.LGChat

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import dev.v1mkss.lgchat.commands.ChatHandler

@Environment(EnvType.SERVER)
object LGChatServer extends DedicatedServerModInitializer {

  override def onInitializeServer(): Unit = {
    LGChat.LOGGER.info("Initializing LGChat Server")
    ChatHandler.register()

    CommandRegistrationCallback.EVENT.register {
      (dispatcher, registryAccess, environment) =>
        ChatModeCommand.register(dispatcher, registryAccess, environment)
    }

    LGChat.LOGGER.info("Registered LGChat commands")
  }
}
