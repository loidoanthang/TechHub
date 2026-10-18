package com.techhub.specification;

import com.techhub.model.entity.Shop;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho VoucherSpecification (Shop Voucher Filtering)")
class VoucherSpecificationTest {

    @Mock
    private Root<Voucher> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Shop> shopPath;

    @Mock
    private Path<UUID> shopIdPath;

    @Mock
    private Path<VoucherType> typePath;

    @Mock
    private Path<String> codePath;

    @Mock
    private Path<String> namePath;

    @Mock
    private Path<VoucherAppliesTo> appliesToPath;

    @Mock
    private Path<DiscountType> discountTypePath;

    @Mock
    private Path<Boolean> isActivePath;

    @Mock
    private Path<Boolean> isDeletedPath;

    @Mock
    private Path<LocalDateTime> startDatePath;

    @Mock
    private Path<LocalDateTime> endDatePath;

    @Mock
    private Expression<String> lowerCodeExpression;

    @Mock
    private Expression<String> lowerNameExpression;

    @Mock
    private Join<Voucher, Shop> shopJoin;

    @Mock
    private Path<String> shopNamePath;

    @Mock
    private Expression<String> lowerShopNameExpression;

    @Mock
    private Predicate predicate;

    private void setupBaseMocks() {
        when(root.<Shop>get("shop")).thenReturn(shopPath);
        when(shopPath.<UUID>get("id")).thenReturn(shopIdPath);
        when(cb.equal(eq(shopIdPath), any(UUID.class))).thenReturn(predicate);

        when(root.<VoucherType>get("type")).thenReturn(typePath);
        when(cb.equal(typePath, VoucherType.SHOP)).thenReturn(predicate);

        when(root.<Boolean>get("isDeleted")).thenReturn(isDeletedPath);
        when(cb.isFalse(isDeletedPath)).thenReturn(predicate);

        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
    }

