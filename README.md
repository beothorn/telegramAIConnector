# TelegramAIConnector

Listens to telegram messages, answer using openai with MCPs.

![cute mascot](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/logo.svg)

# Getting started

Create a bot with telegram [BotFather](https://telegram.me/BotFather) and copy the bot token.  
It only supports open AI for now. You will need an [openAi key](https://platform.openai.com/).  
Get java 21+

Download the [instalation zip](https://github.com/beothorn/telegramAIConnector/releases/download/2.0.0/telegramAIConnector_2_0_0.zip)   
, extract it and fill the values on `application.yml` and `mcp-servers-config.json`.  
Then just execute the jar with `java -jar telegramAIConnector.jar`.

On telegram, send the login command to the bot:  
```
/login YOURPASSWORD
```

Have fun!  

# Features

- Schedule reminders and commands
- Get messages from a http endpoint (optional)
- Upload and download files
- Get the current time