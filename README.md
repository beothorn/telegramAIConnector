# TelegramAIConnector

Listens to telegram messages, answer using openai with MCPs.

![cute mascot](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/logo.svg)

# Getting started

Create a bot with telegram [BotFather](https://telegram.me/BotFather) and copy the bot token.  
It only supports open AI for now. You will need an [openAi key](https://platform.openai.com/).  
Get java 21+

Download the [instalation zip](https://github.com/beothorn/telegramAIConnector/releases/latest)   
, extract it and fill the values on `application.yml` and `mcp-servers-config.json`.  
Then just execute the jar with `java -jar telegramAIConnector.jar`.

On telegram, send the login command to the bot:  
```
/login YOURPASSWORD
```

# Commands

These commands can be called directly, but are also available to the bot.

```
Login to do anything
/login password
Show the chatId, same id to use when calling the systemMessage endpoint
/chatId
Get the bot version
/version
Get the server datetime
/datetime
List the uploaded files
/list
Delete files from the uploaded files
/delete file
Reads a file from the uploaded files
/read file
Sends the file from the uploaded files
/send file
List the scheduled tasks
/listTasks
Show the saved user profile
/profile
Replace the current profile
/newProfile profile text
```

# HTTP messages (optional)

Posting to localhost:9996/systemMessage will send a system message prompt.  

`curl -X POST "http://localhost:9996/systemMessage" -d "chatId=123456789" -d "message=Computer was rebooted."`

# Recommended MCPs

## Google maps

[To take advantage of the location, add the google maps mcp](https://github.com/modelcontextprotocol/servers/tree/main/src/google-maps)  
[Add fetch capabilities](https://github.com/modelcontextprotocol/servers/tree/main/src/fetch)

# Features

- Schedule reminders and commands
- Get messages from a http endpoint (optional)
- Upload and download files
- Rename uploaded files
- Get the current time
- Manage conversations, tasks, profiles and files at /backoffice with a nicer web interface
