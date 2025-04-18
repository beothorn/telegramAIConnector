# TelegramAIConnector

Listens to telegram messages, answer using openai with MCPs.

![cute mascot](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/logo.svg)

# Getting started

Create a bot with telegram [BotFather](https://telegram.me/BotFather) and copy the bot token.  
It only supports open AI for now. You will need an [openAi key](https://platform.openai.com/).  

Create an mcp server config file on your disk. You can see [an example here](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/src/main/resources/mcp-servers-config.json)

Copy the application [configuration properties](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/src/main/resources/application.yaml) and replace the values.  

Download the [latest release](https://github.com/beothorn/telegramAIConnector/releases/download/0.0.1-SNAPSHOT/telegramAIConnector-0.0.1-SNAPSHOT.jar) and java 21+  

Run the application with:  
```
java -jar telegramAIConnector.jar --spring.config.location=file:/path/to/application.yaml
```

On telegram, send the login command to the bot:  
```
/login YOURPASSWORD
```

Have fun!  