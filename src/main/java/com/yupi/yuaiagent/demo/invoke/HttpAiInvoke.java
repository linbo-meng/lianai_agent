package com.yupi.yuaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
/**
 * http方式调用 ai
 */
public class HttpAiInvoke {
    
    public static void main(String[] args) {
        String apiKey = TestApiKey.Api_Key;
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", "qwen-plus");
        
        // 构建 input.messages
        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();
        
        JSONObject systemMessage = new JSONObject();
        systemMessage.set("role", "system");
        systemMessage.set("content", "You are a helpful assistant.");
        messages.add(systemMessage);
        
        JSONObject userMessage = new JSONObject();
        userMessage.set("role", "user");
        userMessage.set("content", "你是谁？");
        messages.add(userMessage);
        
        input.set("messages", messages);
        requestBody.set("input", input);
        
        // 构建 parameters
        JSONObject parameters = new JSONObject();
        parameters.set("result_format", "message");
        requestBody.set("parameters", parameters);
        
        // 发送请求
        String response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", ContentType.JSON.getValue())
                .body(requestBody.toString())
                .execute()
                .body();
        
        // 解析响应
        JSONObject responseJson = JSONUtil.parseObj(response);
        System.out.println("Response: " + responseJson.toStringPretty());
    }
}