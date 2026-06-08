package com.yupi.yuaiagent.chatmemory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.yupi.yuaiagent.chatmemory.entity.ChatConversationEntity;
import com.yupi.yuaiagent.chatmemory.repository.ChatConversationRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 MySQL 持久化的对话记忆（整体存储方案）
 * <p>
 * 每个会话在数据库中对应一条记录，messages 字段以 JSON 数组格式存储整个会话的消息列表。
 * 相比逐条存储，整体存储的读写性能更好，更符合 ChatMemory 接口的语义。
 */
public class JdbcChatMemory implements ChatMemory {

    private final ChatConversationRepository repository;
    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    private static final CollectionType messageRecordListType = objectMapper.getTypeFactory()
            .constructCollectionType(List.class, MessageRecord.class);

    public JdbcChatMemory(ChatConversationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }

        // 1. 查出该会话已有的消息列表
        List<MessageRecord> existingRecords = loadMessageRecords(conversationId);

        // 2. 将新消息转为 MessageRecord
        List<MessageRecord> newRecords = messages.stream()
                .map(this::toMessageRecord)
                .toList();

        // 3. 合并并写回
        existingRecords.addAll(newRecords);
        saveMessageRecords(conversationId, existingRecords);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<MessageRecord> records = loadMessageRecords(conversationId);
        return records.stream()
                .map(this::toMessage)
                .toList();
    }

    /**
     * 获取指定会话的最近 N 条消息
     */
    public List<Message> get(String conversationId, int lastN) {
        List<MessageRecord> records = loadMessageRecords(conversationId);
        int size = records.size();
        if (size <= lastN) {
            return records.stream().map(this::toMessage).toList();
        }
        return records.subList(size - lastN, size).stream()
                .map(this::toMessage)
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }

    // ==================== 序列化 / 反序列化 ====================

    /**
     * 从数据库加载某个会话的消息记录列表
     */
    private List<MessageRecord> loadMessageRecords(String conversationId) {
        Optional<ChatConversationEntity> opt = repository.findByConversationId(conversationId);
        if (opt.isEmpty()) {
            return new ArrayList<>();
        }
        String json = opt.get().getMessages();
        try {
            List<MessageRecord> records = objectMapper.readValue(json, messageRecordListType);
            return new ArrayList<>(records);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化消息列表失败", e);
        }
    }

    /**
     * 将消息记录列表保存到数据库
     */
    private void saveMessageRecords(String conversationId, List<MessageRecord> records) {
        try {
            String json = objectMapper.writeValueAsString(records);
            ChatConversationEntity entity = repository.findByConversationId(conversationId)
                    .orElse(new ChatConversationEntity(conversationId, json));
            entity.setMessages(json);
            repository.save(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化消息列表失败", e);
        }
    }

    /**
     * 将 Message 转为 MessageRecord
     */
    private MessageRecord toMessageRecord(Message message) {
        return new MessageRecord(
                message.getMessageType().name(),
                message.getText(),
                message.getMetadata()
        );
    }

    /**
     * 将 MessageRecord 转为 Message
     */
    private Message toMessage(MessageRecord record) {
        MessageType messageType = MessageType.valueOf(record.messageType());
        String textContent = record.textContent() != null ? record.textContent() : "";
        Map<String, Object> metadata = record.metadata();
        Map<String, Object> safeMetadata = metadata != null ? new HashMap<>(metadata) : Collections.emptyMap();

        return switch (messageType) {
            case USER -> {
                UserMessage msg = new UserMessage(textContent);
                msg.getMetadata().putAll(safeMetadata);
                yield msg;
            }
            case ASSISTANT -> {
                AssistantMessage msg = new AssistantMessage(textContent);
                msg.getMetadata().putAll(safeMetadata);
                yield msg;
            }
            case SYSTEM -> new SystemMessage(textContent);
            case TOOL -> buildToolResponseMessage(safeMetadata);
        };
    }

    @SuppressWarnings("unchecked")
    private Message buildToolResponseMessage(Map<String, Object> metadata) {
        List<Map<String, Object>> toolResponses = metadata != null ?
                (List<Map<String, Object>>) metadata.get("toolResponses") : List.of();
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        if (toolResponses != null) {
            for (Map<String, Object> resp : toolResponses) {
                responses.add(new ToolResponseMessage.ToolResponse(
                        (String) resp.get("id"),
                        (String) resp.get("name"),
                        (String) resp.get("responseData")
                ));
            }
        }
        return new ToolResponseMessage(
                responses,
                metadata != null ? new HashMap<>(metadata) : Collections.emptyMap()
        );
    }

    /**
     * 消息序列化中间记录
     */
    private record MessageRecord(
            String messageType,
            String textContent,
            Map<String, Object> metadata
    ) {
    }
}
