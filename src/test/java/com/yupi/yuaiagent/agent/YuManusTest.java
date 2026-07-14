package com.yupi.yuaiagent.agent;

import dev.langchain4j.agent.tool.P;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class YuManusTest {
@Resource
    private YuManus yuManus;
@Test
public void run(){
    String userPrompt = """  
                我的另一半居住在天津和平区，请帮我找到 5 公里内合适的约会地点，  
                并结合一些网络图片，制定一份详细的约会计划，  
                并以 PDF 格式输出，用中文输出""";
    String answer = yuManus.run(userPrompt);
    Assertions.assertNotNull(answer);
}
}