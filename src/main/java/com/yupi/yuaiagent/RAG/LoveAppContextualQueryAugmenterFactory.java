package com.yupi.yuaiagent.RAG;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 创建上下文查询增前器的方法
 */
public class LoveAppContextualQueryAugmenterFactory {

    public static ContextualQueryAugmenter createInstance(){
        PromptTemplate emptyContextpromptTemplate = new PromptTemplate("""
                你应该输出以下内容：
                抱歉，我只能回答恋爱相关的问题，别的没办法帮到你呦，
                有问题请联系客服 http://www.codefather.cn""");
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextpromptTemplate)
                .build();
    }
}
