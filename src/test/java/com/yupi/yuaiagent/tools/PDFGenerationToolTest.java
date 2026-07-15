package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFGenerationToolTest {

    @Test
    void shouldGenerateChinesePdfWithEmojiFiltered() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String result = tool.generatePDF(
                "qixi_plan_test.pdf",
                """
                        天津和平区七夕约会计划
                        1. 午餐：附近精选餐厅
                        2. 下午：步行街散步
                        包含爱心符号❤️和符号◆
                        预订电话：022-12345678
                        """
        );
        System.out.println(result);
        assertTrue(result.contains("PDF generated successfully"), result);
        assertTrue(Files.exists(Path.of(System.getProperty("user.dir"), "tmp", "pdf", "qixi_plan_test.pdf")));
    }
}
