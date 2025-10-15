# Project Overview

This is a multi-module Maven project for a bot that automates tasks in the game "Whiteout Survival". The bot is written in Java and has a JavaFX GUI and a React-based web UI.

The project is structured as follows:
- `wos-hmi`: The main application module, containing the JavaFX GUI.
- `wos-utiles`: Utility classes.
- `wos-persitence`: Persistence layer for data storage.
- `wos-serv`: Service layer containing the core bot logic.
- `wos-web`: Web-related functionalities, likely providing a web server for the web UI.
- `wos-ot`: The purpose of this module is not immediately clear from its name.
- `wos-web-ui`: A React-based web UI for interacting with the bot.

# Building and Linting

## Prerequisites

- Java (JDK 17 or newer)
- Apache Maven
- Bun

## Building the project

To build the project, run the following command in the root directory:

```sh
mvn clean install package
```

This will create an executable JAR file in the `wos-hmi/target` directory.


## Linting the web UI

To lint the web UI, navigate to the `wos-web-ui` directory and run:

```sh
bun install
bun run lint
```

# Development Conventions

- The project uses Maven for dependency management and building.
- The Java code follows standard Java conventions.
- The web UI is a standard React project using Vite.
- The project uses SLF4J and Logback for logging.