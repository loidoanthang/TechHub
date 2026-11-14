package com.techhub.repository;

import com.techhub.model.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findLatestMessages(@Param("conversationId") UUID conversationId, Pageable pageable);

    default Optional<Message> findLatestMessage(UUID conversationId) {
        List<Message> messages = findLatestMessages(conversationId, org.springframework.data.domain.PageRequest.of(0, 1));
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    @Query("""
            SELECT m
            FROM Message m
            WHERE m.conversation.id IN :convIds
              AND m.createdAt = (
                  SELECT MAX(m2.createdAt)
                  FROM Message m2
                  WHERE m2.conversation.id = m.conversation.id
              )
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<Message> findLatestMessagesByConversationIds(@Param("convIds") Collection<UUID> convIds);

    @Query("""
            SELECT m.conversation.id, COUNT(m.id)
            FROM Message m, ConversationParticipant cp
            WHERE cp.conversation.id = m.conversation.id
              AND cp.user.id = :userId
              AND m.conversation.id IN :convIds
              AND m.sender.id != :userId
              AND (cp.lastReadAt IS NULL OR m.createdAt > cp.lastReadAt)
            GROUP BY m.conversation.id
            """)
    List<Object[]> countUnreadMessagesByConversationIds(
            @Param("convIds") Collection<UUID> convIds,
            @Param("userId") UUID userId
    );

    @Query("""
            SELECT m
            FROM Message m
            JOIN FETCH m.sender
            WHERE m.conversation.id = :conversationId
              AND m.createdAt < :beforeTime
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<Message> findMessagesBefore(
            @Param("conversationId") UUID conversationId,
            @Param("beforeTime") LocalDateTime beforeTime,
            Pageable pageable
    );
}
