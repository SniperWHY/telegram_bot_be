package org.honey.telegram_bot_be.services;

import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.honey.telegram_bot_be.common.PropertiesUtil;
import org.honey.telegram_bot_be.module.ChatSession;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    // 线程安全的Map 用于存储用户与机器人的对话
    private static final Map<String, ChatSession> conversationMap = new ConcurrentHashMap<>();

    private final static int MAX_SESSION_LEN = Integer.parseInt(Objects.requireNonNull(PropertiesUtil.getProperty("max.session.len")));

    public final static OpenAiService service = new OpenAiService(PropertiesUtil.getProperty("openai.key"));

    public void handleUpdate(Update update, Sender<Long, String> sender) {
        Message message = update.getMessage();
        String text = message.getText();
        if (text == null) // 只支持文本消息
            sender.invoke(message.getChatId(), "Sorry, only text messages are supported right now 😭");

        ChatSession session = this.addSession(message, sender);
        if (session != null) session.handleMsg(new ChatMessage("user", text));
    }

    public ChatSession addSession(Message message, Sender<Long, String> sender) {
        ChatSession session = conversationMap.get(message.getChat().getUserName());
        if (session == null) {
            session = new ChatSession(message.getChat(), sender);

            // 如果到达最大长度
            if (conversationMap.size() >= MAX_SESSION_LEN) {
                // 移除最早的会话
                String oldestSessionId = null;
                Date oldestSessionDate = null;
                for (Map.Entry<String, ChatSession> _session : conversationMap.entrySet()) {
                    if (oldestSessionDate == null || _session.getValue().getLastActiveTime().before(oldestSessionDate)) {
                        oldestSessionDate = _session.getValue().getLastActiveTime();
                        oldestSessionId = _session.getKey();
                    }
                }
                conversationMap.remove(oldestSessionId);
                this.dispose(oldestSessionId);
            }
            conversationMap.put(message.getChat().getUserName(), session);
            sender.invoke(message.getChatId(), "Hi, " + message.getChat().getFirstName() + ", I can help you to complete your chat. Please send me a message to start.");
            return null;
        }
        return session;
    }

    public void dispose(String userName) {
        ChatSession session = conversationMap.remove(userName);
        if (session != null)
            session.destroy();
    }
}
