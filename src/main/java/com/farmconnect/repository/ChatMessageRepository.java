package com.farmconnect.repository;

import com.farmconnect.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("select m from ChatMessage m where m.sender.id = :u or m.recipient.id = :u order by m.createdAt desc")
    List<ChatMessage> involving(@Param("u") Long u);

    @Query("select m from ChatMessage m where (m.sender.id = :a and m.recipient.id = :b) or (m.sender.id = :b and m.recipient.id = :a) order by m.createdAt asc")
    List<ChatMessage> between(@Param("a") Long a, @Param("b") Long b);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);
}
