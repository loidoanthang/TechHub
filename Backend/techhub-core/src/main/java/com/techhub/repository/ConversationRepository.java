package com.techhub.repository;

import com.techhub.model.entity.Conversation;
import com.techhub.model.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            SELECT cp1.conversation
            FROM ConversationParticipant cp1
            JOIN ConversationParticipant cp2 ON cp1.conversation.id = cp2.conversation.id
            WHERE cp1.user.id = :user1Id
              AND cp2.user.id = :user2Id
            ORDER BY cp1.conversation.updatedAt DESC
            """)
    List<Conversation> findConversationsBetweenUsers(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id
    );

    default Optional<Conversation> findConversationBetweenUsers(UUID user1Id, UUID user2Id) {
        return findConversationsBetweenUsers(user1Id, user2Id).stream().findFirst();
    }

    @Query(
            value = """
                    SELECT cp.conversation
                    FROM ConversationParticipant cp
                    WHERE cp.user.id = :userId
                      AND cp.isDeleted = false
                      AND (:type IS NULL OR cp.conversation.type = :type)
                    ORDER BY cp.conversation.updatedAt DESC, cp.conversation.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(cp.id)
                    FROM ConversationParticipant cp
                    WHERE cp.user.id = :userId
                      AND cp.isDeleted = false
                      AND (:type IS NULL OR cp.conversation.type = :type)
                    """
    )
    Page<Conversation> findUserConversations(
            @Param("userId") UUID userId,
            @Param("type") ConversationType type,
            Pageable pageable
    );

    @Query("""
            SELECT cp1.conversation
            FROM ConversationParticipant cp1
            JOIN ConversationParticipant cp2 ON cp1.conversation.id = cp2.conversation.id
            WHERE cp1.user.id = :user1Id
              AND cp2.user.id = :user2Id
              AND cp1.conversation.type = :type
            """)
    Optional<Conversation> findConversationBetweenUsersAndType(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id,
            @Param("type") ConversationType type
    );
}
