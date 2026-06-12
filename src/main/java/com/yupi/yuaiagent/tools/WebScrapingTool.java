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
    @Tool(description = "Scrape the context of a web page")
    public String scrapeWebPage(@ToolParam(description = "URl of the web page to scrape") String url) throws IOException {
        try{
            Document document = Jsoup.connect(url).get();
            return document.html();
        }
        catch(Exception e){
            return "Error scraping web page" + e.getMessage();
        }
    }
}