    @Test
    @DisplayName("filterShopVouchers không có optional filter chỉ chứa điều kiện shopId, type SHOP và isDeleted false")
    void filterShopVouchers_BaseOnly() {
        setupBaseMocks();
        UUID shopId = UUID.randomUUID();

        Specification<Voucher> spec = VoucherSpecification.filterShopVouchers(
                shopId, null, null, null, null, LocalDateTime.now()
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(shopIdPath, shopId);
        verify(cb).equal(typePath, VoucherType.SHOP);
        verify(cb).isFalse(isDeletedPath);
    }

    @Test
    @DisplayName("filterShopVouchers có keyword thêm điều kiện like code và name")
    void filterShopVouchers_WithKeyword() {
        setupBaseMocks();
        UUID shopId = UUID.randomUUID();

        when(root.<String>get("code")).thenReturn(codePath);
        when(root.<String>get("name")).thenReturn(namePath);
        when(cb.lower(codePath)).thenReturn(lowerCodeExpression);
        when(cb.lower(namePath)).thenReturn(lowerNameExpression);
        when(cb.like(lowerCodeExpression, "%tech%")).thenReturn(predicate);
        when(cb.like(lowerNameExpression, "%tech%")).thenReturn(predicate);
        when(cb.or(predicate, predicate)).thenReturn(predicate);

        Specification<Voucher> spec = VoucherSpecification.filterShopVouchers(
                shopId, "  TECH  ", null, null, null, LocalDateTime.now()
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).or(predicate, predicate);
    }

    @Test
    @DisplayName("filterShopVouchers có appliesTo và discountType")
    void filterShopVouchers_WithAppliesToAndDiscountType() {
        setupBaseMocks();
        UUID shopId = UUID.randomUUID();

        when(root.<VoucherAppliesTo>get("appliesTo")).thenReturn(appliesToPath);
        when(cb.equal(appliesToPath, VoucherAppliesTo.PRODUCT)).thenReturn(predicate);

        when(root.<DiscountType>get("discountType")).thenReturn(discountTypePath);
        when(cb.equal(discountTypePath, DiscountType.PERCENTAGE)).thenReturn(predicate);

        Specification<Voucher> spec = VoucherSpecification.filterShopVouchers(
                shopId, null, VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE, null, LocalDateTime.now()
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(appliesToPath, VoucherAppliesTo.PRODUCT);
        verify(cb).equal(discountTypePath, DiscountType.PERCENTAGE);
    }

    @Test
    @DisplayName("filterShopVouchers với status UPCOMING")
    void filterShopVouchers_WithStatusUpcoming() {
        setupBaseMocks();
        UUID shopId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(root.<Boolean>get("isActive")).thenReturn(isActivePath);
        when(cb.isTrue(isActivePath)).thenReturn(predicate);

        when(root.<LocalDateTime>get("startDate")).thenReturn(startDatePath);
        when(cb.greaterThan(startDatePath, now)).thenReturn(predicate);

        when(root.<LocalDateTime>get("endDate")).thenReturn(endDatePath);
        when(cb.greaterThanOrEqualTo(endDatePath, now)).thenReturn(predicate);

        Specification<Voucher> spec = VoucherSpecification.filterShopVouchers(
                shopId, null, null, null, VoucherStatus.UPCOMING, now
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).isTrue(isActivePath);
        verify(cb).greaterThan(startDatePath, now);
        verify(cb).greaterThanOrEqualTo(endDatePath, now);
    }

    @Test
    @DisplayName("filterShopVouchers với status ONGOING")
    void filterShopVouchers_WithStatusOngoing() {
        setupBaseMocks();
        UUID shopId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(root.<Boolean>get("isActive")).thenReturn(isActivePath);
        when(cb.isTrue(isActivePath)).thenReturn(predicate);

        when(root.<LocalDateTime>get("startDate")).thenReturn(startDatePath);
        when(cb.lessThanOrEqualTo(startDatePath, now)).thenReturn(predicate);

        when(root.<LocalDateTime>get("endDate")).thenReturn(endDatePath);
        when(cb.greaterThanOrEqualTo(endDatePath, now)).thenReturn(predicate);

        Specification<Voucher> spec = VoucherSpecification.filterShopVouchers(
                shopId, null, null, null, VoucherStatus.ONGOING, now
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).isTrue(isActivePath);
        verify(cb).lessThanOrEqualTo(startDatePath, now);
        verify(cb).greaterThanOrEqualTo(endDatePath, now);
    }

    @Test
    @DisplayName("filterShopVouchers với status EXPIRED")
    void filterShopVouchers_WithStatusExpired() {
        setupBaseMocks();
        UUID shopId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(root.<Boolean>get("isActive")).thenReturn(isActivePath);
        when(cb.isTrue(isActivePath)).thenReturn(predicate);

        when(root.<LocalDateTime>get("endDate")).thenReturn(endDatePath);
        when(cb.lessThan(endDatePath, now)).thenReturn(predicate);

        Specification<Voucher> spec = VoucherSpecification.filterShopVouchers(
                shopId, null, null, null, VoucherStatus.EXPIRED, now
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).isTrue(isActivePath);
        verify(cb).lessThan(endDatePath, now);
    }

    @Test
    @DisplayName("filterShopVouchers với status DISABLED")
    void filterShopVouchers_WithStatusDisabled() {
        setupBaseMocks();
        UUID shopId = UUID.randomUUID();

        when(root.<Boolean>get("isActive")).thenReturn(isActivePath);
        when(cb.isFalse(isActivePath)).thenReturn(predicate);

        Specification<Voucher> spec = VoucherSpecification.filterShopVouchers(
                shopId, null, null, null, VoucherStatus.DISABLED, LocalDateTime.now()
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).isFalse(isActivePath);
    }

    @Test
    @DisplayName("filterAdminVouchers: Base condition chỉ kiểm tra isDeleted false")
    void filterAdminVouchers_BaseOnly() {
        when(root.<Boolean>get("isDeleted")).thenReturn(isDeletedPath);
        when(cb.isFalse(isDeletedPath)).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Specification<Voucher> spec = VoucherSpecification.filterAdminVouchers(
                null, null, null, null, null, null, LocalDateTime.now()
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).isFalse(isDeletedPath);
    }

    @Test
    @DisplayName("filterAdminVouchers: Keyword search với ký tự đại diện %, _ được escape chính xác")
    void filterAdminVouchers_KeywordEscaped() {
        when(root.<Boolean>get("isDeleted")).thenReturn(isDeletedPath);
        when(cb.isFalse(isDeletedPath)).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        when(root.<String>get("code")).thenReturn(codePath);
        when(root.<String>get("name")).thenReturn(namePath);
        when(cb.lower(codePath)).thenReturn(lowerCodeExpression);
        when(cb.lower(namePath)).thenReturn(lowerNameExpression);

        when(root.<Voucher, Shop>join("shop", JoinType.LEFT)).thenReturn(shopJoin);
        when(shopJoin.<String>get("name")).thenReturn(shopNamePath);
        when(cb.lower(shopNamePath)).thenReturn(lowerShopNameExpression);

        when(cb.like(eq(lowerCodeExpression), eq("%10\\%\\_sale%"), eq('\\'))).thenReturn(predicate);
        when(cb.like(eq(lowerNameExpression), eq("%10\\%\\_sale%"), eq('\\'))).thenReturn(predicate);
        when(cb.like(eq(lowerShopNameExpression), eq("%10\\%\\_sale%"), eq('\\'))).thenReturn(predicate);
        when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(predicate);

        Specification<Voucher> spec = VoucherSpecification.filterAdminVouchers(
                null, null, "10%_sale", null, null, null, LocalDateTime.now()
        );

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).like(lowerCodeExpression, "%10\\%\\_sale%", '\\');
        verify(cb).like(lowerNameExpression, "%10\\%\\_sale%", '\\');
        verify(cb).like(lowerShopNameExpression, "%10\\%\\_sale%", '\\');
    }
}
