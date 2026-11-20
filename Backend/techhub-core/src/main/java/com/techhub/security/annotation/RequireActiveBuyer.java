package com.techhub.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation đảm bảo người gọi có Role BUYER và tài khoản người dùng KHÔNG ở trạng thái SUSPENDED.
 * Áp dụng cho các thao tác ghi / đặt hàng / thanh toán / tương tác mua sắm của kênh Buyer.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("hasRole('BUYER') and @buyerSecurity.isActiveBuyer()")
public @interface RequireActiveBuyer {
}
