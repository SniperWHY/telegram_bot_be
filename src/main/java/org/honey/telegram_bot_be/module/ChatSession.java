package org.honey.telegram_bot_be.module;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.honey.telegram_bot_be.services.Sender;
import org.honey.telegram_bot_be.services.UserService;
import org.telegram.telegrambots.meta.api.objects.Chat;

import java.util.*;

public class ChatSession {

    private final Long chatId; // 聊天id
    private String userName; // 用户名
    private Vector<ChatMessage> chatHistory; // 聊天历史
    private Boolean thinking; // 是否在思考中
    // 待处理消息队列
    private final Vector<ChatMessage> waitMsgQueue; // 待处理消息队列

    private final Sender<Long, String> sender; // 发送消息的函数

    private Date lastActiveTime; // 最后一次活跃时间

    public ChatSession(Chat chat, Sender<Long, String> sender) {
        this.chatHistory = new Vector<>();
        this.chatHistory.add(new ChatMessage("user", "my name is " + chat.getFirstName() + " " + chat.getLastName()));
        this.chatId = chat.getId();
        this.userName = chat.getUserName();
        this.sender = sender;
        this.waitMsgQueue = new Vector<>();
        this.thinking = false;
        this.lastActiveTime = new Date();
    }

    /**
     * 实现一个函数，用于处理用户发送的消息，但是有些消息还没有回复thinking为true,则将消息放入待处理消息队列
     * 如果thinking为false,则将消息放入聊天历史中并且请求openai api 获取回复，thinking设置为true 并且将回复放入聊天记录中
     * 处理后如果待处理消息队列不为空，则将第一个消息取出并且处理，直到待处理消息队列为空
     */
    public void handleMsg(ChatMessage msg) {
        this.lastActiveTime = new Date();
        if (this.thinking) {
            this.waitMsgQueue.add(msg);
            return;
        }
        this.chatHistory = ChatSession.filterMessages(this.chatHistory, msg.getContent());
        this.thinking = true;
        List<String> res = this.questOpenAI(this.chatHistory);
        for (String s : res) {
            this.chatHistory.add(new ChatMessage("assistant", s));
            this.sender.invoke(this.chatId, s);
        }
        this.thinking = false;
        if (!this.waitMsgQueue.isEmpty()) {
            this.handleMsg(this.waitMsgQueue.firstElement());
            this.waitMsgQueue.remove(0);
        }
    }

    /**
     * 销毁会话
     */
    public void destroy() {
        this.chatHistory.clear();
        this.waitMsgQueue.clear();
        this.sender.invoke(this.chatId, "The session has been destroy. If you have any questions, you can ask me at any time");
    }

    /**
     * 询问chatGPT 结果
     *
     * @param context 对话上下文
     * @return
     */
    private List<String> questOpenAI(Vector<ChatMessage> context) {
        List<String> res = new ArrayList<>();
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                // 代理设置
                .model("gpt-3.5-turbo")
                .messages(context)
                .build();

        ChatCompletionResult result = UserService.service.createChatCompletion(request);
        if (result == null) {
            res.add("Sorry, there was an error with the service, please try again later! 😭");
            return res;
        }
        result.getChoices().forEach(choice -> res.add(choice.getMessage().getContent()));
        return res;
    }

    public static Vector<ChatMessage> filterMessages(Vector<ChatMessage> messages, String text) {
        final int maxLength = 4096;
        int currentLength = messages.stream().mapToInt(msg -> msg.getContent().split(" ").length).sum();
        while (currentLength > maxLength) {
            int earliestAssistantIdx = -1;
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage msg = messages.get(i);
                if (msg.getRole().equals("assistant")) {
                    earliestAssistantIdx = i;
                    break;
                }
            }
            if (earliestAssistantIdx == -1) {
                messages.remove(0);
            } else {
                messages.subList(0, earliestAssistantIdx + 1).clear();
            }
            currentLength = messages.stream().mapToInt(msg -> msg.getContent().split(" ").length).sum();
        }
        if (currentLength + text.split(" ").length <= maxLength) {
            ChatMessage newMsg = new ChatMessage("user", text);
            messages.add(newMsg);
        }
        return messages;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(Date lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
}
