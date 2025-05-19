# LGChat

## Overview

LGChat is a Minecraft mod designed to enhance player communication by introducing distinct local and global chat channels. This mod leverages the Fabric API to provide a seamless and efficient chat experience, allowing players to communicate effectively within a specified radius or across the entire server.

## Key Features

- **Local Chat:** Enables proximity-based communication, fostering localized interactions among players.
- **Global Chat:** Facilitates server-wide communication, ensuring announcements and general discussions reach all players.
- **Chat Mode Preference:** Players can personalize their chat experience by setting a preferred chat mode, defaulting to global.
- **Language Localization:** Supports multiple languages for in-game messages, enhancing accessibility and user experience.
- **Team Color Integration:** Player names in chat messages are colored based on their scoreboard team, enhancing visual clarity and team identification.

## Installation

### Prerequisites

- Java 17 or higher
- Minecraft 1.20.1
- Fabric Loader (version >= 0.16.14)
- Fabric API (version >= 0.92.5+1.20.1)
- Fabric Language Scala (version >= 0.3.1)

### Setup Instructions

1.  Download the latest stable release of LGChat from the [GitHub Releases](link-to-releases) page.
2.  Place the downloaded `.jar` file into the `mods` directory of your Minecraft server or client installation.
3.  Start the Minecraft server or client with the Fabric Loader.

## Usage

### Chat Commands

LGChat provides a set of commands to manage chat preferences and language settings:

- `/lgchat local !`: Sets the chat mode to local, requiring the "!" prefix for local messages.
- `/lgchat global !`: Sets the chat mode to global, requiring the "!" prefix for global messages.
- `/lgchat lang <language>`: Sets the language for in-game messages. Supported languages: `en` (English), `uk` (Ukrainian).
- `/lgchat lang`: Displays the currently selected language.

### Configuration

The base mod configuration is located in `common/src/main/resources/fabric.mod.json`.

## Contributing

We welcome contributions to LGChat! If you have suggestions, bug reports, or would like to contribute code, please follow these guidelines:

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Submit a pull request with a clear description of your changes.

## License

LGChat is licensed under the [MIT License](LICENSE).
