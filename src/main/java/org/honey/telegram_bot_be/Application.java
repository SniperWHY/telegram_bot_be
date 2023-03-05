package org.honey.telegram_bot_be;

import org.honey.telegram_bot_be.bots.ChatSpecialist;
import org.honey.telegram_bot_be.common.PropertiesUtil;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.Objects;

public class Application {

    public static void main(String[] args) {
        boolean openProxy = Boolean.parseBoolean(Objects.requireNonNull(PropertiesUtil.getProperty("proxy.open")));
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            if (openProxy) {
                DefaultBotOptions botOptions = new DefaultBotOptions();
                String proxyHost = PropertiesUtil.getProperty("proxy.host");
                int proxyPort = Integer.parseInt(Objects.requireNonNull(PropertiesUtil.getProperty("proxy.port")));
                botOptions.setProxyHost(proxyHost);
                botOptions.setProxyPort(proxyPort);
                botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
                botsApi.registerBot(new ChatSpecialist(botOptions));
            } else {
                botsApi.registerBot(new ChatSpecialist());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
