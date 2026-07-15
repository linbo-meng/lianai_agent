package com.yupi.yuaiagent.agent;

import com.yupi.yuaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper=false)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent{
    /**
     * 处理当前状态决定下一次行动
     *
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     *
     * @return 行动执行结果
     */
    public abstract String act();

    @Override
    public String step(){
        try {
            //先思考
            boolean shouldAct = think();
            if(!shouldAct){
                //无需行动时结束循环，避免白跑到 maxSteps
                setState(AgentState.FINISHED);
                return "思考完成 无需行动";
            }
            //再行动
            return act();
        }
        catch (Exception e){
            log.error("步骤执行失败", e);
            return "步骤执行失败" + e.getMessage();
        }

    }
}
