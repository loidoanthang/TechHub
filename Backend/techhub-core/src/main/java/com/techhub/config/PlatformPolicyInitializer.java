package com.techhub.config;

import com.techhub.model.entity.TransactionPolicy;
import com.techhub.repository.TransactionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class PlatformPolicyInitializer implements ApplicationRunner {

    private final TransactionPolicyRepository transactionPolicyRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (transactionPolicyRepository.count() == 0) {
            log.info("Platform policies table is empty. Initializing default system policies at startup...");
            List<TransactionPolicy> defaultPolicies = List.of(
                    TransactionPolicy.builder()
                            .policyKey("AUTO_APPROVE_RETURN_HOURS")
                            .policyValue("72")
                            .description("Thời gian tự động duyệt khiếu nại nếu Seller không phản hồi (giờ)")
                            .build(),
                    TransactionPolicy.builder()
                            .policyKey("ORDER_AUTO_COMPLETE_HOURS")
                            .policyValue("72")
                            .description("Thời gian SLA tự động kết chuyển đơn DELIVERED sang COMPLETED (giờ)")
                            .build(),
                    TransactionPolicy.builder()
                            .policyKey("PLATFORM_COMMISSION_RATE")
                            .policyValue("0.05")
                            .description("Tỷ lệ hoa hồng chiết khấu sàn áp dụng cho Seller (5%)")
                            .build(),
                    TransactionPolicy.builder()
                            .policyKey("RETURN_REQUEST_WINDOW_HOURS")
                            .policyValue("72")
                            .description("Thời hạn cho phép Buyer gửi khiếu nại Trả hàng/Hoàn tiền sau khi nhận hàng (giờ)")
                            .build(),
                    TransactionPolicy.builder()
                            .policyKey("VNPAY_PAYMENT_TIMEOUT_MINUTES")
                            .policyValue("15")
                            .description("Thời gian chờ thanh toán đơn hàng VNPAY trước khi tự hủy (phút)")
                            .build()
            );
            transactionPolicyRepository.saveAll(defaultPolicies);
            log.info("Successfully seeded 5 default platform policies at startup.");
        }
    }
}
