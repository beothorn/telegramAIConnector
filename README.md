# TelegramAIConnector

Listens to telegram messages, answer using openai with MCPs.

![cute mascot](https://raw.githubusercontent.com/beothorn/telegramAIConnector/refs/heads/main/logo.svg)

Required environment vars:  
TELEGRAM_BOT_KEY  
OPENAI_API_KEY  
CHAT_PASSWORD  
MCP_SERVERS_FILE  (example file:/home/you/servers.json)  

Start with
```
/login YOURPASSWORD
``

## Build and run

```
./gradlew build bootJar && java -jar build/libs/telegramAIConnector-0.0.1-SNAPSHOT.jar \
'--spring.ai.openai.api-key=YOUR_OPENAI_KEY' \
'--telegram.key=YOUR_TELEGRAM_KEY' \
'--telegram.password=YOUR_PASSWORD_TO_USE_THE_BOT'
```  
Or set the env vars:  
```
export TELEGRAM_BOT_KEY=''  
export OPENAI_API_KEY=''   
export CHAT_PASSWORD=''  

java -jar build/libs/telegramAIConnector-0.0.1-SNAPSHOT.jar
```