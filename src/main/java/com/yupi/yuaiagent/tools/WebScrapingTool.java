package com.yupi.yuaiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

/**
 * 网页抓取工具
 */
public class WebScrapingTool {

    private static final int MAX_RESULT_CHARS = 4000;

    @Tool(description = "Scrape the text content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URl of the web page to scrape") String url) throws IOException {
        try {
            Document document = Jsoup.connect(url).get();
            // 只返回纯文本并截断，避免整页 HTML 撑爆 Agent 上下文
            String text = document.text();
            return ToolResultTruncator.truncate(text, MAX_RESULT_CHARS);
        } catch (Exception e) {
            return "Error scraping web page" + e.getMessage();
        }
    }
}
