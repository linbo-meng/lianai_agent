package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.RAG.LoveAppRagCustomAdvisorFactory;
import com.yupi.yuaiagent.RAG.QueryRewriter;
import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.advisor.ReReadingAdvisor;
import com.yupi.yuaiagent.chatmemory.JdbcChatMemory;
import com.yupi.yuaiagent.chatmemory.repository.ChatConversationRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LoveApp {
    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    /**
     * 初始化Ai客户端
     * @param dashscopeChatModel
     */
    public LoveApp(ChatModel dashscopeChatModel,
                   ChatConversationRepository chatConversationRepository) {
        // 初始化基于 MySQL 的对话记忆
        ChatMemory chatMemory = new JdbcChatMemory(chatConversationRepository);

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        //自定义日志advisor，可按需开启
                        new MyLoggerAdvisor()
                        //自定义推理增强 advisor，可按需开启
                        //  ,new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * Ai基础对话（支持多轮对话内容）
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message , String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec
                        .param("chat_memory_conversation_id", chatId)
                        .param("chat_memory_retrieve_size", 10)
                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }
    record LoveReport(String title, List<String> suggestions) {}

    /**
     * AI 恋爱报告功能（实战结构化输出）
     * @param message
     * @param chatId
     * @return
     */

    public LoveReport doChatWithReport(String message , String chatId) {
       LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT+"每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec
                        .param("chat_memory_conversation_id", chatId)
                        .param("chat_memory_retrieve_size", 10)
                )
                .call()
                .entity(LoveReport.class);
       log.info("loveReport:{}", loveReport);
        return loveReport;
}
//AI 恋爱知识库问答功能
    @Resource
    private VectorStore LoveAppvectorStore;


    @Resource
    private Advisor LoveAppRagCloudAdvisor;

    @Resource
    private VectorStore PgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message , String chatId) {
        //查询重写
        String reWriteMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                //使用改写后的查询
                .user(reWriteMessage)
                .advisors(spec -> spec
                        .param("chat_memory_conversation_id", chatId)
                        .param("chat_memory_retrieve_size", 15)
                )
                //开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                //应用Rag知识库问答
                .advisors(new QuestionAnswerAdvisor(LoveAppvectorStore))
                //应用 RAG 检索增强服务（基于知识库服务）
  //           .advisors(LoveAppRagCloudAdvisor)
                //应用 RAG 检索增强服务（基于PgVector向量存储）
  //              .advisors(new QuestionAnswerAdvisor(PgVectorVectorStore))
                //应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                LoveAppvectorStore,"单身"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }
    
    //AI工具调用能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 恋爱报告功能（支持调用工具）
     * @param message
     * @param chatId
     * @return
     */

    public String doChatWithTools(String message , String chatId) {
        ChatResponse chatResponse = chatClient        
                .prompt()
                .user(message)
                .advisors(spec -> spec
                        .param("chat_memory_conversation_id", chatId)
                        .param("chat_memory_retrieve_size", 10)
                )
                //开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }

    //ai调用mcp服务

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 恋爱报告功能（调用MCP服务）
     * @param message
     * @param chatId
     * @return
     */

    public String doChatWithMcp(String message , String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec
                        .param("chat_memory_conversation_id", chatId)
                        .param("chat_memory_retrieve_size", 10)
                )
                //开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }
     

}
