package com.yupi.yuaiagent.chatmemory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 对话记忆实体
 * 每个会话对应一条记录，messages 字段存储整个会话的消息列表（JSON 数组）
 */
@Entity
@Table(name = "chat_conversation", indexes = {
        @Index(name = "idx_conversation_id", columnList = "conversationId", unique = true)
})
public class ChatConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话ID（唯一） */
    @Column(nullable = false, length = 64, unique = true)
    private String conversationId;

    /** 消息列表（JSON 数组格式） */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String messages;

    /** 更新时间 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ChatConversationEntity() {
    }

    public ChatConversationEntity(String conversationId, String messages) {
        this.conversationId = conversationId;
        this.messages = messages;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
