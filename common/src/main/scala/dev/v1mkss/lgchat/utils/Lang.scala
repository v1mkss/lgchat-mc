package dev.v1mkss.lgchat.utils

import net.minecraft.text.{MutableText, Text}
import net.minecraft.util.Formatting
import scala.collection.mutable
import dev.v1mkss.lgchat.LGChat

// --- Enum Definitions ---
sealed trait Language {
  def code: String
  def displayName: String
}

object Language {
  case object EN extends Language {
    override val code = "en"
    override val displayName = "English"
  }
  case object UK extends Language {
    override val code = "uk"
    override val displayName = "Українська"
  }

  val values: Seq[Language] = Seq(EN, UK) // Defines all available languages

  // Find a language by its code, case-insensitive
  def fromCode(code: String): Option[Language] =
    values.find(_.code.equalsIgnoreCase(code))

  val Default: Language = UK // Set your default language here
}

sealed trait MessageKey {

  /** The original string identifier for this message key. */
  def id: String
}

object MessageKey {
  case object CommandErrorPlayerOnly extends MessageKey {
    val id = "command.error.player_only"
  }
  case object CommandSetPrefixLocal extends MessageKey {
    val id = "command.set.prefix_local"
  }
  case object CommandSetPrefixGlobal extends MessageKey {
    val id = "command.set.prefix_global"
  }
  case object CommandCurrentPrefixLocal extends MessageKey {
    val id = "command.current.prefix_local"
  }
  case object CommandCurrentPrefixGlobal extends MessageKey {
    val id = "command.current.prefix_global"
  }
  case object CommandLangSet extends MessageKey { val id = "command.lang.set" }
  case object CommandLangInvalid extends MessageKey {
    val id = "command.lang.invalid"
  }
  case object CommandLangCurrent extends MessageKey {
    val id = "command.lang.current"
  }
  case object SymbolLangGlobalChat extends MessageKey {
    val id = "chat.lang.global.symbol"
  }
  case object SymbolLangLocalChat extends MessageKey {
    val id = "chat.lang.local.symbol"
  }
  case object ErrorNotInTeam extends MessageKey {
    val id = "message.lgchat.error_not_in_team"
  }
  case object SymbolTeamChat extends MessageKey {
    val id = "message.lgchat.symbol_team_chat"
  }
  case object ErrorEmptyMessage extends MessageKey {
    val id = "message.lgchat.error_empty_message"
  }
  case object CommandTchatUsage extends MessageKey {
    val id = "commands.tchat.usage"
  }
}
// --- End Enum Definitions ---

object Lang {

  private var currentLanguage: Language = Language.Default

  // Stores all messages: Map[Language -> Map[MessageKey -> Text]]
  private val allMessages
      : mutable.Map[Language, mutable.Map[MessageKey, Text]] = mutable.Map.empty

  // Helper to simplify Text creation
  private def txt(literal: String, formats: Formatting*): MutableText = {
    Text.literal(literal).formatted(formats*)
  }

