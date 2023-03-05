package org.honey.telegram_bot_be.bots;

import org.honey.telegram_bot_be.common.PropertiesUtil;
import org.honey.telegram_bot_be.services.Sender;
import org.honey.telegram_bot_be.services.UserService;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class ChatSpecialist extends TelegramLongPollingBot {

    private static final String BOT_USERNAME = PropertiesUtil.getProperty("bot.username");

    private static final String BOT_TOKEN = PropertiesUtil.getProperty("bot.token");

    private final UserService userService = new UserService();

    public ChatSpecialist(DefaultBotOptions defaultBotOptions) {
        super(defaultBotOptions);
    }

    public ChatSpecialist() {
    }

    @Override
    public String getBotUsername() {
        return ChatSpecialist.BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return ChatSpecialist.BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            Sender<Long, String> sender = (chatId, text) -> {
                try {
                    execute(this.genResponseMessage(chatId, text));
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            };

            switch (message) {
                case "/start" -> this.userService.addSession(update.getMessage(), sender);
                case "/clear" -> userService.dispose(update.getMessage().getChat().getUserName());
                default -> userService.handleUpdate(update, sender);
            }
        } else {
            try {
                execute(this.genResponseMessage(update.getMessage().getChatId(), "Sorry, only text messages are supported right now 😭"));
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private SendMessage genResponseMessage(Long chatId, String text) {
        SendMessage chatCompletionRequestMessage = new SendMessage();
        chatCompletionRequestMessage.setChatId(chatId);
        chatCompletionRequestMessage.setText(text);
        chatCompletionRequestMessage.enableMarkdown(true);
        return chatCompletionRequestMessage;
    }
}
