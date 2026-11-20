package com.techhub.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation đảm bảo người gọi có Role SELLER và Shop tương ứng KHÔNG ở trạng thái SUSPENDED hoặc BANNED.
 * Áp dụng cho các thao tác ghi / tạo sản phẩm / tạo voucher / quản lý gian hàng của kênh Seller.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("hasRole('SELLER') and @shopSecurity.isActiveShop()")
public @interface RequireActiveShop {
}