  def initialize(): Unit = {
    // Ensure there's an entry in allMessages for each defined language
    Language.values.foreach { lang =>
      allMessages.put(lang, mutable.Map.empty[MessageKey, Text])
    }

    // --- English Messages ---
    val enMap = allMessages(Language.EN) // Get the map for English
    enMap ++= Map(
      MessageKey.CommandErrorPlayerOnly ->
        txt("This command can only be run by a player.", Formatting.RED),
      MessageKey.CommandSetPrefixLocal ->
        txt("Chat mode set: ", Formatting.GRAY)
          .append(txt("Use '!' for Local chat", Formatting.GREEN))
          .append(txt(".", Formatting.GRAY)),
      MessageKey.CommandSetPrefixGlobal ->
        txt("Chat mode set: ", Formatting.GRAY)
          .append(txt("Use '!' for Global chat", Formatting.YELLOW))
          .append(txt(" (default).", Formatting.GRAY)),
      MessageKey.CommandCurrentPrefixLocal ->
        txt("Current mode: ", Formatting.GRAY)
          .append(txt("'!' means Local chat", Formatting.GREEN)),
      MessageKey.CommandCurrentPrefixGlobal ->
        txt("Current mode: ", Formatting.GRAY)
          .append(txt("'!' means Global chat", Formatting.YELLOW)),
      MessageKey.CommandLangSet ->
        txt("Language set to: ", Formatting.GRAY),
      MessageKey.CommandLangInvalid ->
        txt(
          s"Invalid language. Use one of: ${Language.values.map(_.code).mkString(", ")}.",
          Formatting.RED
        ),
      MessageKey.CommandLangCurrent ->
        txt("Current language: ", Formatting.GRAY),
      MessageKey.SymbolLangGlobalChat ->
        txt("G", Formatting.YELLOW),
      MessageKey.SymbolLangLocalChat ->
        txt("L", Formatting.GREEN),
      MessageKey.ErrorNotInTeam ->
        txt("You are not in a team.", Formatting.RED),
      MessageKey.SymbolTeamChat ->
        txt("T", Formatting.AQUA),
      MessageKey.ErrorEmptyMessage ->
        txt("Message cannot be empty.", Formatting.RED),
      MessageKey.CommandTchatUsage ->
        txt("Usage: /tchat <message>", Formatting.GRAY)
    )

    // --- Ukrainian Messages ---
    val ukMap = allMessages(Language.UK) // Get the map for Ukrainian
    ukMap ++= Map(
      MessageKey.CommandErrorPlayerOnly ->
        txt("Цю команду може використовувати лише гравець.", Formatting.RED),
      MessageKey.CommandSetPrefixLocal ->
        txt("Режим чату змінено: ", Formatting.GRAY)
          .append(
            txt("Використовуйте '!' для Локального чату", Formatting.GREEN)
          )
          .append(txt(".", Formatting.GRAY)),
      MessageKey.CommandSetPrefixGlobal ->
        txt("Режим чату змінено: ", Formatting.GRAY)
          .append(
            txt("Використовуйте '!' для Глобального чату", Formatting.YELLOW)
          )
          .append(txt(" (за замовчуванням).", Formatting.GRAY)),
      MessageKey.CommandCurrentPrefixLocal ->
        txt("Поточний режим: ", Formatting.GRAY)
          .append(txt("'!' означає Локальний чат", Formatting.GREEN)),
      MessageKey.CommandCurrentPrefixGlobal ->
        txt("Поточний режим: ", Formatting.GRAY)
          .append(txt("'!' означає Глобальний чат", Formatting.YELLOW)),
      MessageKey.CommandLangSet ->
        txt("Мову змінено на: ", Formatting.GRAY),
      MessageKey.CommandLangInvalid ->
        txt(
          s"Невірна мова. Використовуйте одну з: ${Language.values.map(_.code).mkString(", ")}.",
          Formatting.RED
        ),
      MessageKey.CommandLangCurrent ->
        txt("Поточна мова: ", Formatting.GRAY),
      MessageKey.SymbolLangGlobalChat ->
        txt("Г", Formatting.YELLOW),
      MessageKey.SymbolLangLocalChat ->
        txt("Л", Formatting.GREEN),
      MessageKey.ErrorNotInTeam ->
        txt("Ви не перебуваєте в команді.", Formatting.RED),
      MessageKey.SymbolTeamChat ->
        txt("К", Formatting.AQUA),
      MessageKey.ErrorEmptyMessage ->
        txt("Повідомлення не може бути порожнім.", Formatting.RED),
      MessageKey.CommandTchatUsage ->
        txt("Використання: /tchat <повідомлення>", Formatting.GRAY)
    )
  }

  def get(key: MessageKey): Text = {
    // Prioritize current language, then fallback, then default "missing" text.
    allMessages
      .get(currentLanguage)
      .flatMap(_.get(key)) // Option[Text] for current lang
      .orElse(
        allMessages.get(Language.EN).flatMap(_.get(key))
      ) // Option[Text] for EN fallback
      .getOrElse {
        LGChat.LOGGER.warn(
          s"Missing translation for key '${key.id}' in language '${currentLanguage.code}' and fallback '${Language.EN.code}'."
        )
        Text
          .literal(s"MISSING_TRANSLATION: ${key.id}")
          .formatted(Formatting.DARK_RED, Formatting.ITALIC)
      }
  }

  def getMutable(key: MessageKey): MutableText = {
    get(key).copy()
  }

  def setLang(langCode: String): Boolean = {
    Language.fromCode(langCode.toLowerCase) match {
      case Some(lang) =>
        currentLanguage = lang
        // Assumes LGChat.LOGGER is accessible (e.g., LGChat is an object or has a static-like logger)
        LGChat.LOGGER.info(
          s"LGChat language set to: ${currentLanguage.code} (${currentLanguage.displayName})"
        )
        true
      case None =>
        LGChat.LOGGER.warn(s"Attempted to set invalid language code: $langCode")
        false
    }
  }

  def getCurrentLanguage: Language = currentLanguage
  def getCurrentLanguageCode: String = currentLanguage.code
  def getSupportedLanguageDisplayNames: Seq[String] =
    Language.values.map(lang => s"${lang.displayName} (${lang.code})")
}
