package org.example;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class App {
    public static void main(String[] args) {
        try {
            String botToken = args[0];
            String aiToken = args[1];
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new EchoBot(botToken, aiToken));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
