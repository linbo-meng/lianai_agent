package com.yupi.yuaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 终端操作工具
 */
public class TerminalOperationTool {

    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        StringBuilder output = new StringBuilder();
        try {
            List<String> cmdList = buildShellCommand(command);
            ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            Charset charset = getSystemCharset();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }
        } catch (IOException | InterruptedException e) {
            output.append("Error executing command: ").append(e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return ToolResultTruncator.truncate(output.toString(), 3000);
    }

    /**
     * 根据操作系统构建 shell 命令，用于执行 dir 等内部命令
     */
    private List<String> buildShellCommand(String command) {
        List<String> cmdList = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cmdList.add("cmd.exe");
            cmdList.add("/c");
        } else {
            cmdList.add("/bin/sh");
            cmdList.add("-c");
        }
        cmdList.add(command);
        return cmdList;
    }

    /**
     * 获取操作系统对应的字符编码，避免中文乱码
     * Windows cmd.exe 使用 GBK，Linux/Mac 使用 UTF-8
     */
    private Charset getSystemCharset() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return Charset.forName("GBK");
        }
        return Charset.forName("UTF-8");
    }
}

