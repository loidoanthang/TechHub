package com.techhub.modules.conversation.repository;

import com.techhub.modules.conversation.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.techhub.modules.conversation.entity.ConversationParticipant;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findByConversationId(UUID conversationId);

    List<ConversationParticipant> findByUserIdAndIsDeletedFalse(UUID userId);

    @Query("""
            SELECT cp
            FROM ConversationParticipant cp
            JOIN FETCH cp.user u
            LEFT JOIN FETCH u.roles
            WHERE cp.conversation.id IN :convIds
              AND cp.user.id != :userId
            """)
    List<ConversationParticipant> findOpponentsInConversations(
            @Param("convIds") Collection<UUID> convIds,
            @Param("userId") UUID userId
    );
}
