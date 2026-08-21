package com.manage.manageme.repository;

import com.manage.manageme.entity.ChatThread;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {
    List<ChatThread> findByProfileIdOrderByCreatedAtDesc(Long profileId);
}