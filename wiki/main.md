# Telegram AI Connector

A bot running in java that answer telegram calls.
It supports MCPs, tools and has a backoffice to manage users and messages.

## The bot

`TelegramAiBot` connects to Telegram using the token defined in
`application.yaml`. Users must login with `/login PASSWORD` before sending other
commands. Messages, locations and uploaded files are forwarded to the AI
service through `AiBotService` and answered asynchronously. Standard commands
include `/version`, `/datetime`, `/profile`, `/listTasks` and more as shown in
the README.

## Configuring and running

To configure you need an application.yaml and a mcp-servers-config.json

You can see an example of the [applcation.yaml here](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/src/main/resources/application.yaml) and the [mcp configuration here](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/src/main/resources/mcp-servers-config.json)

To run, put the application yaml in the same folder as the jar and run with `java -jar pathtoJar`

### MCP configuration

The `spring.ai.mcp.client.stdio.servers-configuration` property points to the
JSON file describing the available MCP servers. Place your
`mcp-servers-config.json` beside the jar or provide a full path using the
`MCP_SERVERS_FILE` environment variable.

## Backoffice

You can access the backoffice at yourAddress:9996/backoffice

There you can manage users conversations, files and profiles.  

For more details, check [the backoffice page](./backoffice.md)

## Tools

* [FalAi tools](./falai-tools.md)
* [System tools](./system-tools.md)
* [Telegram tools](./telegram-tools.md)
* [AI analysis tool](./analysis-tool.md)


