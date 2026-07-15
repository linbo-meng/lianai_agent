package com.yupi.yuaiagent.tools;
import cn.hutool.core.io.FileUtil;
import com.yupi.yuaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.Path;

/**
 * 文件操作工具类（提供文件读写功能）
 */
public class FileOperationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    @Tool(description = "Read context from a file")
    public String readFile(@ToolParam(description = "Name of a file to read") String fileName) {
        String filePath = FILE_DIR + "/" + fileName;
        try {
            return ToolResultTruncator.truncate(FileUtil.readUtf8String(filePath), 4000);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write context to a file")
    public String writeFile(@ToolParam(description = "Name of a file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content) {
        // 规范化文件名，禁止路径穿越
        String safeName = Path.of(fileName == null ? "note.txt" : fileName)
                .getFileName()
                .toString()
                .replaceAll("[\\\\/:*?\"<>|]", "_");
        if (safeName.isBlank()) {
            safeName = "note.txt";
        }
        String filePath = FILE_DIR + "/" + safeName;
        try {
            FileUtil.mkdir(FILE_DIR);
            // Hutool 参数顺序：content 在前，path 在后
            FileUtil.writeUtf8String(content == null ? "" : content, filePath);
            return "File written successfully: " + filePath
                    + " | download: type=file&name=" + safeName;
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}