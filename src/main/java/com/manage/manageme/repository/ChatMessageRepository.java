package com.manage.manageme.repository;

import com.manage.manageme.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByProfileIdOrderByCreatedAtAsc(Long profileId);
}