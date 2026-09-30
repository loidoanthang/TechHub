package com.techhub.specification;

import com.techhub.model.entity.User;
import com.techhub.model.enums.UserStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho UserSpecification (Dynamic Criteria Queries)")
class UserSpecificationTest {

    @Mock
    private Root<User> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> statusPath;

    @Mock
    private Path<Object> emailVerifiedPath;

    @Mock
    private Path<String> firstNamePath;

    @Mock
    private Expression<String> lowerExpression;

    @Mock
    private Predicate predicate;

    @Test
    @DisplayName("hasStatus với status = null trả về null predicate")
    void hasStatus_NullStatus_ReturnsNull() {
        Specification<User> spec = UserSpecification.hasStatus(null);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNull(result);
    }

    @Test
    @DisplayName("hasStatus với status hợp lệ trả về equal predicate")
    void hasStatus_ValidStatus_ReturnsPredicate() {
        when(root.get("status")).thenReturn(statusPath);
        when(cb.equal(statusPath, UserStatus.ACTIVE)).thenReturn(predicate);

        Specification<User> spec = UserSpecification.hasStatus(UserStatus.ACTIVE);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        assertEquals(predicate, result);
    }

    @Test
    @DisplayName("hasEmailVerified với null trả về null predicate")
    void hasEmailVerified_Null_ReturnsNull() {
        Specification<User> spec = UserSpecification.hasEmailVerified(null);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNull(result);
    }

    @Test
    @DisplayName("hasEmailVerified với giá trị true trả về equal predicate")
    void hasEmailVerified_True_ReturnsPredicate() {
        when(root.get("emailVerified")).thenReturn(emailVerifiedPath);
        when(cb.equal(emailVerifiedPath, true)).thenReturn(predicate);

        Specification<User> spec = UserSpecification.hasEmailVerified(true);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        assertEquals(predicate, result);
    }

    @Test
    @DisplayName("hasKeyword với keyword null hoặc rỗng trả về null predicate")
    void hasKeyword_NullOrBlank_ReturnsNull() {
        assertNull(UserSpecification.hasKeyword(null).toPredicate(root, query, cb));
        assertNull(UserSpecification.hasKeyword("").toPredicate(root, query, cb));
        assertNull(UserSpecification.hasKeyword("   ").toPredicate(root, query, cb));
    }

    @Test
    @DisplayName("hasKeyword với keyword hợp lệ trả về OR predicate của 4 trường")
    void hasKeyword_ValidKeyword_ReturnsOrPredicate() {
        Path<String> mockField = mock(Path.class);
        when(root.<String>get(anyString())).thenReturn(mockField);
        when(cb.lower(any())).thenReturn(lowerExpression);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.or(any(Predicate[].class))).thenReturn(predicate);

        Specification<User> spec = UserSpecification.hasKeyword("loi");
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        assertEquals(predicate, result);
    }
}
