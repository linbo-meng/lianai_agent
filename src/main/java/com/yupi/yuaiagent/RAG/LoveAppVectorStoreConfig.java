package com.yupi.yuaiagent.RAG;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 恋爱大师向量数据库配置（初始化基于内存的向量数据库Bean）
 */
@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeyWordEnricher myKeyWordEnricher;

    @Bean
    VectorStore LoveAppvectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        //加载文档
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        //切割文档
       // List<Document> splitDocuments = myTokenTextSplitter.splitDocuments(documents);

        //自动补充关键词元信息
        List<Document> enrichedDocuments = myKeyWordEnricher.enrichDocument(documents);
        simpleVectorStore.add(enrichedDocuments);
        return simpleVectorStore;

    }

}
