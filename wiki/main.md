# Telegram AI Connector

A bot running in java that answer telegram calls.  
It supports MCPs, tools and has a backoffice to manage users and messages.  

## Configuring and running

To configure you need an application.yaml and a mcp-servers-config.json  

You can see an example of the [applcation.yaml here](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/src/main/resources/application.yaml) and the [mcp configuration here](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/src/main/resources/mcp-servers-config.json)  

To run, put the application yaml in the same folder as the jar and run with `java -jar pathtoJar` 

## Backoffice

You can access the backoffice at yourAddress:9996/backoffice  

There you can manage users conversations, files and profiles.  

For more details, check [the backoffice page](./backoffice.md)


