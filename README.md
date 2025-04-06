# TelegramAIConnector

Listens to telegram messages, answer using openai with MCPs.  

Required environment vars:  
TELEGRAM_BOT_KEY  
OPENAI_API_KEY  

## Build and run

```'
./gradlew build bootJar && java -jar build/libs/telegramAIConnector-0.0.1-SNAPSHOT.jar \
'--spring.ai.openai.api-key=YOUR_OPENAI_KEY' \
'--telegram.key=YOUR_TELEGRAM_KEY'
```  
Or set the env vars:  
```
TELEGRAM_BOT_KEY
OPENAI_API_KEY
``
 



