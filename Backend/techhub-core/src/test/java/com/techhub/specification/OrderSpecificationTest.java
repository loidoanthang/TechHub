package com.techhub.specification;

import com.techhub.model.entity.Order;
import com.techhub.model.entity.OrderItem;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.OrderStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho OrderSpecification (Buyer Order Filtering & Search)")
class OrderSpecificationTest {

    @Mock
    private Root<Order> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<User> userPath;

    @Mock
    private Path<UUID> userIdPath;

    @Mock
    private Path<OrderStatus> statusPath;

    @Mock
    private Path<String> orderNumberPath;

    @Mock
    private Path<Shop> shopPath;

    @Mock
    private Path<String> shopNamePath;

    @Mock
    private Subquery<UUID> itemSubquery;

    @Mock
    private Root<OrderItem> itemRoot;

    @Mock
    private Path<Order> itemOrderPath;

    @Mock
    private Path<UUID> itemOrderIdPath;

    @Mock
    private Path<String> itemProductNamePath;

    @Mock
    private Predicate dummyPredicate;

    @Test
    @DisplayName("filterBuyerOrders: Chỉ lọc theo userId (không status, không keyword)")
    void filterBuyerOrders_OnlyUserId() {
        UUID userId = UUID.randomUUID();

        when(root.<User>get("user")).thenReturn(userPath);
        when(userPath.<UUID>get("id")).thenReturn(userIdPath);
        when(cb.equal(userIdPath, userId)).thenReturn(dummyPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(dummyPredicate);

        Specification<Order> spec = OrderSpecification.filterBuyerOrders(userId, null, null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(userIdPath, userId);
        verify(cb, never()).equal(eq(statusPath), any());
    }

    @Test
    @DisplayName("filterBuyerOrders: Có lọc theo status")
    void filterBuyerOrders_WithStatus() {
        UUID userId = UUID.randomUUID();

        when(root.<User>get("user")).thenReturn(userPath);
        when(userPath.<UUID>get("id")).thenReturn(userIdPath);
        when(cb.equal(userIdPath, userId)).thenReturn(dummyPredicate);

        when(root.<OrderStatus>get("status")).thenReturn(statusPath);
        when(cb.equal(statusPath, OrderStatus.CONFIRMED)).thenReturn(dummyPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(dummyPredicate);

        Specification<Order> spec = OrderSpecification.filterBuyerOrders(userId, OrderStatus.CONFIRMED, null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(userIdPath, userId);
        verify(cb).equal(statusPath, OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("filterBuyerOrders: Có keyword và escape ký tự đặc biệt %, _, \\")
    void filterBuyerOrders_WithKeywordAndSpecialChars() {
        UUID userId = UUID.randomUUID();
        String keyword = "test%_\\search";

        when(root.<User>get("user")).thenReturn(userPath);
        when(userPath.<UUID>get("id")).thenReturn(userIdPath);
        when(cb.equal(userIdPath, userId)).thenReturn(dummyPredicate);

        // Order number matching
        when(root.<String>get("orderNumber")).thenReturn(orderNumberPath);
        when(cb.lower(orderNumberPath)).thenReturn(orderNumberPath);
        when(cb.like(eq(orderNumberPath), anyString(), eq('\\'))).thenReturn(dummyPredicate);

        // Shop name matching
        when(root.<Shop>get("shop")).thenReturn(shopPath);
        when(shopPath.<String>get("name")).thenReturn(shopNamePath);
        when(cb.lower(shopNamePath)).thenReturn(shopNamePath);
        when(cb.like(eq(shopNamePath), anyString(), eq('\\'))).thenReturn(dummyPredicate);

        // Subquery matching
        doReturn(itemSubquery).when(query).subquery(UUID.class);
        when(itemSubquery.from(OrderItem.class)).thenReturn(itemRoot);
        when(itemRoot.<Order>get("order")).thenReturn(itemOrderPath);
        when(itemOrderPath.<UUID>get("id")).thenReturn(itemOrderIdPath);
        when(itemSubquery.select(itemOrderIdPath)).thenReturn(itemSubquery);
        when(itemRoot.<String>get("productNameSnapshot")).thenReturn(itemProductNamePath);
        when(cb.lower(itemProductNamePath)).thenReturn(itemProductNamePath);
        when(cb.equal(itemOrderPath, root)).thenReturn(dummyPredicate);
        when(cb.like(eq(itemProductNamePath), anyString(), eq('\\'))).thenReturn(dummyPredicate);
        when(itemSubquery.where(any(Predicate[].class))).thenReturn(itemSubquery);
        when(cb.exists(itemSubquery)).thenReturn(dummyPredicate);

        when(cb.or(any(Predicate[].class))).thenReturn(dummyPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(dummyPredicate);

        Specification<Order> spec = OrderSpecification.filterBuyerOrders(userId, null, keyword);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).like(eq(orderNumberPath), contains("test\\%\\_\\\\search"), eq('\\'));
        verify(cb).exists(itemSubquery);
    }

    @Test
    @DisplayName("filterBuyerOrders: Keyword rỗng hoặc chỉ có khoảng trắng thì bỏ qua")
    void filterBuyerOrders_BlankKeyword_Ignored() {
        UUID userId = UUID.randomUUID();

        when(root.<User>get("user")).thenReturn(userPath);
        when(userPath.<UUID>get("id")).thenReturn(userIdPath);
        when(cb.equal(userIdPath, userId)).thenReturn(dummyPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(dummyPredicate);

        Specification<Order> spec = OrderSpecification.filterBuyerOrders(userId, null, "   ");
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(query, never()).subquery(any(Class.class));
    }
}
