package com.yupi.yuaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    //核心属性
    private String name;

    //提示词
    private String systemPrompt;
    private String nextStepPrompt;

    //代理状态
    private AgentState state = AgentState.IDLE;

    //执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 10;

    //LLM大模型
    private ChatClient chatClient;

    //Memory记忆（需要自主维护会话上下文）
    private List<Message> messagesList = new ArrayList<>();

    /** SSE 断线或超时后用于停止后台执行循环 */
    private volatile boolean cancelled = false;

    /**
     * 运行代理
     *
     * @param userPrompt
     * @return 执行结果
     */
    public String run(String userPrompt) {
        //基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state" + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        this.cancelled = false;
        //执行
        this.state = AgentState.RUNNING;
        //记录消息上下文
        messagesList.add(new UserMessage(userPrompt));
        //保存结果列表
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                //单步执行
                String stepResult = step();
                String result = "Step" + stepNumber + ":" + stepResult;
                results.add(result);
            }
            //检查是否超出步骤限制
            if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                state = AgentState.FINISHED;
                results.add("Terminated :Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            this.cleanup();
        }
    }

    /**
     * 运行代理（SSE 流式输出）
     *
     * @param userPrompt
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {
        //创建一个超时时间较长的SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L);//五分钟超时
        this.cancelled = false;

        //使用线程异步处理，避免阻塞主线程
        CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
            try {
                //基础校验
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误，无法从状态运行代理" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误，不能使用空提示词代理");
                    sseEmitter.complete();
                    return;
                }

                //执行
                this.state = AgentState.RUNNING;
                //记录消息上下文
                messagesList.add(new UserMessage(userPrompt));
                //执行循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED && !cancelled; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);
                    //单步执行
                    String stepResult = step();
                    if (cancelled) {
                        break;
                    }
                    String result = "Step" + stepNumber + ":" + stepResult;
                    //输出当前每一行的结果到 SSE
                    sseEmitter.send(result);
                }
                // 已取消（超时/断线）时不再 complete，由 SseEmitter 自身生命周期结束
                if (cancelled) {
                    return;
                }
                //检查是否超出步骤限制
                if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                    state = AgentState.FINISHED;
                    sseEmitter.send("执行结果，达到最大步骤(" + maxSteps + ")");
                }
                //正常完成
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent", e);
                try {
                    sseEmitter.send("执行错误" + e.getMessage());
                    sseEmitter.complete();
                } catch (Exception ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                this.cleanup();
            }
        });

        //设置超时回调
        sseEmitter.onTimeout(() -> {
            this.cancelled = true;
            this.state = AgentState.ERROR;
            future.cancel(true);
            this.cleanup();
            log.warn("SSE Connection timeout");
        });

        //设置错误回调（客户端异常断开等）
        sseEmitter.onError((ex) -> {
            this.cancelled = true;
            this.state = AgentState.ERROR;
            future.cancel(true);
            this.cleanup();
            log.warn("SSE Connection error", ex);
        });

        //设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.cancelled = true;
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE Connection completed");
        });

        return sseEmitter;
    }

    /**
     * 定义单个步骤
     * @return
     */
    public abstract String step();

    /**
     * 清理资源并重置状态，便于同一实例再次运行
     */
    protected void cleanup() {
        this.currentStep = 0;
        this.messagesList.clear();
        this.state = AgentState.IDLE;
    }
}
