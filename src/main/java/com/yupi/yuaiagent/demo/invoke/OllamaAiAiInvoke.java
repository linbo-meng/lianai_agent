package com.yupi.yuaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring AI 框架 调用AI大模型（ollama）
 */
@Component
public class OllamaAiAiInvoke implements CommandLineRunner {
    @Resource
    private ChatModel ollamaChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = ollamaChatModel.call(new Prompt("你好"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());



    }
}
