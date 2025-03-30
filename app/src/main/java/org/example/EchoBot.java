package org.example;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

public class EchoBot implements LongPollingSingleThreadUpdateConsumer {

    private TelegramClient telegramClient;
    private final String apiToken;

    public EchoBot(String token, String apiToken) {
        telegramClient = new OkHttpTelegramClient(token);
        this.apiToken = apiToken;
    }

    @Override
    public void consume(final List<Update> updates) {
        System.out.println("Consume list");
        updates.forEach(this::consume);
    }

    @Override
    public void consume(final Update update) {
        System.out.println("Consume");
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            System.out.println(text);

            String chatId = update.getMessage().getChatId() + "";

            OpenAIClient client = new OpenAIOkHttpClient.Builder()
                    .apiKey(apiToken)
//                    .baseUrl("https://generativelanguage.googleapis.com/v1beta") // https://generativelanguage.googleapis.com/v1beta/models
                    .build();

            try {
                ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                        .addUserMessage(text)
                        .model(ChatModel.GPT_4O_MINI)
                        .build();
                ChatCompletion chatCompletion = client.chat().completions().create(params);

                SendMessage sendMessage = new SendMessage(chatId, chatCompletion.choices().get(0).message().content().orElse("Could not get answer"));
                try {
                    telegramClient.execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }catch (Exception e) {
                System.out.println(e.getMessage());
                SendMessage sendMessage = new SendMessage(chatId, e.getMessage());
                try {
                    telegramClient.execute(sendMessage);
                } catch (TelegramApiException te) {
                    te.printStackTrace();
                }
            }
        }
    }
}
