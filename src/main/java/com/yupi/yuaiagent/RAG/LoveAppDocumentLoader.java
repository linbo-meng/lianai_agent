package com.yupi.yuaiagent.RAG;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LoveAppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;
    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇markdown文档
     * @return
     */

    public List<Document> loadMarkdowns(){
        List<Document> allDocuments = new ArrayList<>();
        try{
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for(Resource resource:resources) {
                String fileName = resource.getFilename();
                //提取文档(单身，恋爱，已婚)作为标签
                String status = fileName.substring(fileName.length()-6, fileName.length()-4);
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(false)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", fileName)
                        .withAdditionalMetadata("status",status)
                        .build();
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get());
            }
        }
        catch(Exception e){
            log.error("Markdown 文档加载失败" +e);
        }
        return allDocuments;
    }
}
