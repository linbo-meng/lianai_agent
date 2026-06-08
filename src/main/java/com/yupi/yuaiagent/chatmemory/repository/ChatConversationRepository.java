package com.yupi.yuaiagent.chatmemory.repository;

import com.yupi.yuaiagent.chatmemory.entity.ChatConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 对话记忆仓库
 */
@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, Long> {

    Optional<ChatConversationEntity> findByConversationId(String conversationId);

    void deleteByConversationId(String conversationId);
}
