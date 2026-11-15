package com.techhub.modules;

import com.techhub.modules.order.controller.OrderController;
import com.techhub.modules.order.controller.ReturnRequestController;
import com.techhub.modules.payment.controller.PaymentController;
import com.techhub.modules.payment.controller.PaymentStatusController;
import com.techhub.modules.review.controller.ReviewController;
import com.techhub.modules.shop.controller.ShopFollowingController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Route parity: buyer endpoints live at canonical paths without /buyer")
class BuyerRouteMigrationTest {

    private static final Class<?>[] CONTROLLERS = {
            OrderController.class, ReturnRequestController.class, PaymentStatusController.class,
            ReviewController.class, ShopFollowingController.class
    };

    private static List<String> routes(Class<?> type) {
        String base = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class).path()[0];
        List<String> result = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null) {
                continue;
            }
            String sub = mapping.path().length == 0 ? "" : mapping.path()[0];
            result.add(mapping.method()[0].name() + " " + base + sub);
        }
        return result;
    }

    @Test
    @DisplayName("Expected canonical routes exist and nothing is left under /api/buyer")
    void routesAreCanonical() {
        Set<String> all = new HashSet<>();
        for (Class<?> controller : CONTROLLERS) {
            all.addAll(routes(controller));
        }
        List<String> expected = List.of(
                "GET /api/orders", "GET /api/orders/{id}", "PATCH /api/orders/{id}/complete",
                "POST /api/orders/{id}/cancellation-request", "POST /api/orders/{id}/cancel",
                "POST /api/orders/{id}/reorder", "POST /api/orders/{id}/return-request",
                "GET /api/orders/{id}/return-request", "GET /api/return-requests/{id}",
                "POST /api/return-requests/{id}/cancel", "GET /api/payment-methods",
                "GET /api/payments/{id}/status", "POST /api/reviews", "PUT /api/reviews/{id}",
                "POST /api/reviews/{id}/helpful", "GET /api/followed-shops");
        for (String route : expected) {
            assertTrue(all.contains(route), "Missing route: " + route);
        }
        assertEquals(expected.size(), all.size());
        assertFalse(all.stream().anyMatch(route -> route.contains("/buyer")));
    }

    @Test
    @DisplayName("VNPay IPN/callback contract is unchanged")
    void vnpayRoutesUnchanged() {
        List<String> vnpay = routes(PaymentController.class);
        assertTrue(vnpay.stream().allMatch(route -> route.contains("/api/payment/vnpay")));
    }
}
