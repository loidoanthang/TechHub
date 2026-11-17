package com.techhub.config;

import com.techhub.modules.order.entity.Order;
import com.techhub.modules.order.entity.OrderItem;
import com.techhub.modules.order.enums.OrderStatus;
import com.techhub.modules.order.repository.OrderItemRepository;
import com.techhub.modules.order.repository.OrderRepository;
import com.techhub.modules.payment.enums.PaymentMethod;
import com.techhub.modules.payment.enums.PaymentStatus;
import com.techhub.modules.payment.repository.PaymentRepository;
import com.techhub.modules.review.entity.Review;
import com.techhub.modules.review.entity.ReviewHelpful;
import com.techhub.modules.review.entity.ReviewMedia;
import com.techhub.modules.review.enums.ReviewMediaType;
import com.techhub.modules.review.repository.ReviewHelpfulRepository;
import com.techhub.modules.review.repository.ReviewMediaRepository;
import com.techhub.modules.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import com.techhub.modules.category.enums.CategoryAttributeDataType;
import com.techhub.modules.category.enums.CategoryStatus;
import com.techhub.modules.product.enums.ProductApprovalStatus;
import com.techhub.modules.product.enums.ProductVariantStatus;
import com.techhub.modules.product.enums.ProductVisibilityStatus;
import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.user.enums.AddressLabel;
import com.techhub.modules.user.enums.Role;
import com.techhub.modules.user.enums.UserStatus;
import com.techhub.modules.voucher.enums.DiscountType;
import com.techhub.modules.voucher.enums.VoucherAppliesTo;
import com.techhub.modules.voucher.enums.VoucherType;
import com.techhub.modules.cart.entity.Cart;
import com.techhub.modules.cart.entity.CartItem;
import com.techhub.modules.cart.repository.CartItemRepository;
import com.techhub.modules.cart.repository.CartRepository;
import com.techhub.modules.category.entity.Category;
import com.techhub.modules.category.entity.CategoryAttribute;
import com.techhub.modules.category.repository.CategoryAttributeRepository;
import com.techhub.modules.category.repository.CategoryRepository;
import com.techhub.modules.payment.entity.Payment;
import com.techhub.modules.product.entity.Product;
import com.techhub.modules.product.entity.ProductImage;
import com.techhub.modules.product.entity.ProductVariant;
import com.techhub.modules.product.repository.ProductImageRepository;
import com.techhub.modules.product.repository.ProductRepository;
import com.techhub.modules.product.repository.ProductVariantRepository;
import com.techhub.modules.seller.entity.Seller;
import com.techhub.modules.seller.repository.SellerRepository;
import com.techhub.modules.shipping.entity.ShippingOption;
import com.techhub.modules.shipping.repository.ShippingOptionRepository;
import com.techhub.modules.shop.entity.Shop;
import com.techhub.modules.shop.repository.ShopRepository;
import com.techhub.modules.user.entity.Address;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.repository.AddressRepository;
import com.techhub.modules.user.repository.UserRepository;
import com.techhub.modules.voucher.entity.Voucher;
import com.techhub.modules.voucher.repository.VoucherRepository;

@Slf4j
@Component
@org.springframework.core.annotation.Order(1)
@Profile("!test")
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final ShippingOptionRepository shippingOptionRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final VoucherRepository voucherRepository;
    private final PasswordEncoder passwordEncoder;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking initial test dataset for Postman testing...");

        if (userRepository.existsByEmail("buyer@techhub.vn")) {
            log.info("Test dataset already exists in database. Ensuring seller roles, categories, attributes, and full order dataset exist...");
            ensureSellerRolesExist();
            ensureAdditionalCategoriesAndProductsExist();
            ensureAllCategoryAttributesExist();
            ensureProductAndVariantAttributesPopulated();
            ensureComprehensiveOrdersAndReviewsExist();
            printSummaryLog();
            return;
        }

        log.info("Initializing comprehensive test dataset for TechHub Checkout & Payment flow...");

        String commonPassword = passwordEncoder.encode("Password123@");
        LocalDateTime now = LocalDateTime.now();

        // 1. ADMIN USER
        User adminUser = new User();
        adminUser.setEmail("admin@techhub.vn");
        adminUser.setPassword(commonPassword);
        adminUser.setFirstName("Hệ Thống");
        adminUser.setLastName("Admin");
        adminUser.setPhone("0999888777");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        userRepository.save(adminUser);

        // 2. BUYER USER & ADDRESS
        User buyerUser = new User();
        buyerUser.setEmail("buyer@techhub.vn");
        buyerUser.setPassword(commonPassword);
        buyerUser.setFirstName("Văn A");
        buyerUser.setLastName("Nguyễn");
        buyerUser.setPhone("0901234567");
        buyerUser.setStatus(UserStatus.ACTIVE);
        buyerUser.setEmailVerified(true);
        buyerUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        userRepository.save(buyerUser);

        Address buyerAddress = Address.builder()
                .user(buyerUser)
                .label(AddressLabel.HOME)
                .recipientName("Nguyễn Văn A")
                .phone("0901234567")
                .province("Thành phố Hà Nội")
                .district("Quận Cầu Giấy")
                .ward("Phường Dịch Vọng Hậu")
                .streetAddress("Số 144 Xuân Thủy")
                .isDefault(true)
                .build();
        addressRepository.save(buyerAddress);

        // 3. SELLER 1 & SHOP 1 (Apple Flagship Store)
        User seller1User = new User();
        seller1User.setEmail("seller.apple@techhub.vn");
        seller1User.setPassword(commonPassword);
        seller1User.setFirstName("Minh");
        seller1User.setLastName("Trần");
        seller1User.setPhone("0912345678");
        seller1User.setStatus(UserStatus.ACTIVE);
        seller1User.setEmailVerified(true);
        seller1User.setRoles(new HashSet<>(Set.of(Role.BUYER, Role.SELLER)));
        userRepository.save(seller1User);

        Seller seller1 = Seller.builder()
                .user(seller1User)
                .sellerName("apple-authorized-reseller")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("1012345678")
                .bankAccountHolder("TRAN MINH")
                .balance(BigDecimal.valueOf(10000000))
                .build();
        sellerRepository.save(seller1);

        Shop shop1 = Shop.builder()
                .seller(seller1)
                .name("Apple Flagship Store")
                .description("Cửa hàng ủy quyền chính hãng Apple Việt Nam")
                .avatarUrl("https://images.unsplash.com/photo-1611186871348-b1ce696e52c9")
                .bannerUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8")
                .status(ShopStatus.ACTIVE)
                .build();
        shopRepository.save(shop1);

        ShippingOption shippingOption1 = ShippingOption.builder()
                .shop(shop1)
                .name("Giao hàng tiêu chuẩn")
                .shippingFee(BigDecimal.valueOf(30000))
                .isActive(true)
                .build();
        shippingOptionRepository.save(shippingOption1);

        Address shop1Address = Address.builder()
                .shopId(shop1.getId())
                .recipientName("Kho Apple Flagship")
                .phone("0912345678")
                .province("Thành phố Hà Nội")
                .district("Quận Ba Đình")
                .ward("Phường Liễu Giai")
                .streetAddress("Tòa nhà Lotte Center, 54 Liễu Giai")
                .isDefault(true)
                .build();
        addressRepository.save(shop1Address);

        // 4. SELLER 2 & SHOP 2 (Anker Official Store)
        User seller2User = new User();
        seller2User.setEmail("seller.anker@techhub.vn");
        seller2User.setPassword(commonPassword);
        seller2User.setFirstName("Hoàng");
        seller2User.setLastName("Lê");
        seller2User.setPhone("0923456789");
        seller2User.setStatus(UserStatus.ACTIVE);
        seller2User.setEmailVerified(true);
        seller2User.setRoles(new HashSet<>(Set.of(Role.BUYER, Role.SELLER)));
        userRepository.save(seller2User);

        Seller seller2 = Seller.builder()
                .user(seller2User)
                .sellerName("anker-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Techcombank")
                .bankAccountNumber("1903456789")
                .bankAccountHolder("LE HOANG")
                .balance(BigDecimal.valueOf(5000000))
                .build();
        sellerRepository.save(seller2);

        Shop shop2 = Shop.builder()
                .seller(seller2)
                .name("Anker Official Store")
                .description("Gian hàng phụ kiện công nghệ và củ cáp sạc số 1 thế giới")
                .avatarUrl("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c")
                .bannerUrl("https://images.unsplash.com/photo-1550009158-9ebf69173e03")
                .status(ShopStatus.ACTIVE)
                .build();
        shopRepository.save(shop2);

        ShippingOption shippingOption2 = ShippingOption.builder()
                .shop(shop2)
                .name("Giao hàng nhanh")
                .shippingFee(BigDecimal.valueOf(25000))
                .isActive(true)
                .build();
        shippingOptionRepository.save(shippingOption2);

        Address shop2Address = Address.builder()
                .shopId(shop2.getId())
                .recipientName("Kho Anker Official")
                .phone("0923456789")
                .province("Thành phố Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("Số 65 Lê Lợi")
                .isDefault(true)
                .build();
        addressRepository.save(shop2Address);

        // 5. CATEGORIES
        Category categoryPhone = Category.builder()
                .name("Điện thoại thông minh")
                .description("Điện thoại thông minh, flagship cao cấp iOS và Android")
                .status(CategoryStatus.ACTIVE)
                .isDeleted(false)
                .build();
        categoryRepository.save(categoryPhone);

        Category categoryCharger = Category.builder()
                .name("Củ cáp & Phụ kiện sạc")
                .description("Củ sạc nhanh GaN, cáp sạc siêu bền, trạm sạc và pin dự phòng")
                .status(CategoryStatus.ACTIVE)
                .isDeleted(false)
                .build();
        categoryRepository.save(categoryCharger);

        Category categoryLaptop = Category.builder()
                .name("Laptop & Máy tính")
                .description("Máy tính xách tay, linh kiện và phụ kiện tin học")
                .status(CategoryStatus.ACTIVE)
                .isDeleted(false)
                .build();
        categoryRepository.save(categoryLaptop);

        // 6. PRODUCTS & VARIANTS
        // Product 1: iPhone 15 Pro Max (Shop 1 - Apple)
        Product productIphone = Product.builder()
                .shop(shop1)
                .category(categoryPhone)
                .brandName("Apple")
                .name("iPhone 15 Pro Max 256GB Chính Hãng VN/A")
                .slug("iphone-15-pro-max-256gb-vn-a")
                .description("iPhone 15 Pro Max thiết kế Titan chuẩn hàng không vũ trụ, chip A17 Pro mạnh mẽ vượt trội.")
                .attributes(Map.of(
                        "Màn hình", "Super Retina XDR OLED 6.7 inch 120Hz ProMotion",
                        "Chipset", "Apple A17 Pro (3nm)",
                        "RAM", "8GB",
                        "Bộ nhớ trong", "256GB",
                        "Camera chính", "Chính 48MP + Góc siêu rộng 12MP + Telephoto 5x 12MP",
                        "Pin & Sạc", "4422 mAh, Sạc nhanh PD 20W, MagSafe 15W",
                        "Chất liệu khung", "Titan chuẩn hàng không vũ trụ",
                        "Kháng nước", "IP68"
                ))
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
        productRepository.save(productIphone);

        ProductImage imgIphone = ProductImage.builder()
                .product(productIphone)
                .imageUrl("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=800&q=80")
                .displayOrder(0)
                .build();
        productImageRepository.save(imgIphone);

        ProductVariant variantIphoneNat = ProductVariant.builder()
                .product(productIphone)
                .sku("IP15PM-256GB-NAT")
                .variantName("Titan Tự Nhiên - 256GB")
                .price(BigDecimal.valueOf(29490000))
                .stockQuantity(100)
                .status(ProductVariantStatus.ACTIVE)
                .imageUrl("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=800&q=80")
                .attributes(Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"))
                .isDeleted(false)
                .build();
        productVariantRepository.save(variantIphoneNat);

        ProductVariant variantIphoneBlue = ProductVariant.builder()
                .product(productIphone)
                .sku("IP15PM-256GB-BLU")
                .variantName("Titan Xanh - 256GB")
                .price(BigDecimal.valueOf(28990000))
                .stockQuantity(100)
                .status(ProductVariantStatus.ACTIVE)
                .imageUrl("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=800&q=80")
                .attributes(Map.of("Màu sắc", "Titan Xanh", "Dung lượng", "256GB"))
                .isDeleted(false)
                .build();
        productVariantRepository.save(variantIphoneBlue);

        // Product 2: Củ sạc Apple 20W (Shop 1 - Apple)
        Product productCharger = Product.builder()
                .shop(shop1)
                .category(categoryCharger)
                .brandName("Apple")
                .name("Củ Sạc Nhanh Apple 20W Type-C Chính Hãng")
                .slug("cu-sac-nhanh-apple-20w-type-c")
                .description("Củ sạc nhanh Apple 20W cổng USB-C tương thích iPhone, iPad.")
                .attributes(Map.of(
                        "Công suất tối đa", "20W",
                        "Số cổng kết nối", "1 cổng USB-C",
                        "Công nghệ sạc", "Power Delivery (PD 3.0)",
                        "Chuẩn tương thích", "iPhone 8 đến iPhone 16 series, iPad Pro/Air/Mini",
                        "Đặc điểm nổi bật", "Chính hãng Apple, bảo vệ chống quá nhiệt"
                ))
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
        productRepository.save(productCharger);

        ProductImage imgCharger = ProductImage.builder()
                .product(productCharger)
                .imageUrl("https://images.unsplash.com/photo-1622445268048-c89b38e07a60")
                .displayOrder(0)
                .build();
        productImageRepository.save(imgCharger);

        ProductVariant variantCharger = ProductVariant.builder()
                .product(productCharger)
                .sku("APPLE-20W-USB-C")
                .variantName("Trắng Tiêu Chuẩn")
                .price(BigDecimal.valueOf(520000))
                .stockQuantity(500)
                .status(ProductVariantStatus.ACTIVE)
                .imageUrl("https://images.unsplash.com/photo-1622445268048-c89b38e07a60")
                .attributes(Map.of("Màu sắc", "Trắng", "Cổng kết nối", "Type-C"))
                .isDeleted(false)
                .build();
        productVariantRepository.save(variantCharger);

        // Product 3: Cáp sạc Anker PowerLine (Shop 2 - Anker)
        Product productAnkerCable = Product.builder()
                .shop(shop2)
                .category(categoryCharger)
                .brandName("Anker")
                .name("Cáp Sạc Anker PowerLine III USB-C to Lightning 0.9m")
                .slug("cap-sac-anker-powerline-iii-usb-c-to-lightning-0-9m")
                .description("Cáp sạc siêu bền Anker chuẩn MFi chứng nhận bởi Apple.")
                .attributes(Map.of(
                        "Công suất tối đa", "Sạc nhanh Power Delivery 60W (3A)",
                        "Số cổng kết nối", "Đầu cắm USB-C to Lightning (0.9 mét)",
                        "Công nghệ sạc", "Hỗ trợ sạc nhanh Power Delivery cho iPhone/iPad",
                        "Chuẩn tương thích", "iPhone, iPad có cổng sạc Lightning",
                        "Đặc điểm nổi bật", "Chứng nhận Apple MFi, vỏ sợi Kevlar chịu uốn cong 25.000 lần"
                ))
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
        productRepository.save(productAnkerCable);

        ProductImage imgAnker = ProductImage.builder()
                .product(productAnkerCable)
                .imageUrl("https://images.unsplash.com/photo-1583863788434-e58a36330cf0")
                .displayOrder(0)
                .build();
        productImageRepository.save(imgAnker);

        ProductVariant variantAnkerBlack = ProductVariant.builder()
                .product(productAnkerCable)
                .sku("ANKER-A8832-BLK")
                .variantName("Màu Đen - 0.9m")
                .price(BigDecimal.valueOf(280000))
                .stockQuantity(300)
                .status(ProductVariantStatus.ACTIVE)
                .imageUrl("https://images.unsplash.com/photo-1583863788434-e58a36330cf0")
                .attributes(Map.of("Màu sắc", "Đen", "Chiều dài", "0.9m"))
                .isDeleted(false)
                .build();
        productVariantRepository.save(variantAnkerBlack);

        // 7. VOUCHERS
        // Platform Order Voucher
        Voucher platOrderVoucher = Voucher.builder()
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("TECHHUB50K")
                .name("Giảm 50K cho đơn hàng từ 500K toàn sàn")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.valueOf(500000))
                .usageLimit(1000)
                .usageLimitPerUser(5)
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(30))
                .isActive(true)
                .isDeleted(false)
                .build();
        voucherRepository.save(platOrderVoucher);

        // Platform Shipping Voucher
        Voucher platShipVoucher = Voucher.builder()
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.SHIPPING_FEE)
                .code("FREESHIP30K")
                .name("Miễn phí vận chuyển 30K toàn sàn")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(30000))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .usageLimit(1000)
                .usageLimitPerUser(5)
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(30))
                .isActive(true)
                .isDeleted(false)
                .build();
        voucherRepository.save(platShipVoucher);

        // Shop 1 Voucher (Apple Store)
        Voucher shop1Voucher = Voucher.builder()
                .shop(shop1)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("APPLE20K")
                .name("Giảm 20K cho đơn từ 500K tại Apple Flagship Store")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(20000))
                .minOrderAmount(BigDecimal.valueOf(500000))
                .usageLimit(500)
                .usageLimitPerUser(2)
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(30))
                .isActive(true)
                .isDeleted(false)
                .build();
        voucherRepository.save(shop1Voucher);

        // Shop 2 Voucher (Anker Store)
        Voucher shop2Voucher = Voucher.builder()
                .shop(shop2)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("ANKER15K")
                .name("Giảm 15K cho đơn từ 200K tại Anker Official Store")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(15000))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .usageLimit(500)
                .usageLimitPerUser(2)
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(30))
                .isActive(true)
                .isDeleted(false)
                .build();
        voucherRepository.save(shop2Voucher);

        // 8. CART & CART ITEMS FOR BUYER
        Cart buyerCart = Cart.builder()
                .user(buyerUser)
                .items(new ArrayList<>())
                .build();
        cartRepository.save(buyerCart);

        // Item 1: Củ Sạc Apple 20W (Qty: 1) -> Shop 1
        CartItem cartItem1 = CartItem.builder()
                .cart(buyerCart)
                .product(productCharger)
                .productVariant(variantCharger)
                .quantity(1)
                .build();
        cartItemRepository.save(cartItem1);

        // Item 2: Cáp Sạc Anker Đen (Qty: 2) -> Shop 2
        CartItem cartItem2 = CartItem.builder()
                .cart(buyerCart)
                .product(productAnkerCable)
                .productVariant(variantAnkerBlack)
                .quantity(2)
                .build();
        cartItemRepository.save(cartItem2);

        // Item 3: iPhone 15 Pro Max Titan Tự Nhiên (Qty: 1) -> Shop 1
        CartItem cartItem3 = CartItem.builder()
                .cart(buyerCart)
                .product(productIphone)
                .productVariant(variantIphoneNat)
                .quantity(1)
                .build();
        cartItemRepository.save(cartItem3);

        // 9. ENSURE SELLER ROLES, EXTENDED PRODUCTS & FULL ORDERS/REVIEWS DATASET
        ensureSellerRolesExist();
        ensureAdditionalCategoriesAndProductsExist();
        ensureAllCategoryAttributesExist();
        ensureProductAndVariantAttributesPopulated();
        ensureComprehensiveOrdersAndReviewsExist();

        log.info("Comprehensive test dataset created successfully!");
        printSummaryLog();
    }

    private void ensureSellerRolesExist() {
        for (String email : List.of("seller.apple@techhub.vn", "seller.anker@techhub.vn", "seller123@techhub.vn")) {
            userRepository.findByEmail(email).ifPresent(user -> {
                boolean changed = false;
                if (!user.getRoles().contains(Role.BUYER)) {
                    user.getRoles().add(Role.BUYER);
                    changed = true;
                }
                if (!user.getRoles().contains(Role.SELLER)) {
                    user.getRoles().add(Role.SELLER);
                    changed = true;
                }
                if (changed) {
                    userRepository.save(user);
                    log.info("Updated roles for {}: {}", email, user.getRoles());
                }
            });
        }
    }

    private void ensureAdditionalCategoriesAndProductsExist() {
        String commonPassword = passwordEncoder.encode("Password123@");

        // 1. Ensure SELLER123 & Shop 3 (TechHub Gear Store) exist
        Shop shop123 = shopRepository.findBySeller_SellerName("seller123-tech-store").orElse(null);
        if (shop123 == null) {
            User seller123User = userRepository.findByEmail("seller123@techhub.vn").orElseGet(() -> {
                User u = new User();
                u.setEmail("seller123@techhub.vn");
                u.setPassword(passwordEncoder.encode("123123"));
                u.setFirstName("Văn Cường");
                u.setLastName("Đỗ");
                u.setPhone("0934567890");
                u.setStatus(UserStatus.ACTIVE);
                u.setEmailVerified(true);
                u.setRoles(new HashSet<>(Set.of(Role.BUYER, Role.SELLER)));
                return userRepository.save(u);
            });

            Seller seller123 = sellerRepository.findByUserId(seller123User.getId()).orElseGet(() -> {
                Seller s = Seller.builder()
                        .user(seller123User)
                        .sellerName("seller123-tech-store")
                        .verificationStatus(SellerVerificationStatus.APPROVED)
                        .bankName("MBBank")
                        .bankAccountNumber("9999123123")
                        .bankAccountHolder("DO VAN CUONG")
                        .balance(BigDecimal.valueOf(8000000))
                        .build();
                return sellerRepository.save(s);
            });

            shop123 = Shop.builder()
                    .seller(seller123)
                    .name("TechHub Gear Store")
                    .description("Phụ kiện, đồ chơi công nghệ, bàn phím cơ và màn hình chuyên nghiệp")
                    .avatarUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f")
                    .bannerUrl("https://images.unsplash.com/photo-1526738549149-8e07eca6c147")
                    .status(ShopStatus.ACTIVE)
                    .build();
            shopRepository.save(shop123);

            ShippingOption shippingOption123 = ShippingOption.builder()
                    .shop(shop123)
                    .name("Giao hàng hỏa tốc")
                    .shippingFee(BigDecimal.valueOf(35000))
                    .isActive(true)
                    .build();
            shippingOptionRepository.save(shippingOption123);

            Address shop123Address = Address.builder()
                    .shopId(shop123.getId())
                    .recipientName("Kho TechHub Gear")
                    .phone("0934567890")
                    .province("Thành phố Hà Nội")
                    .district("Quận Hai Bà Trưng")
                    .ward("Phường Bách Khoa")
                    .streetAddress("Số 1 Đại Cồ Việt")
                    .isDefault(true)
                    .build();
            addressRepository.save(shop123Address);
        }

        Shop appleShop = shopRepository.findBySeller_SellerName("apple-authorized-reseller").orElse(null);
        Shop ankerShop = shopRepository.findBySeller_SellerName("anker-official-store").orElse(null);

        if (appleShop == null || ankerShop == null) {
            log.warn("Apple or Anker shop not found. Skipping additional products creation.");
            return;
        }

        // 2. Ensure Categories exist
        Category categoryPhone = categoryRepository.findByNameIgnoreCase("Điện thoại thông minh")
                .or(() -> categoryRepository.findByNameIgnoreCase("Điện thoại & Phụ kiện"))
                .map(c -> {
                    if (!"Điện thoại thông minh".equalsIgnoreCase(c.getName())) {
                        c.setName("Điện thoại thông minh");
                        c.setDescription("Điện thoại thông minh, flagship cao cấp iOS và Android");
                        return categoryRepository.save(c);
                    }
                    return c;
                })
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Điện thoại thông minh")
                        .description("Điện thoại thông minh, flagship cao cấp iOS và Android")
                        .status(CategoryStatus.ACTIVE)
                        .isDeleted(false)
                        .build()));

        Category categoryCharger = categoryRepository.findByNameIgnoreCase("Củ cáp & Phụ kiện sạc")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Củ cáp & Phụ kiện sạc")
                        .description("Củ sạc nhanh GaN, cáp sạc siêu bền, trạm sạc và pin dự phòng")
                        .status(CategoryStatus.ACTIVE)
                        .isDeleted(false)
                        .build()));

        Category categoryLaptop = categoryRepository.findByNameIgnoreCase("Laptop & Máy tính")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Laptop & Máy tính")
                        .description("Máy tính xách tay cao cấp, MacBook, máy bàn")
                        .status(CategoryStatus.ACTIVE)
                        .isDeleted(false)
                        .build()));

        Category categoryTablet = categoryRepository.findByNameIgnoreCase("Máy tính bảng")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Máy tính bảng")
                        .description("iPad, máy tính bảng thông minh các loại")
                        .status(CategoryStatus.ACTIVE)
                        .isDeleted(false)
                        .build()));

        Category categoryAudio = categoryRepository.findByNameIgnoreCase("Tai nghe & Âm thanh")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Tai nghe & Âm thanh")
                        .description("Tai nghe Bluetooth True Wireless, chụp tai, loa không dây")
                        .status(CategoryStatus.ACTIVE)
                        .isDeleted(false)
                        .build()));

        Category categoryKeyboard = categoryRepository.findByNameIgnoreCase("Bàn phím máy tính")
                .or(() -> categoryRepository.findByNameIgnoreCase("Bàn phím & Chuột công nghệ"))
                .map(c -> {
                    if (!"Bàn phím máy tính".equalsIgnoreCase(c.getName())) {
                        c.setName("Bàn phím máy tính");
                        c.setDescription("Bàn phím cơ Custom, bàn phím không dây, phụ kiện phím cơ");
                        return categoryRepository.save(c);
                    }
                    return c;
                })
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Bàn phím máy tính")
                        .description("Bàn phím cơ Custom, bàn phím không dây, phụ kiện phím cơ")
                        .status(CategoryStatus.ACTIVE)
                        .isDeleted(false)
                        .build()));

        Category categoryMouse = categoryRepository.findByNameIgnoreCase("Chuột máy tính")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Chuột máy tính")
                        .description("Chuột không dây công thái học, chuột gaming cao cấp")
                        .status(CategoryStatus.ACTIVE)
                        .isDeleted(false)
                        .build()));

        Category categoryMonitor = categoryRepository.findByNameIgnoreCase("Màn hình máy tính")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Màn hình máy tính")
                        .description("Màn hình chuyên đồ họa, màn hình gaming 2K 4K")
                        .status(CategoryStatus.ACTIVE)
                        .isDeleted(false)
                        .build()));

        // 3. CREATE ADDITIONAL PRODUCTS & VARIANTS
        // Product 4: MacBook Pro 14 M3 Pro (Apple Shop)
        if (!productRepository.existsBySlug("macbook-pro-14-m3-pro-512gb")) {
            createProductWithVariants(
                    appleShop,
                    categoryLaptop,
                    "Apple",
                    "MacBook Pro 14 M3 Pro 18GB/512GB Chính Hãng Apple VN",
                    "macbook-pro-14-m3-pro-512gb",
                    "MacBook Pro 14 inch trang bị chip Apple M3 Pro với CPU 11 lõi, GPU 14 lõi, màn hình Liquid Retina XDR 120Hz siêu sáng.",
                    Map.of(
                            "Màn hình", "14.2 inch Liquid Retina XDR (3024 x 1964) 120Hz ProMotion",
                            "Chipset", "Apple M3 Pro (11-core CPU, 14-core GPU)",
                            "RAM", "18GB Unified Memory",
                            "Ổ cứng", "512GB SSD PCIe NVMe",
                            "Cổng kết nối", "3 x Thunderbolt 4, HDMI, Jack 3.5mm, Khe thẻ SDXC, MagSafe 3",
                            "Thời lượng pin", "Lên đến 18 giờ xem video",
                            "Hệ điều hành", "macOS Sonoma",
                            "Trọng lượng", "1.61 kg"
                    ),
                    "https://images.unsplash.com/photo-1517336714731-489689fd1ca8",
                    List.of(
                            ProductVariant.builder()
                                    .sku("MBP14-M3P-BLK")
                                    .variantName("Space Black (Đen Không Gian) - 18GB/512GB")
                                    .price(BigDecimal.valueOf(49990000))
                                    .stockQuantity(50)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8")
                                    .attributes(Map.of("Màu sắc", "Space Black", "Dung lượng", "512GB", "RAM", "18GB"))
                                    .isDeleted(false)
                                    .build(),
                            ProductVariant.builder()
                                    .sku("MBP14-M3P-SLV")
                                    .variantName("Silver (Bạc) - 18GB/512GB")
                                    .price(BigDecimal.valueOf(49990000))
                                    .stockQuantity(40)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8")
                                    .attributes(Map.of("Màu sắc", "Silver", "Dung lượng", "512GB", "RAM", "18GB"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 5: Tai Nghe Apple AirPods Pro 2 USB-C (Apple Shop)
        if (!productRepository.existsBySlug("airpods-pro-2-usb-c")) {
            createProductWithVariants(
                    appleShop,
                    categoryAudio,
                    "Apple",
                    "Tai Nghe Apple AirPods Pro 2 USB-C Chính Hãng VN/A",
                    "airpods-pro-2-usb-c",
                    "Tai nghe True Wireless Apple AirPods Pro Gen 2 cổng USB-C, chip H2 khử tiếng ồn chủ động gấp 2 lần, chống bụi nước chuẩn IP54.",
                    Map.of(
                            "Kiểu tai nghe", "In-ear True Wireless",
                            "Công nghệ âm thanh", "Spatial Audio cá nhân hóa, Adaptive EQ",
                            "Chống ồn", "Chống ồn chủ động (ANC) 2X & Xuyên âm thích ứng",
                            "Thời lượng pin", "Lên đến 6 giờ nghe (30 giờ kèm hộp sạc)",
                            "Cổng sạc", "USB-C, Hỗ trợ sạc MagSafe & Qi không dây",
                            "Kết nối", "Bluetooth 5.3, Chip Apple H2 & U1"
                    ),
                    "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434",
                    List.of(
                            ProductVariant.builder()
                                    .sku("APP2-USBC-WHT")
                                    .variantName("Trắng Chuẩn USB-C MagSafe")
                                    .price(BigDecimal.valueOf(5790000))
                                    .stockQuantity(200)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1600294037681-c80b4cb5b434")
                                    .attributes(Map.of("Màu sắc", "Trắng", "Cổng sạc", "USB-C"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 6: iPad Air M2 11 inch Wi-Fi 128GB (Apple Shop)
        if (!productRepository.existsBySlug("ipad-air-m2-11-inch-wifi-128gb")) {
            createProductWithVariants(
                    appleShop,
                    categoryTablet,
                    "Apple",
                    "iPad Air M2 11 inch Wi-Fi 128GB Chính Hãng",
                    "ipad-air-m2-11-inch-wifi-128gb",
                    "iPad Air M2 hoàn toàn mới với màn hình Liquid Retina 11 inch sắc nét, chip M2 đỉnh cao cho đồ họa và AI, hỗ trợ Apple Pencil Pro.",
                    Map.of(
                            "Màn hình", "Liquid Retina 11 inch (2360 x 1640) True Tone 500 nits",
                            "Chipset", "Apple M2 (8-core CPU, 9-core GPU, 16-core Neural Engine)",
                            "RAM", "8GB",
                            "Bộ nhớ trong", "128GB",
                            "Camera sau", "12MP Wide f/1.8 quay phim 4K",
                            "Camera trước", "12MP Ultra Wide góc ngang hỗ trợ Center Stage",
                            "Kết nối", "Wi-Fi 6E, Bluetooth 5.3, USB-C",
                            "Phụ kiện tương thích", "Apple Pencil Pro, Apple Pencil USB-C, Magic Keyboard"
                    ),
                    "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0",
                    List.of(
                            ProductVariant.builder()
                                    .sku("IPADAIR-M2-128-GRY")
                                    .variantName("Space Gray (Xám) - 128GB")
                                    .price(BigDecimal.valueOf(16490000))
                                    .stockQuantity(80)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0")
                                    .attributes(Map.of("Màu sắc", "Space Gray", "Dung lượng", "128GB", "Kết nối", "Wi-Fi"))
                                    .isDeleted(false)
                                    .build(),
                            ProductVariant.builder()
                                    .sku("IPADAIR-M2-128-STA")
                                    .variantName("Starlight (Ánh Sao) - 128GB")
                                    .price(BigDecimal.valueOf(16490000))
                                    .stockQuantity(80)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0")
                                    .attributes(Map.of("Màu sắc", "Starlight", "Dung lượng", "128GB", "Kết nối", "Wi-Fi"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 7: Củ Sạc Nhanh Anker 735 GaNPrime 65W 3 Cổng (Anker Shop)
        if (!productRepository.existsBySlug("cu-sac-nhanh-anker-735-ganprime-65w")) {
            createProductWithVariants(
                    ankerShop,
                    categoryCharger,
                    "Anker",
                    "Củ Sạc Nhanh Anker 735 GaNPrime 65W 3 Cổng (2C1A)",
                    "cu-sac-nhanh-anker-735-ganprime-65w",
                    "Củ sạc nhanh Anker GaNPrime 65W công nghệ PowerIQ 4.0 với 2 cổng USB-C và 1 cổng USB-A sạc cùng lúc laptop, điện thoại và máy tính bảng.",
                    Map.of(
                            "Công suất tối đa", "65W Max",
                            "Số cổng kết nối", "3 cổng (2 USB-C + 1 USB-A)",
                            "Công nghệ sạc", "GaNPrime, PowerIQ 4.0, ActiveShield 2.0",
                            "Chuẩn tương thích", "MacBook, Dell XPS, iPhone, iPad, Samsung",
                            "Đặc điểm nổi bật", "Công nghệ GaN nhỏ hơn 53%, sạc đồng thời 3 thiết bị"
                    ),
                    "https://images.unsplash.com/photo-1583863788434-e58a36330cf0",
                    List.of(
                            ProductVariant.builder()
                                    .sku("ANKER-A2668-BLK")
                                    .variantName("Đen Nhám GaNPrime 65W")
                                    .price(BigDecimal.valueOf(1050000))
                                    .stockQuantity(250)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1583863788434-e58a36330cf0")
                                    .attributes(Map.of("Màu sắc", "Đen", "Công suất", "65W", "Số cổng", "2 Type-C + 1 Type-A"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 8: Trạm Pin Dự Phòng Anker 737 PowerCore 24,000mAh 140W (Anker Shop)
        if (!productRepository.existsBySlug("pin-du-phong-anker-737-powercore-24000mah-140w")) {
            createProductWithVariants(
                    ankerShop,
                    categoryCharger,
                    "Anker",
                    "Pin Dự Phòng Anker 737 PowerCore 24,000mAh 140W 3 Cổng",
                    "pin-du-phong-anker-737-powercore-24000mah-140w",
                    "Pin sạc dự phòng cao cấp Anker 737 hỗ trợ chuẩn Power Delivery 3.1 công suất siêu khủng 140W, màn hình kỹ thuật số hiển thị thông số sạc trực quan.",
                    Map.of(
                            "Công suất tối đa", "140W Two-Way Fast Charging (PD 3.1)",
                            "Số cổng kết nối", "3 cổng (2 USB-C + 1 USB-A)",
                            "Công nghệ sạc", "Power Delivery 3.1 hai chiều siêu tốc 140W",
                            "Chuẩn tương thích", "MacBook Pro 16, Dell, iPhone, iPad, Steam Deck",
                            "Đặc điểm nổi bật", "Dung lượng khủng 24.000mAh, màn hình OLED màu kỹ thuật số thông minh"
                    ),
                    "https://images.unsplash.com/photo-1609592426507-cc09072a2e4e",
                    List.of(
                            ProductVariant.builder()
                                    .sku("ANKER-A1289-GRY")
                                    .variantName("Xám Đen - Màn Hình OLED 140W")
                                    .price(BigDecimal.valueOf(2790000))
                                    .stockQuantity(120)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1609592426507-cc09072a2e4e")
                                    .attributes(Map.of("Màu sắc", "Xám Đen", "Dung lượng", "24000mAh", "Công suất", "140W"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 9: Tai Nghe Chống Ồn Anker Soundcore Space One (Anker Shop)
        if (!productRepository.existsBySlug("tai-nghe-chong-on-anker-soundcore-space-one")) {
            createProductWithVariants(
                    ankerShop,
                    categoryAudio,
                    "Anker",
                    "Tai Nghe Chụp Tai Bluetooth Anker Soundcore Space One Hi-Res LDAC",
                    "tai-nghe-chong-on-anker-soundcore-space-one",
                    "Tai nghe chụp tai Bluetooth Anker Soundcore Space One khử ồn chủ động 2X, chuẩn âm thanh Hi-Res Audio Wireless LDAC, pin 55 giờ.",
                    Map.of(
                            "Kiểu tai nghe", "Over-ear (Chụp tai)",
                            "Công nghệ âm thanh", "Hi-Res Audio, Hi-Res Wireless, LDAC codec",
                            "Chống ồn", "Chống ồn chủ động thích ứng (Adaptive ANC) khử giọng nói 2X",
                            "Thời lượng pin", "40 giờ (bật ANC), 55 giờ (tắt ANC)",
                            "Cổng sạc", "USB-C, Sạc nhanh 5 phút nghe 4 giờ",
                            "Kết nối", "Bluetooth 5.3, Đa điểm Multipoint, Cáp AUX 3.5mm"
                    ),
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e",
                    List.of(
                            ProductVariant.builder()
                                    .sku("ANKER-A3062-BLK")
                                    .variantName("Jet Black (Đen Huyền Bí)")
                                    .price(BigDecimal.valueOf(1990000))
                                    .stockQuantity(100)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e")
                                    .attributes(Map.of("Màu sắc", "Jet Black", "Kiểu dáng", "Over-ear"))
                                    .isDeleted(false)
                                    .build(),
                            ProductVariant.builder()
                                    .sku("ANKER-A3062-BLU")
                                    .variantName("Sky Blue (Xanh Thiên Thanh)")
                                    .price(BigDecimal.valueOf(1990000))
                                    .stockQuantity(80)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e")
                                    .attributes(Map.of("Màu sắc", "Sky Blue", "Kiểu dáng", "Over-ear"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 10: Bàn Phím Cơ Không Dây Keychron K2 Pro QMK/VIA (Shop 3: TechHub Gear Store)
        if (!productRepository.existsBySlug("ban-phim-co-keychron-k2-pro")) {
            createProductWithVariants(
                    shop123,
                    categoryKeyboard,
                    "Keychron",
                    "Bàn Phím Cơ Không Dây Keychron K2 Pro QMK/VIA Hotswap RGB",
                    "ban-phim-co-keychron-k2-pro",
                    "Bàn phím cơ Custom không dây Keychron K2 Pro layout 75%, hỗ trợ QMK/VIA keymap tùy biến, mạch xuôi Hotswap, foam tiêu âm cao cấp.",
                    Map.of(
                            "Layout", "75% (84 phím)",
                            "Switch", "Keychron K Pro (Hỗ trợ Hotswap 3-pin & 5-pin)",
                            "Keycap", "OSA Profile PBT Double-shot xuyên led",
                            "Khả năng tùy biến", "Hỗ trợ phần mềm QMK/VIA keymap và macro đầy đủ",
                            "Đèn nền", "RGB 22 chế độ hiệu ứng, mạch xuôi South-facing",
                            "Kết nối", "Bluetooth 5.1 (kết nối tối đa 3 thiết bị) & Type-C có dây",
                            "Dung lượng pin", "4000 mAh (dùng đến 300 giờ khi tắt led)",
                            "Hệ điều hành tương thích", "macOS / Windows / Linux / Android"
                    ),
                    "https://images.unsplash.com/photo-1587829741301-dc798b83add3",
                    List.of(
                            ProductVariant.builder()
                                    .sku("KEYCHRON-K2P-RED")
                                    .variantName("Red Switch (Êm Ái) - Led RGB Hotswap")
                                    .price(BigDecimal.valueOf(2250000))
                                    .stockQuantity(150)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3")
                                    .attributes(Map.of("Loại Switch", "Red Switch (Linear)", "Đèn LED", "RGB Hotswap", "Layout", "75%"))
                                    .isDeleted(false)
                                    .build(),
                            ProductVariant.builder()
                                    .sku("KEYCHRON-K2P-BRN")
                                    .variantName("Brown Switch (Khấc Nhẹ) - Led RGB Hotswap")
                                    .price(BigDecimal.valueOf(2250000))
                                    .stockQuantity(150)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3")
                                    .attributes(Map.of("Loại Switch", "Brown Switch (Tactile)", "Đèn LED", "RGB Hotswap", "Layout", "75%"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 11: Chuột Không Dây Logitech MX Master 3S (Shop 3: TechHub Gear Store)
        if (!productRepository.existsBySlug("chuot-logitech-mx-master-3s")) {
            createProductWithVariants(
                    shop123,
                    categoryMouse,
                    "Logitech",
                    "Chuột Không Dây Công Thái Học Logitech MX Master 3S 8000 DPI",
                    "chuot-logitech-mx-master-3s",
                    "Chuột công thái học hàng đầu cho dân văn phòng và lập trình viên Logitech MX Master 3S, cảm biến 8000 DPI trên mọi mặt phẳng kính, click chống ồn Quiet Click.",
                    Map.of(
                            "Độ phân giải cảm biến", "200 đến 8000 DPI (công nghệ Darkfield di chuyển trên mọi bề mặt kể cả kính)",
                            "Nút bấm", "Quiet Click giảm 90% tiếng ồn click chuột",
                            "Cuộn siêu tốc", "Con lăn MagSpeed điện từ cuộn 1000 dòng/giây",
                            "Con lăn ngón cái", "Hỗ trợ cuộn ngang (Horizontal scroll)",
                            "Kết nối", "Bluetooth Low Energy & Logi Bolt USB Receiver",
                            "Phần mềm hỗ trợ", "Logi Options+ (Tùy biến theo từng ứng dụng, Logitech Flow chuyển dữ liệu giữa các máy)",
                            "Thời lượng pin", "Lên đến 70 ngày sau một lần sạc đầy qua USB-C",
                            "Trọng lượng", "141g"
                    ),
                    "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7",
                    List.of(
                            ProductVariant.builder()
                                    .sku("LOGI-MXM3S-GRY")
                                    .variantName("Graphite (Xám Đen Đậm)")
                                    .price(BigDecimal.valueOf(2190000))
                                    .stockQuantity(200)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7")
                                    .attributes(Map.of("Màu sắc", "Graphite", "Độ nhạy", "8000 DPI", "Kết nối", "Bluetooth + Logi Bolt"))
                                    .isDeleted(false)
                                    .build(),
                            ProductVariant.builder()
                                    .sku("LOGI-MXM3S-WHT")
                                    .variantName("Pale Gray (Trắng Xám Nhạt)")
                                    .price(BigDecimal.valueOf(2190000))
                                    .stockQuantity(150)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7")
                                    .attributes(Map.of("Màu sắc", "Pale Gray", "Độ nhạy", "8000 DPI", "Kết nối", "Bluetooth + Logi Bolt"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 12: Màn Hình Dell UltraSharp U2724D 27 inch 2K (Shop 3: TechHub Gear Store)
        if (!productRepository.existsBySlug("man-hinh-dell-ultrasharp-u2724d-27-inch-2k")) {
            createProductWithVariants(
                    shop123,
                    categoryMonitor,
                    "Dell",
                    "Màn Hình Đồ Họa Dell UltraSharp U2724D 27 inch 2K 120Hz IPS Black",
                    "man-hinh-dell-ultrasharp-u2724d-27-inch-2k",
                    "Màn hình Dell UltraSharp 27 inch độ phân giải 2K QHD, tấm nền IPS Black tương phản 2000:1, tần số quét 120Hz, đạt 100% sRGB và 98% DCI-P3.",
                    Map.of(
                            "Kích thước màn hình", "27 inch 16:9",
                            "Độ phân giải", "2K QHD (2560 x 1440)",
                            "Tấm nền & Tương phản", "IPS Black, Tương phản tĩnh 2000:1",
                            "Tần số quét", "120Hz mượt mà",
                            "Độ bao phủ màu", "100% sRGB, 100% BT.709, 98% Display P3, Delta E < 2",
                            "Công nghệ bảo vệ mắt", "Cảm biến ánh sáng môi trường tự điều chỉnh nhiệt độ màu, ComfortView Plus",
                            "Cổng kết nối", "DisplayPort 1.4, HDMI 2.1, USB-C (Data 10Gbps), USB-A Hub 10Gbps",
                            "Chân đế", "Điều chỉnh độ cao, xoay dọc 90 độ, xoay trái phải, nghiêng"
                    ),
                    "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf",
                    List.of(
                            ProductVariant.builder()
                                    .sku("DELL-U2724D-SLV")
                                    .variantName("Bạc Tiêu Chuẩn IPS Black 120Hz")
                                    .price(BigDecimal.valueOf(11490000))
                                    .stockQuantity(60)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf")
                                    .attributes(Map.of("Màu sắc", "Silver", "Kích thước", "27 inch", "Độ phân giải", "2K QHD 120Hz"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        // Product 13: Samsung Galaxy S24 Ultra 256GB (Shop 3: TechHub Gear Store)
        if (!productRepository.existsBySlug("samsung-galaxy-s24-ultra-256gb")) {
            createProductWithVariants(
                    shop123,
                    categoryPhone,
                    "Samsung",
                    "Samsung Galaxy S24 Ultra 12GB/256GB Khung Viền Titan",
                    "samsung-galaxy-s24-ultra-256gb",
                    "Samsung Galaxy S24 Ultra đỉnh cao quyền năng Galaxy AI, khung viền Titan bền bỉ, bút S-Pen tích hợp, camera mắt thần bóng đêm 200MP.",
                    Map.of(
                            "Màn hình", "Dynamic AMOLED 2X 6.8 inch QHD+, 1-120Hz LTPO, Kính Gorilla Armor chống chói",
                            "Chipset", "Qualcomm Snapdragon 8 Gen 3 for Galaxy",
                            "RAM", "12GB LPDDR5X",
                            "Bộ nhớ trong", "256GB UFS 4.0",
                            "Camera chính", "200MP f/1.7 OIS + 50MP Periscope 5x + 10MP Tele 3x + 12MP Ultra-wide",
                            "Pin & Sạc", "5000 mAh, Sạc nhanh có dây 45W, Sạc không dây 15W",
                            "Chất liệu khung", "Khung viền Titanium chuẩn hàng không vũ trụ",
                            "Kháng nước", "IP68"
                    ),
                    "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf",
                    List.of(
                            ProductVariant.builder()
                                    .sku("SS-S24U-256-GRY")
                                    .variantName("Titanium Gray (Xám Titan) - 256GB")
                                    .price(BigDecimal.valueOf(27990000))
                                    .stockQuantity(90)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1610945265064-0e34e5519bbf")
                                    .attributes(Map.of("Màu sắc", "Titanium Gray", "Dung lượng", "256GB", "RAM", "12GB"))
                                    .isDeleted(false)
                                    .build(),
                            ProductVariant.builder()
                                    .sku("SS-S24U-256-BLK")
                                    .variantName("Titanium Black (Đen Titan) - 256GB")
                                    .price(BigDecimal.valueOf(27990000))
                                    .stockQuantity(90)
                                    .status(ProductVariantStatus.ACTIVE)
                                    .imageUrl("https://images.unsplash.com/photo-1610945265064-0e34e5519bbf")
                                    .attributes(Map.of("Màu sắc", "Titanium Black", "Dung lượng", "256GB", "RAM", "12GB"))
                                    .isDeleted(false)
                                    .build()
                    )
            );
        }

        log.info("Additional categories and 10 products created successfully!");
    }

    private Product createProductWithVariants(
            Shop shop,
            Category category,
            String brandName,
            String name,
            String slug,
            String description,
            Map<String, Object> attributes,
            String mainImageUrl,
            List<ProductVariant> variants
    ) {
        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .brandName(brandName)
                .name(name)
                .slug(slug)
                .description(description)
                .attributes(attributes)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
        productRepository.save(product);

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(mainImageUrl)
                .displayOrder(0)
                .build();
        productImageRepository.save(image);

        for (ProductVariant v : variants) {
            v.setProduct(product);
            productVariantRepository.save(v);
        }
        return product;
    }

    private ProductVariant findVariantBySku(String sku) {
        return productVariantRepository.findAll().stream()
                .filter(v -> sku.equalsIgnoreCase(v.getSku()))
                .findFirst().orElse(null);
    }

    private void ensureCategoryAttribute(
            Category category,
            String name,
            CategoryAttributeDataType dataType,
            List<String> options,
            String sampleValue
    ) {
        if (!categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(category.getId(), name)) {
            CategoryAttribute attr = CategoryAttribute.builder()
                    .category(category)
                    .name(name)
                    .dataType(dataType)
                    .options(options)
                    .sampleValue(sampleValue)
                    .build();
            categoryAttributeRepository.save(attr);
            log.info("Created CategoryAttribute '{}' for category '{}'", name, category.getName());
        }
    }

    private void ensureAllCategoryAttributesExist() {
        log.info("Ensuring category attributes match product technical specifications schema...");

        // 1. Điện thoại thông minh
        Category categoryPhone = categoryRepository.findByNameIgnoreCase("Điện thoại thông minh")
                .or(() -> categoryRepository.findByNameIgnoreCase("Điện thoại & Phụ kiện"))
                .orElse(null);
        if (categoryPhone != null) {
            categoryAttributeRepository.findByCategoryId(categoryPhone.getId()).stream()
                    .filter(a -> "Màu sắc".equalsIgnoreCase(a.getName()))
                    .findFirst()
                    .ifPresent(categoryAttributeRepository::delete);

            ensureCategoryAttribute(categoryPhone, "Màn hình", CategoryAttributeDataType.TEXT, null, "6.7 inch Super Retina XDR OLED 120Hz ProMotion");
            ensureCategoryAttribute(categoryPhone, "Chipset", CategoryAttributeDataType.TEXT, null, "Apple A17 Pro (3nm) / Snapdragon 8 Gen 3");
            ensureCategoryAttribute(categoryPhone, "RAM", CategoryAttributeDataType.TEXT, null, "8GB / 12GB LPDDR5X");
            ensureCategoryAttribute(categoryPhone, "Bộ nhớ trong", CategoryAttributeDataType.TEXT, null, "256GB / 512GB UFS 4.0");
            ensureCategoryAttribute(categoryPhone, "Camera chính", CategoryAttributeDataType.TEXT, null, "Chính 48MP + 12MP Tele 5x / 200MP OIS");
            ensureCategoryAttribute(categoryPhone, "Pin & Sạc", CategoryAttributeDataType.TEXT, null, "4422 mAh / 5000 mAh, sạc nhanh PD 20W/45W");
            ensureCategoryAttribute(categoryPhone, "Chất liệu khung", CategoryAttributeDataType.TEXT, null, "Titan chuẩn hàng không vũ trụ");
            ensureCategoryAttribute(categoryPhone, "Kháng nước", CategoryAttributeDataType.TEXT, null, "IP68");
        }

        // 2. Củ cáp & Phụ kiện sạc
        Category categoryCharger = categoryRepository.findByNameIgnoreCase("Củ cáp & Phụ kiện sạc").orElse(null);
        if (categoryCharger != null) {
            ensureCategoryAttribute(categoryCharger, "Công suất tối đa", CategoryAttributeDataType.TEXT, null, "20W / 65W GaN / 140W PD 3.1");
            ensureCategoryAttribute(categoryCharger, "Số cổng kết nối", CategoryAttributeDataType.TEXT, null, "1 USB-C / 3 cổng (2 Type-C + 1 Type-A)");
            ensureCategoryAttribute(categoryCharger, "Công nghệ sạc", CategoryAttributeDataType.TEXT, null, "Power Delivery 3.0 / GaNPrime / PowerIQ 4.0");
            ensureCategoryAttribute(categoryCharger, "Chuẩn tương thích", CategoryAttributeDataType.TEXT, null, "iPhone, iPad, MacBook, Android, Laptop");
            ensureCategoryAttribute(categoryCharger, "Đặc điểm nổi bật", CategoryAttributeDataType.TEXT, null, "Chứng nhận Apple MFi / Màn hình màu OLED thông minh");
        }

        // 3. Laptop & Máy tính
        Category categoryLaptop = categoryRepository.findByNameIgnoreCase("Laptop & Máy tính").orElse(null);
        if (categoryLaptop != null) {
            ensureCategoryAttribute(categoryLaptop, "Màn hình", CategoryAttributeDataType.TEXT, null, "14.2 inch Liquid Retina XDR (3024 x 1964) 120Hz");
            ensureCategoryAttribute(categoryLaptop, "Chipset", CategoryAttributeDataType.TEXT, null, "Apple M3 Pro (11-core CPU, 14-core GPU)");
            ensureCategoryAttribute(categoryLaptop, "RAM", CategoryAttributeDataType.TEXT, null, "18GB Unified Memory");
            ensureCategoryAttribute(categoryLaptop, "Ổ cứng", CategoryAttributeDataType.TEXT, null, "512GB SSD PCIe NVMe");
            ensureCategoryAttribute(categoryLaptop, "Cổng kết nối", CategoryAttributeDataType.TEXT, null, "3 x Thunderbolt 4, HDMI, Jack 3.5mm, MagSafe 3");
            ensureCategoryAttribute(categoryLaptop, "Thời lượng pin", CategoryAttributeDataType.TEXT, null, "Lên đến 18 giờ xem video");
            ensureCategoryAttribute(categoryLaptop, "Hệ điều hành", CategoryAttributeDataType.TEXT, null, "macOS Sonoma");
            ensureCategoryAttribute(categoryLaptop, "Trọng lượng", CategoryAttributeDataType.TEXT, null, "1.61 kg");
        }

        // 4. Máy tính bảng
        Category categoryTablet = categoryRepository.findByNameIgnoreCase("Máy tính bảng").orElse(null);
        if (categoryTablet != null) {
            ensureCategoryAttribute(categoryTablet, "Màn hình", CategoryAttributeDataType.TEXT, null, "Liquid Retina 11 inch (2360 x 1640) 500 nits");
            ensureCategoryAttribute(categoryTablet, "Chipset", CategoryAttributeDataType.TEXT, null, "Apple M2 (8-core CPU, 9-core GPU)");
            ensureCategoryAttribute(categoryTablet, "RAM", CategoryAttributeDataType.TEXT, null, "8GB");
            ensureCategoryAttribute(categoryTablet, "Bộ nhớ trong", CategoryAttributeDataType.TEXT, null, "128GB");
            ensureCategoryAttribute(categoryTablet, "Camera sau", CategoryAttributeDataType.TEXT, null, "12MP Wide f/1.8 quay phim 4K");
            ensureCategoryAttribute(categoryTablet, "Camera trước", CategoryAttributeDataType.TEXT, null, "12MP Ultra Wide hỗ trợ Center Stage");
            ensureCategoryAttribute(categoryTablet, "Kết nối", CategoryAttributeDataType.TEXT, null, "Wi-Fi 6E, Bluetooth 5.3, USB-C");
            ensureCategoryAttribute(categoryTablet, "Phụ kiện tương thích", CategoryAttributeDataType.TEXT, null, "Apple Pencil Pro, Magic Keyboard");
        }

        // 5. Tai nghe & Âm thanh
        Category categoryAudio = categoryRepository.findByNameIgnoreCase("Tai nghe & Âm thanh").orElse(null);
        if (categoryAudio != null) {
            ensureCategoryAttribute(categoryAudio, "Kiểu tai nghe", CategoryAttributeDataType.TEXT, null, "In-ear True Wireless / Over-ear");
            ensureCategoryAttribute(categoryAudio, "Công nghệ âm thanh", CategoryAttributeDataType.TEXT, null, "Spatial Audio, Hi-Res Audio Wireless LDAC");
            ensureCategoryAttribute(categoryAudio, "Chống ồn", CategoryAttributeDataType.TEXT, null, "Chống ồn chủ động (ANC) 2X thích ứng");
            ensureCategoryAttribute(categoryAudio, "Thời lượng pin", CategoryAttributeDataType.TEXT, null, "6 giờ (30 giờ kèm hộp sạc) / 55 giờ");
            ensureCategoryAttribute(categoryAudio, "Cổng sạc", CategoryAttributeDataType.TEXT, null, "USB-C, Hỗ trợ sạc MagSafe & Qi");
            ensureCategoryAttribute(categoryAudio, "Kết nối", CategoryAttributeDataType.TEXT, null, "Bluetooth 5.3");
        }

        // 6. Bàn phím máy tính
        Category categoryKeyboard = categoryRepository.findByNameIgnoreCase("Bàn phím máy tính")
                .or(() -> categoryRepository.findByNameIgnoreCase("Bàn phím & Chuột công nghệ"))
                .orElse(null);
        if (categoryKeyboard != null) {
            ensureCategoryAttribute(categoryKeyboard, "Layout", CategoryAttributeDataType.TEXT, null, "75% (84 phím)");
            ensureCategoryAttribute(categoryKeyboard, "Switch", CategoryAttributeDataType.TEXT, null, "Keychron K Pro (Hotswap 3-pin & 5-pin)");
            ensureCategoryAttribute(categoryKeyboard, "Keycap", CategoryAttributeDataType.TEXT, null, "OSA Profile PBT Double-shot");
            ensureCategoryAttribute(categoryKeyboard, "Khả năng tùy biến", CategoryAttributeDataType.TEXT, null, "Hỗ trợ phần mềm QMK/VIA keymap và macro đầy đủ");
            ensureCategoryAttribute(categoryKeyboard, "Đèn nền", CategoryAttributeDataType.TEXT, null, "RGB 22 chế độ hiệu ứng, mạch xuôi South-facing");
            ensureCategoryAttribute(categoryKeyboard, "Kết nối", CategoryAttributeDataType.TEXT, null, "Bluetooth 5.1 & Type-C có dây");
            ensureCategoryAttribute(categoryKeyboard, "Dung lượng pin", CategoryAttributeDataType.TEXT, null, "4000 mAh (dùng đến 300 giờ)");
            ensureCategoryAttribute(categoryKeyboard, "Hệ điều hành tương thích", CategoryAttributeDataType.TEXT, null, "macOS / Windows / Linux / Android");
        }

        // 7. Chuột máy tính
        Category categoryMouse = categoryRepository.findByNameIgnoreCase("Chuột máy tính").orElse(null);
        if (categoryMouse != null) {
            ensureCategoryAttribute(categoryMouse, "Độ phân giải cảm biến", CategoryAttributeDataType.TEXT, null, "200 đến 8000 DPI Darkfield");
            ensureCategoryAttribute(categoryMouse, "Nút bấm", CategoryAttributeDataType.TEXT, null, "Quiet Click giảm 90% tiếng ồn click chuột");
            ensureCategoryAttribute(categoryMouse, "Cuộn siêu tốc", CategoryAttributeDataType.TEXT, null, "Con lăn MagSpeed điện từ cuộn 1000 dòng/giây");
            ensureCategoryAttribute(categoryMouse, "Con lăn ngón cái", CategoryAttributeDataType.TEXT, null, "Hỗ trợ cuộn ngang Horizontal scroll");
            ensureCategoryAttribute(categoryMouse, "Kết nối", CategoryAttributeDataType.TEXT, null, "Bluetooth Low Energy & Logi Bolt USB Receiver");
            ensureCategoryAttribute(categoryMouse, "Phần mềm hỗ trợ", CategoryAttributeDataType.TEXT, null, "Logi Options+ & Logitech Flow");
            ensureCategoryAttribute(categoryMouse, "Thời lượng pin", CategoryAttributeDataType.TEXT, null, "Lên đến 70 ngày sau một lần sạc đầy qua USB-C");
            ensureCategoryAttribute(categoryMouse, "Trọng lượng", CategoryAttributeDataType.TEXT, null, "141g");
        }

        // 8. Màn hình máy tính
        Category categoryMonitor = categoryRepository.findByNameIgnoreCase("Màn hình máy tính").orElse(null);
        if (categoryMonitor != null) {
            ensureCategoryAttribute(categoryMonitor, "Kích thước màn hình", CategoryAttributeDataType.TEXT, null, "27 inch 16:9");
            ensureCategoryAttribute(categoryMonitor, "Độ phân giải", CategoryAttributeDataType.TEXT, null, "2K QHD (2560 x 1440)");
            ensureCategoryAttribute(categoryMonitor, "Tấm nền & Tương phản", CategoryAttributeDataType.TEXT, null, "IPS Black, Tương phản tĩnh 2000:1");
            ensureCategoryAttribute(categoryMonitor, "Tần số quét", CategoryAttributeDataType.TEXT, null, "120Hz mượt mà");
            ensureCategoryAttribute(categoryMonitor, "Độ bao phủ màu", CategoryAttributeDataType.TEXT, null, "100% sRGB, 100% BT.709, 98% Display P3, Delta E < 2");
            ensureCategoryAttribute(categoryMonitor, "Công nghệ bảo vệ mắt", CategoryAttributeDataType.TEXT, null, "Cảm biến ánh sáng môi trường, ComfortView Plus");
            ensureCategoryAttribute(categoryMonitor, "Cổng kết nối", CategoryAttributeDataType.TEXT, null, "DisplayPort 1.4, HDMI 2.1, USB-C 10Gbps");
            ensureCategoryAttribute(categoryMonitor, "Chân đế", CategoryAttributeDataType.TEXT, null, "Điều chỉnh độ cao, xoay dọc 90 độ, nghiêng");
        }
    }

    private void ensureProductAndVariantAttributesPopulated() {
        log.info("Checking and populating dynamic attributes for products and variants...");

        // Ensure product category mappings are clean and aligned with schemas
        Map<String, String> productCategoryMap = Map.of(
                "iphone-15-pro-max-256gb-vn-a", "Điện thoại thông minh",
                "samsung-galaxy-s24-ultra-256gb", "Điện thoại thông minh",
                "cu-sac-nhanh-apple-20w-type-c", "Củ cáp & Phụ kiện sạc",
                "cap-sac-anker-powerline-iii-usb-c-to-lightning-0-9m", "Củ cáp & Phụ kiện sạc",
                "cu-sac-nhanh-anker-735-ganprime-65w", "Củ cáp & Phụ kiện sạc",
                "pin-du-phong-anker-737-powercore-24000mah-140w", "Củ cáp & Phụ kiện sạc",
                "macbook-pro-14-m3-pro-512gb", "Laptop & Máy tính",
                "ipad-air-m2-11-inch-wifi-128gb", "Máy tính bảng",
                "airpods-pro-2-usb-c", "Tai nghe & Âm thanh",
                "tai-nghe-chong-on-anker-soundcore-space-one", "Tai nghe & Âm thanh"
        );
        Map<String, String> productCategoryMap2 = Map.of(
                "ban-phim-co-keychron-k2-pro", "Bàn phím máy tính",
                "chuot-logitech-mx-master-3s", "Chuột máy tính",
                "man-hinh-dell-ultrasharp-u2724d-27-inch-2k", "Màn hình máy tính"
        );
        Map<String, String> allProductCategoryMap = new HashMap<>(productCategoryMap);
        allProductCategoryMap.putAll(productCategoryMap2);

        for (Map.Entry<String, String> entry : allProductCategoryMap.entrySet()) {
            productRepository.findAll().stream()
                    .filter(p -> entry.getKey().equalsIgnoreCase(p.getSlug()))
                    .findFirst()
                    .ifPresent(p -> {
                        Category targetCat = categoryRepository.findByNameIgnoreCase(entry.getValue())
                                .or(() -> {
                                    if ("Điện thoại thông minh".equalsIgnoreCase(entry.getValue())) {
                                        return categoryRepository.findByNameIgnoreCase("Điện thoại & Phụ kiện");
                                    } else if ("Bàn phím máy tính".equalsIgnoreCase(entry.getValue())) {
                                        return categoryRepository.findByNameIgnoreCase("Bàn phím & Chuột công nghệ");
                                    }
                                    return Optional.empty();
                                })
                                .orElse(null);
                        if (targetCat != null && (p.getCategory() == null || !targetCat.getId().equals(p.getCategory().getId()))) {
                            p.setCategory(targetCat);
                            productRepository.save(p);
                            log.info("Reassigned product '{}' to category '{}'", p.getName(), targetCat.getName());
                        }
                    });
        }

        Map<String, Map<String, Object>> productSpecsMapPart1 = Map.of(
                "iphone-15-pro-max-256gb-vn-a", Map.of(
                        "Màn hình", "Super Retina XDR OLED 6.7 inch 120Hz ProMotion",
                        "Chipset", "Apple A17 Pro (3nm)",
                        "RAM", "8GB",
                        "Bộ nhớ trong", "256GB",
                        "Camera chính", "Chính 48MP + Góc siêu rộng 12MP + Telephoto 5x 12MP",
                        "Pin & Sạc", "4422 mAh, Sạc nhanh PD 20W, MagSafe 15W",
                        "Chất liệu khung", "Titan chuẩn hàng không vũ trụ",
                        "Kháng nước", "IP68"
                ),
                "cu-sac-nhanh-apple-20w-type-c", Map.of(
                        "Công suất tối đa", "20W",
                        "Số cổng kết nối", "1 cổng USB-C",
                        "Công nghệ sạc", "Power Delivery (PD 3.0)",
                        "Chuẩn tương thích", "iPhone 8 đến iPhone 16 series, iPad Pro/Air/Mini",
                        "Đặc điểm nổi bật", "Chính hãng Apple, bảo vệ chống quá nhiệt"
                ),
                "cap-sac-anker-powerline-iii-usb-c-to-lightning-0-9m", Map.of(
                        "Công suất tối đa", "Sạc nhanh Power Delivery 60W (3A)",
                        "Số cổng kết nối", "Đầu cắm USB-C to Lightning (0.9 mét)",
                        "Công nghệ sạc", "Hỗ trợ sạc nhanh Power Delivery cho iPhone/iPad",
                        "Chuẩn tương thích", "iPhone, iPad có cổng sạc Lightning",
                        "Đặc điểm nổi bật", "Chứng nhận Apple MFi, vỏ sợi Kevlar chịu uốn cong 25.000 lần"
                ),
                "macbook-pro-14-m3-pro-512gb", Map.of(
                        "Màn hình", "14.2 inch Liquid Retina XDR (3024 x 1964) 120Hz ProMotion",
                        "Chipset", "Apple M3 Pro (11-core CPU, 14-core GPU)",
                        "RAM", "18GB Unified Memory",
                        "Ổ cứng", "512GB SSD PCIe NVMe",
                        "Cổng kết nối", "3 x Thunderbolt 4, HDMI, Jack 3.5mm, Khe thẻ SDXC, MagSafe 3",
                        "Thời lượng pin", "Lên đến 18 giờ xem video",
                        "Hệ điều hành", "macOS Sonoma",
                        "Trọng lượng", "1.61 kg"
                ),
                "airpods-pro-2-usb-c", Map.of(
                        "Kiểu tai nghe", "In-ear True Wireless",
                        "Công nghệ âm thanh", "Spatial Audio cá nhân hóa, Adaptive EQ",
                        "Chống ồn", "Chống ồn chủ động (ANC) 2X & Xuyên âm thích ứng",
                        "Thời lượng pin", "Lên đến 6 giờ nghe (30 giờ kèm hộp sạc)",
                        "Cổng sạc", "USB-C, Hỗ trợ sạc MagSafe & Qi không dây",
                        "Kết nối", "Bluetooth 5.3, Chip Apple H2 & U1"
                ),
                "ipad-air-m2-11-inch-wifi-128gb", Map.of(
                        "Màn hình", "Liquid Retina 11 inch (2360 x 1640) True Tone 500 nits",
                        "Chipset", "Apple M2 (8-core CPU, 9-core GPU, 16-core Neural Engine)",
                        "RAM", "8GB",
                        "Bộ nhớ trong", "128GB",
                        "Camera sau", "12MP Wide f/1.8 quay phim 4K",
                        "Camera trước", "12MP Ultra Wide góc ngang hỗ trợ Center Stage",
                        "Kết nối", "Wi-Fi 6E, Bluetooth 5.3, USB-C",
                        "Phụ kiện tương thích", "Apple Pencil Pro, Apple Pencil USB-C, Magic Keyboard"
                ),
                "cu-sac-nhanh-anker-735-ganprime-65w", Map.of(
                        "Công suất tối đa", "65W Max",
                        "Số cổng kết nối", "3 cổng (2 USB-C + 1 USB-A)",
                        "Công nghệ sạc", "GaNPrime, PowerIQ 4.0, ActiveShield 2.0",
                        "Chuẩn tương thích", "MacBook, Dell XPS, iPhone, iPad, Samsung",
                        "Đặc điểm nổi bật", "Công nghệ GaN nhỏ hơn 53%, sạc đồng thời 3 thiết bị"
                ),
                "pin-du-phong-anker-737-powercore-24000mah-140w", Map.of(
                        "Công suất tối đa", "140W Two-Way Fast Charging (PD 3.1)",
                        "Số cổng kết nối", "3 cổng (2 USB-C + 1 USB-A)",
                        "Công nghệ sạc", "Power Delivery 3.1 hai chiều siêu tốc 140W",
                        "Chuẩn tương thích", "MacBook Pro 16, Dell, iPhone, iPad, Steam Deck",
                        "Đặc điểm nổi bật", "Dung lượng khủng 24.000mAh, màn hình OLED màu kỹ thuật số thông minh"
                ),
                "tai-nghe-chong-on-anker-soundcore-space-one", Map.of(
                        "Kiểu tai nghe", "Over-ear (Chụp tai)",
                        "Công nghệ âm thanh", "Hi-Res Audio, Hi-Res Wireless, LDAC codec",
                        "Chống ồn", "Chống ồn chủ động thích ứng (Adaptive ANC) khử giọng nói 2X",
                        "Thời lượng pin", "40 giờ (bật ANC), 55 giờ (tắt ANC)",
                        "Cổng sạc", "USB-C, Sạc nhanh 5 phút nghe 4 giờ",
                        "Kết nối", "Bluetooth 5.3, Đa điểm Multipoint, Cáp AUX 3.5mm"
                ),
                "ban-phim-co-keychron-k2-pro", Map.of(
                        "Layout", "75% (84 phím)",
                        "Switch", "Keychron K Pro (Hỗ trợ Hotswap 3-pin & 5-pin)",
                        "Keycap", "OSA Profile PBT Double-shot xuyên led",
                        "Khả năng tùy biến", "Hỗ trợ phần mềm QMK/VIA keymap và macro đầy đủ",
                        "Đèn nền", "RGB 22 chế độ hiệu ứng, mạch xuôi South-facing",
                        "Kết nối", "Bluetooth 5.1 (kết nối tối đa 3 thiết bị) & Type-C có dây",
                        "Dung lượng pin", "4000 mAh (dùng đến 300 giờ khi tắt led)",
                        "Hệ điều hành tương thích", "macOS / Windows / Linux / Android"
                )
        );

        Map<String, Map<String, Object>> productSpecsMapPart2 = Map.of(
                "chuot-logitech-mx-master-3s", Map.of(
                        "Độ phân giải cảm biến", "200 đến 8000 DPI (công nghệ Darkfield di chuyển trên mọi bề mặt kể cả kính)",
                        "Nút bấm", "Quiet Click giảm 90% tiếng ồn click chuột",
                        "Cuộn siêu tốc", "Con lăn MagSpeed điện từ cuộn 1000 dòng/giây",
                        "Con lăn ngón cái", "Hỗ trợ cuộn ngang (Horizontal scroll)",
                        "Kết nối", "Bluetooth Low Energy & Logi Bolt USB Receiver",
                        "Phần mềm hỗ trợ", "Logi Options+ (Tùy biến theo từng ứng dụng, Logitech Flow chuyển dữ liệu giữa các máy)",
                        "Thời lượng pin", "Lên đến 70 ngày sau một lần sạc đầy qua USB-C",
                        "Trọng lượng", "141g"
                ),
                "man-hinh-dell-ultrasharp-u2724d-27-inch-2k", Map.of(
                        "Kích thước màn hình", "27 inch 16:9",
                        "Độ phân giải", "2K QHD (2560 x 1440)",
                        "Tấm nền & Tương phản", "IPS Black, Tương phản tĩnh 2000:1",
                        "Tần số quét", "120Hz mượt mà",
                        "Độ bao phủ màu", "100% sRGB, 100% BT.709, 98% Display P3, Delta E < 2",
                        "Công nghệ bảo vệ mắt", "Cảm biến ánh sáng môi trường tự điều chỉnh nhiệt độ màu, ComfortView Plus",
                        "Cổng kết nối", "DisplayPort 1.4, HDMI 2.1, USB-C (Data 10Gbps), USB-A Hub 10Gbps",
                        "Chân đế", "Điều chỉnh độ cao, xoay dọc 90 độ, xoay trái phải, nghiêng"
                ),
                "samsung-galaxy-s24-ultra-256gb", Map.of(
                        "Màn hình", "Dynamic AMOLED 2X 6.8 inch QHD+, 1-120Hz LTPO, Kính Gorilla Armor chống chói",
                        "Chipset", "Qualcomm Snapdragon 8 Gen 3 for Galaxy",
                        "RAM", "12GB LPDDR5X",
                        "Bộ nhớ trong", "256GB UFS 4.0",
                        "Camera chính", "200MP f/1.7 OIS + 50MP Periscope 5x + 10MP Tele 3x + 12MP Ultra-wide",
                        "Pin & Sạc", "5000 mAh, Sạc nhanh có dây 45W, Sạc không dây 15W",
                        "Chất liệu khung", "Khung viền Titanium chuẩn hàng không vũ trụ",
                        "Kháng nước", "IP68"
                )
        );

        Map<String, Map<String, Object>> allProductSpecs = new HashMap<>(productSpecsMapPart1);
        allProductSpecs.putAll(productSpecsMapPart2);

        for (Map.Entry<String, Map<String, Object>> entry : allProductSpecs.entrySet()) {
            productRepository.findAll().stream()
                    .filter(p -> entry.getKey().equalsIgnoreCase(p.getSlug()))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setAttributes(entry.getValue());
                        productRepository.save(p);
                        log.info("Populated standardized specs for product: {}", p.getName());
                    });
        }

        Map<String, Map<String, Object>> variantAttributesPart1 = Map.of(
                "IP15PM-256GB-NAT", Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                "IP15PM-256GB-BLU", Map.of("Màu sắc", "Titan Xanh", "Dung lượng", "256GB"),
                "APPLE-20W-USB-C", Map.of("Màu sắc", "Trắng", "Cổng kết nối", "Type-C"),
                "ANKER-A8832-BLK", Map.of("Màu sắc", "Đen", "Chiều dài", "0.9m"),
                "MBP14-M3P-BLK", Map.of("Màu sắc", "Space Black", "Dung lượng", "512GB", "RAM", "18GB"),
                "MBP14-M3P-SLV", Map.of("Màu sắc", "Silver", "Dung lượng", "512GB", "RAM", "18GB"),
                "APP2-USBC-WHT", Map.of("Màu sắc", "Trắng", "Cổng sạc", "USB-C"),
                "IPADAIR-M2-128-GRY", Map.of("Màu sắc", "Space Gray", "Dung lượng", "128GB", "Kết nối", "Wi-Fi"),
                "IPADAIR-M2-128-STA", Map.of("Màu sắc", "Starlight", "Dung lượng", "128GB", "Kết nối", "Wi-Fi"),
                "ANKER-A2668-BLK", Map.of("Màu sắc", "Đen", "Công suất", "65W", "Số cổng", "2 Type-C + 1 Type-A")
        );

        Map<String, Map<String, Object>> variantAttributesPart2 = Map.of(
                "ANKER-A1289-GRY", Map.of("Màu sắc", "Xám Đen", "Dung lượng", "24000mAh", "Công suất", "140W"),
                "ANKER-A3062-BLK", Map.of("Màu sắc", "Jet Black", "Kiểu dáng", "Over-ear"),
                "ANKER-A3062-BLU", Map.of("Màu sắc", "Sky Blue", "Kiểu dáng", "Over-ear"),
                "KEYCHRON-K2P-RED", Map.of("Loại Switch", "Red Switch (Linear)", "Đèn LED", "RGB Hotswap", "Layout", "75%"),
                "KEYCHRON-K2P-BRN", Map.of("Loại Switch", "Brown Switch (Tactile)", "Đèn LED", "RGB Hotswap", "Layout", "75%"),
                "LOGI-MXM3S-GRY", Map.of("Màu sắc", "Graphite", "Độ nhạy", "8000 DPI", "Kết nối", "Bluetooth + Logi Bolt"),
                "LOGI-MXM3S-WHT", Map.of("Màu sắc", "Pale Gray", "Độ nhạy", "8000 DPI", "Kết nối", "Bluetooth + Logi Bolt"),
                "DELL-U2724D-SLV", Map.of("Màu sắc", "Silver", "Kích thước", "27 inch", "Độ phân giải", "2K QHD 120Hz"),
                "SS-S24U-256-GRY", Map.of("Màu sắc", "Titanium Gray", "Dung lượng", "256GB", "RAM", "12GB"),
                "SS-S24U-256-BLK", Map.of("Màu sắc", "Titanium Black", "Dung lượng", "256GB", "RAM", "12GB")
        );

        Map<String, Map<String, Object>> allVariantAttributes = new HashMap<>(variantAttributesPart1);
        allVariantAttributes.putAll(variantAttributesPart2);

        for (Map.Entry<String, Map<String, Object>> entry : allVariantAttributes.entrySet()) {
            productVariantRepository.findAll().stream()
                    .filter(v -> entry.getKey().equalsIgnoreCase(v.getSku()))
                    .findFirst()
                    .ifPresent(v -> {
                        if (v.getAttributes() == null || v.getAttributes().isEmpty()) {
                            v.setAttributes(entry.getValue());
                            productVariantRepository.save(v);
                            log.info("Backfilled attributes for variant: {}", v.getSku());
                        }
                    });
        }
    }

    private void ensureComprehensiveOrdersAndReviewsExist() {
        if (orderRepository.existsByOrderNumber("ORD-TEST-COMP-001")) {
            log.info("Comprehensive order dataset already exists. Skipping orders init.");
            return;
        }

        log.info("Initializing comprehensive Order and Review dataset across all 11 order statuses...");

        String commonPassword = passwordEncoder.encode("Password123@");
        LocalDateTime now = LocalDateTime.now();

        // Ensure all buyers exist
        User buyer1 = userRepository.findByEmail("buyer@techhub.vn").orElseThrow();

        User buyer2 = userRepository.findByEmail("buyer2@techhub.vn").orElseGet(() -> {
            User u = new User();
            u.setEmail("buyer2@techhub.vn");
            u.setPassword(commonPassword);
            u.setFirstName("Thị Lan");
            u.setLastName("Trần");
            u.setPhone("0902222333");
            u.setAvatarUrl("https://images.unsplash.com/photo-1494790108377-be9c29b29330");
            u.setStatus(UserStatus.ACTIVE);
            u.setEmailVerified(true);
            u.setRoles(new HashSet<>(Set.of(Role.BUYER)));
            return userRepository.save(u);
        });

        User buyer3 = userRepository.findByEmail("buyer3@techhub.vn").orElseGet(() -> {
            User u = new User();
            u.setEmail("buyer3@techhub.vn");
            u.setPassword(commonPassword);
            u.setFirstName("Hoàng Nam");
            u.setLastName("Lê");
            u.setPhone("0903333444");
            u.setAvatarUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d");
            u.setStatus(UserStatus.ACTIVE);
            u.setEmailVerified(true);
            u.setRoles(new HashSet<>(Set.of(Role.BUYER)));
            return userRepository.save(u);
        });

        User buyer4 = userRepository.findByEmail("buyer4@techhub.vn").orElseGet(() -> {
            User u = new User();
            u.setEmail("buyer4@techhub.vn");
            u.setPassword(commonPassword);
            u.setFirstName("Quốc Bảo");
            u.setLastName("Phạm");
            u.setPhone("0904444555");
            u.setAvatarUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e");
            u.setStatus(UserStatus.ACTIVE);
            u.setEmailVerified(true);
            u.setRoles(new HashSet<>(Set.of(Role.BUYER)));
            return userRepository.save(u);
        });

        Shop appleShop = shopRepository.findBySeller_SellerName("apple-authorized-reseller").orElse(null);
        Shop ankerShop = shopRepository.findBySeller_SellerName("anker-official-store").orElse(null);
        Shop shop123 = shopRepository.findBySeller_SellerName("seller123-tech-store").orElse(null);

        if (appleShop == null || ankerShop == null || shop123 == null) {
            log.warn("Shops missing. Cannot populate comprehensive orders.");
            return;
        }

        // Retrieve variants by SKU
        ProductVariant vIphoneNat = findVariantBySku("IP15PM-256GB-NAT");
        ProductVariant vIphoneBlue = findVariantBySku("IP15PM-256GB-BLU");
        ProductVariant vCharger = findVariantBySku("APPLE-20W-USB-C");
        ProductVariant vAnkerCable = findVariantBySku("ANKER-A8832-BLK");
        ProductVariant vMacbook = findVariantBySku("MBP14-M3P-BLK");
        ProductVariant vAirpods = findVariantBySku("APP2-USBC-WHT");
        ProductVariant vIpad = findVariantBySku("IPADAIR-M2-128-GRY");
        ProductVariant vAnker65W = findVariantBySku("ANKER-A2668-BLK");
        ProductVariant vAnker737 = findVariantBySku("ANKER-A1289-GRY");
        ProductVariant vSpaceOne = findVariantBySku("ANKER-A3062-BLK");
        ProductVariant vKeychron = findVariantBySku("KEYCHRON-K2P-RED");
        ProductVariant vLogi = findVariantBySku("LOGI-MXM3S-GRY");
        ProductVariant vDell = findVariantBySku("DELL-U2724D-SLV");
        ProductVariant vS24 = findVariantBySku("SS-S24U-256-GRY");

        // -----------------------------------------------------------------------------------------
        // SECTION A: 8 REVIEW TEST ORDERS (PHASE 5 / BR-20)
        // -----------------------------------------------------------------------------------------
        if (!orderRepository.existsByOrderNumber("ORD-TEST-REV-001") && vIphoneNat != null) {
            Order o1 = createOrder("ORD-TEST-REV-001", buyer2, appleShop, vIphoneNat, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(10), now.minusDays(8), now.minusDays(7), null, "Giao nhanh trong ngày", null);
            OrderItem i1 = orderItemRepository.findByOrderId(o1.getId()).get(0);
            Review r1 = reviewRepository.save(Review.builder().orderItem(i1).user(buyer2).rating(5)
                    .comment("Máy nhận được chuẩn VNA nguyên seal, bọc hộp cẩn thận 3 lớp chống sốc. Màu Titan Tự Nhiên bên ngoài sang và nhẹ hơn hẳn đời trước. Màn hình 120Hz mượt mà, camera zoom 5x chụp nét căng. Rất hài lòng với dịch vụ giao hàng nhanh của shop!")
                    .sellerResponse("Dạ Apple Flagship Store chân thành cảm ơn chị Lan đã tin tưởng lựa chọn mua sắm! Chúc chị có trải nghiệm tuyệt vời với chiếc iPhone 15 Pro Max. Shop luôn sẵn sàng hỗ trợ bảo hành chính hãng 12 tháng tại các trung tâm ủy quyền ạ!")
                    .sellerResponseAt(now.minusDays(6)).build());
            reviewMediaRepository.saveAll(List.of(
                    ReviewMedia.builder().review(r1).mediaUrl("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=800&q=80").mediaType(ReviewMediaType.IMAGE).displayOrder(0).build(),
                    ReviewMedia.builder().review(r1).mediaUrl("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5").mediaType(ReviewMediaType.IMAGE).displayOrder(1).build()
            ));
            reviewHelpfulRepository.saveAll(List.of(
                    ReviewHelpful.builder().review(r1).user(buyer1).build(),
                    ReviewHelpful.builder().review(r1).user(buyer3).build()
            ));
        }

        if (!orderRepository.existsByOrderNumber("ORD-TEST-REV-002") && vIphoneBlue != null) {
            Order o2 = createOrder("ORD-TEST-REV-002", buyer3, appleShop, vIphoneBlue, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(7), now.minusDays(5), now.minusDays(4), null, "Giao hàng giờ hành chính", null);
            OrderItem i2 = orderItemRepository.findByOrderId(o2.getId()).get(0);
            Review r2 = reviewRepository.save(Review.builder().orderItem(i2).user(buyer3).rating(4)
                    .comment("Điện thoại xịn, cấu hình đỉnh cao chơi game rất mượt, pin trâu dùng cả ngày. Giao hàng trong 2 tiếng rất nhanh. Trừ 1 sao do bên vận chuyển làm móp nhẹ 1 góc vỏ hộp bên ngoài, may mà máy bên trong không ảnh hưởng gì.")
                    .sellerResponse(null).sellerResponseAt(null).build());
            reviewMediaRepository.save(ReviewMedia.builder().review(r2).mediaUrl("https://images.unsplash.com/photo-1592750475338-74b7b21085ab").mediaType(ReviewMediaType.IMAGE).displayOrder(0).build());
            reviewHelpfulRepository.save(ReviewHelpful.builder().review(r2).user(buyer4).build());
        }

        if (!orderRepository.existsByOrderNumber("ORD-TEST-REV-003") && vIphoneNat != null) {
            Order o3 = createOrder("ORD-TEST-REV-003", buyer4, appleShop, vIphoneNat, 1,
                    PaymentMethod.COD, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(6), now.minusDays(4), now.minusDays(3), null, "Đồng kiểm khi nhận hàng", null);
            OrderItem i3 = orderItemRepository.findByOrderId(o3.getId()).get(0);
            reviewRepository.save(Review.builder().orderItem(i3).user(buyer4).rating(5)
                    .comment("Hàng chuẩn Apple chính hãng VN/A nguyên tem xuất xưởng. Kích hoạt bảo hành đúng ngày nhận máy. Đóng gói cẩn thận, shipper lịch sự.")
                    .sellerResponse("Apple Flagship Store cảm ơn anh Bảo rất nhiều! Chúc anh luôn có những trải nghiệm công nghệ đỉnh cao cùng sản phẩm ạ!")
                    .sellerResponseAt(now.minusDays(2)).build());
        }

        if (!orderRepository.existsByOrderNumber("ORD-TEST-REV-004") && vCharger != null) {
            Order o4 = createOrder("ORD-TEST-REV-004", buyer1, appleShop, vCharger, 1,
                    PaymentMethod.COD, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(5), now.minusDays(3), now.minusDays(2), null, "Giao tận tay", null);
            OrderItem i4 = orderItemRepository.findByOrderId(o4.getId()).get(0);
            Review r4 = reviewRepository.save(Review.builder().orderItem(i4).user(buyer1).rating(5)
                    .comment("Củ sạc chính hãng cầm đầm tay, chân cắm sáng bóng và chắc chắn. Sạc cho iPhone 15 từ 20% lên 80% chỉ mất tầm 35 phút. Shop đóng gói cẩn thận, giao hỏa tốc rất ưng ý!")
                    .sellerResponse("Cảm ơn bạn Nguyễn Văn A đã tin dùng phụ kiện chính hãng Apple! Đừng ngần ngại nhắn shop nếu cần hỗ trợ kỹ thuật nhé!")
                    .sellerResponseAt(now.minusDays(1)).build());
            reviewMediaRepository.save(ReviewMedia.builder().review(r4).mediaUrl("https://images.unsplash.com/photo-1622445268048-c89b38e07a60").mediaType(ReviewMediaType.IMAGE).displayOrder(0).build());
            reviewHelpfulRepository.saveAll(List.of(
                    ReviewHelpful.builder().review(r4).user(buyer2).build(),
                    ReviewHelpful.builder().review(r4).user(buyer3).build(),
                    ReviewHelpful.builder().review(r4).user(buyer4).build()
            ));
        }

        if (!orderRepository.existsByOrderNumber("ORD-TEST-REV-005") && vCharger != null) {
            Order o5 = createOrder("ORD-TEST-REV-005", buyer2, appleShop, vCharger, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(4), now.minusDays(2), now.minusDays(1), null, null, null);
            OrderItem i5 = orderItemRepository.findByOrderId(o5.getId()).get(0);
            reviewRepository.save(Review.builder().orderItem(i5).user(buyer2).rating(3)
                    .comment("Sạc nhanh chuẩn Power Delivery, nhưng khi sạc củ sạc và lưng máy khá ấm. Dây cắm vào củ sạc hơi chặt. Mức giá cao hơn các hãng phụ kiện bên thứ ba.")
                    .sellerResponse("Dạ shop chào chị Lan ạ. Trong quá trình sạc nhanh ở dải pin 0-50%, củ sạc 20W hoạt động ở công suất tối đa nên sẽ sinh nhiệt ấm nhẹ trong ngưỡng an toàn tiêu chuẩn của Apple. Khi pin đạt trên 80%, máy sẽ tự động kích hoạt tính năng sạc tối ưu và hạ nhiệt độ xuống mát bình thường ạ. Chị an tâm sử dụng nhé!")
                    .sellerResponseAt(now.minusDays(1)).build());
        }

        if (!orderRepository.existsByOrderNumber("ORD-TEST-REV-006") && vAnkerCable != null) {
            Order o6 = createOrder("ORD-TEST-REV-006", buyer3, ankerShop, vAnkerCable, 1,
                    PaymentMethod.COD, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(5), now.minusDays(3), now.minusDays(2), null, null, null);
            OrderItem i6 = orderItemRepository.findByOrderId(o6.getId()).get(0);
            Review r6 = reviewRepository.save(Review.builder().orderItem(i6).user(buyer3).rating(5)
                    .comment("Cáp sạc Anker chất lượng không bao giờ làm thất vọng. Dây bọc dù chắc chắn, chống rối tốt, sạc nhanh ổn định chuẩn MFi không báo lỗi phụ kiện. Giá hợp lý, đáng tiền hơn cáp zin nhiều!")
                    .sellerResponse("Anker Official Store chân thành cảm ơn đánh giá của anh Nam! Chúc anh có những trải nghiệm bền bỉ cùng phụ kiện Anker!")
                    .sellerResponseAt(now.minusDays(1)).build());
            reviewMediaRepository.save(ReviewMedia.builder().review(r6).mediaUrl("https://images.unsplash.com/photo-1583863788434-e58a36330cf0").mediaType(ReviewMediaType.IMAGE).displayOrder(0).build());
        }

        if (!orderRepository.existsByOrderNumber("ORD-TEST-REV-007") && vAnkerCable != null) {
            Order o7 = createOrder("ORD-TEST-REV-007", buyer4, ankerShop, vAnkerCable, 1,
                    PaymentMethod.COD, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(3), now.minusDays(2), now.minusDays(1), null, null, null);
            OrderItem i7 = orderItemRepository.findByOrderId(o7.getId()).get(0);
            reviewRepository.save(Review.builder().orderItem(i7).user(buyer4).rating(2)
                    .comment("Dùng được khoảng 4 ngày thì đầu cáp có hiện tượng lỏng, cắm vào iPhone lúc nhận sạc lúc không. Đề nghị shop liên hệ hỗ trợ đổi mới theo chính sách bảo hành 18 tháng.")
                    .sellerResponse(null).sellerResponseAt(null).build());
        }

        if (!orderRepository.existsByOrderNumber("ORD-TEST-REV-008") && vAnkerCable != null) {
            // Completed order with NO review -> for Buyer 1 to test CREATE REVIEW
            createOrder("ORD-TEST-REV-008", buyer1, ankerShop, vAnkerCable, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(2), now.minusDays(1), now.minusDays(1), null, "Thanh toán trước qua VNPAY", null);
        }

        // -----------------------------------------------------------------------------------------
        // SECTION B: ORDERS ACROSS ALL STATUSES FOR BUYER 1 (buyer@techhub.vn)
        // -----------------------------------------------------------------------------------------
        // 1. COMPLETED: MacBook Pro 14 M3 Pro
        if (vMacbook != null) {
            createOrder("ORD-TEST-COMP-001", buyer1, appleShop, vMacbook, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(15), now.minusDays(13), now.minusDays(12), null, "Giao trong giờ hành chính", null);
        }

        // 2. PENDING_PAYMENT: Chuột Logitech MX Master 3S
        if (vLogi != null) {
            createOrder("ORD-TEST-PEND-001", buyer1, shop123, vLogi, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PENDING, OrderStatus.PENDING_PAYMENT,
                    now.minusHours(2), null, null, null, "Chờ quét mã VNPAY-QR", null);
        }

        // 3. PENDING_CONFIRMATION: Củ sạc Anker 65W
        if (vAnker65W != null) {
            createOrder("ORD-TEST-CONF-001", buyer1, ankerShop, vAnker65W, 1,
                    PaymentMethod.COD, PaymentStatus.PENDING, OrderStatus.PENDING_CONFIRMATION,
                    now.minusHours(5), null, null, null, "Đơn mới đặt, shop gọi xác nhận trước khi giao", null);
        }

        // 4. CONFIRMED: Bàn phím cơ Keychron K2 Pro
        if (vKeychron != null) {
            createOrder("ORD-TEST-CONF-002", buyer1, shop123, vKeychron, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.CONFIRMED,
                    now.minusHours(12), null, null, null, "Shop đã xác nhận đơn hàng thành công", null);
        }

        // 5. PREPARING: Tai nghe Apple AirPods Pro 2
        if (vAirpods != null) {
            createOrder("ORD-TEST-PREP-001", buyer1, appleShop, vAirpods, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.PREPARING,
                    now.minusDays(1), null, null, null, "Đang đóng gói và bọc túi khí chống sốc", null);
        }

        // 6. WAITING_FOR_PICKUP: Cáp sạc Anker
        if (vAnkerCable != null) {
            createOrder("ORD-TEST-PICK-001", buyer1, ankerShop, vAnkerCable, 2,
                    PaymentMethod.COD, PaymentStatus.PENDING, OrderStatus.WAITING_FOR_PICKUP,
                    now.minusDays(1).minusHours(4), null, null, null, "Đã in phiếu vận đơn, chờ shipper tới lấy", null);
        }

        // 7. SHIPPING: Pin dự phòng Anker 737
        if (vAnker737 != null) {
            createOrder("ORD-TEST-SHIP-001", buyer1, ankerShop, vAnker737, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.SHIPPING,
                    now.minusDays(2), null, null, null, "Đang trung chuyển qua bưu cục Cầu Giấy", null);
        }

        // 8. DELIVERED: Màn hình Dell UltraSharp U2724D
        if (vDell != null) {
            createOrder("ORD-TEST-DELV-001", buyer1, shop123, vDell, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.DELIVERED,
                    now.minusDays(3), now.minusHours(6), null, null, "Shipper đã giao thành công cho lễ tân", null);
        }

        // 9. CANCELLED: Samsung Galaxy S24 Ultra
        if (vS24 != null) {
            createOrder("ORD-TEST-CANC-001", buyer1, shop123, vS24, 1,
                    PaymentMethod.COD, PaymentStatus.CANCELLED, OrderStatus.CANCELLED,
                    now.minusDays(4), null, null, now.minusDays(3), "Khách hàng đổi ý muốn đổi sang màu khác", null);
        }

        // 10. RETURN_REQUESTED: Tai nghe Soundcore Space One
        if (vSpaceOne != null) {
            createOrder("ORD-TEST-RETN-001", buyer1, ankerShop, vSpaceOne, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.RETURN_REQUESTED,
                    now.minusDays(5), now.minusDays(2), null, null, "Khách yêu cầu đổi trả do đeo không vừa tai", null);
        }

        // 11. REFUNDED: Củ Sạc Nhanh Apple 20W
        if (vCharger != null) {
            createOrder("ORD-TEST-REFD-001", buyer1, appleShop, vCharger, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.REFUNDED,
                    now.minusDays(8), now.minusDays(6), null, null, "Đã hoàn tiền 100% qua cổng VNPAY", BigDecimal.valueOf(550000));
        }

        // -----------------------------------------------------------------------------------------
        // SECTION C: ORDERS FOR OTHER BUYERS (BUYER 2, 3, 4) TO POPULATE SELLER DASHBOARDS
        // -----------------------------------------------------------------------------------------
        if (vS24 != null) {
            createOrder("ORD-TEST-B2-001", buyer2, shop123, vS24, 1,
                    PaymentMethod.COD, PaymentStatus.PENDING, OrderStatus.CONFIRMED,
                    now.minusHours(8), null, null, null, "Đơn COD của chị Lan", null);
        }
        if (vIpad != null) {
            createOrder("ORD-TEST-B2-002", buyer2, appleShop, vIpad, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.SHIPPING,
                    now.minusDays(1), null, null, null, "Giao iPad tới chung cư", null);
        }
        if (vLogi != null) {
            createOrder("ORD-TEST-B2-003", buyer2, shop123, vLogi, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(6), now.minusDays(4), now.minusDays(3), null, "Đã hoàn tất", null);
        }

        if (vDell != null) {
            createOrder("ORD-TEST-B3-001", buyer3, shop123, vDell, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.PREPARING,
                    now.minusHours(14), null, null, null, "Đang đóng gói màn hình cẩn thận", null);
        }
        if (vMacbook != null) {
            createOrder("ORD-TEST-B3-002", buyer3, appleShop, vMacbook, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.COMPLETED,
                    now.minusDays(14), now.minusDays(12), now.minusDays(11), null, "Giao hàng công ty", null);
        }
        if (vKeychron != null) {
            createOrder("ORD-TEST-B3-003", buyer3, shop123, vKeychron, 1,
                    PaymentMethod.COD, PaymentStatus.PENDING, OrderStatus.PENDING_CONFIRMATION,
                    now.minusHours(3), null, null, null, "Gọi trước khi giao 15p", null);
        }

        if (vAirpods != null) {
            createOrder("ORD-TEST-B4-001", buyer4, appleShop, vAirpods, 1,
                    PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.CONFIRMED,
                    now.minusHours(10), null, null, null, "Đã thanh toán thẻ", null);
        }
        if (vAnker737 != null) {
            createOrder("ORD-TEST-B4-002", buyer4, ankerShop, vAnker737, 1,
                    PaymentMethod.COD, PaymentStatus.PENDING, OrderStatus.SHIPPING,
                    now.minusDays(1), null, null, null, "Đang vận chuyển", null);
        }

        log.info("Comprehensive order dataset initialized successfully with 27 orders covering all 11 OrderStatus enums!");
    }

    private Order createOrder(
            String orderNumber,
            User buyer,
            Shop shop,
            ProductVariant variant,
            int quantity,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            OrderStatus orderStatus,
            LocalDateTime orderTime,
            LocalDateTime deliveredAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt,
            String note,
            BigDecimal refundedAmount
    ) {
        if (orderRepository.existsByOrderNumber(orderNumber)) {
            return orderRepository.findByOrderNumber(orderNumber).orElse(null);
        }

        BigDecimal productPrice = variant.getPrice();
        BigDecimal totalProductAmount = productPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal shippingFee = BigDecimal.valueOf(30000);
        BigDecimal totalAmount = totalProductAmount.add(shippingFee);
        BigDecimal commissionRate = BigDecimal.valueOf(5.00);
        BigDecimal commissionAmount = totalProductAmount.multiply(BigDecimal.valueOf(0.05));
        BigDecimal sellerEarnings = totalAmount.subtract(commissionAmount);

        Payment payment = Payment.builder()
                .user(buyer)
                .paymentMethod(paymentMethod)
                .transactionId("TXN-" + orderNumber)
                .bankCode(paymentMethod == PaymentMethod.VNPAY ? "NCB" : null)
                .status(paymentStatus)
                .amount(totalAmount)
                .refundedAmount(refundedAmount != null ? refundedAmount : BigDecimal.ZERO)
                .createdAt(orderTime)
                .build();
        paymentRepository.save(payment);

        String addressSnapshot = String.format("%s %s, %s, Số 144 Xuân Thủy, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Thành phố Hà Nội",
                buyer.getLastName(), buyer.getFirstName(), buyer.getPhone() != null ? buyer.getPhone() : "0901234567");

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(buyer)
                .shop(shop)
                .payment(payment)
                .status(orderStatus)
                .deliveryAddressSnapshot(addressSnapshot)
                .totalProductAmount(totalProductAmount)
                .shippingFee(shippingFee)
                .shopDiscountAmount(BigDecimal.ZERO)
                .platformDiscountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .totalCommission(commissionAmount)
                .totalSellerEarnings(sellerEarnings)
                .note(note)
                .deliveredAt(deliveredAt)
                .completedAt(completedAt)
                .cancelledAt(cancelledAt)
                .createdAt(orderTime)
                .build();
        orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .productVariant(variant)
                .productNameSnapshot(variant.getProduct().getName())
                .variantNameSnapshot(variant.getVariantName())
                .skuSnapshot(variant.getSku())
                .imageUrlSnapshot(variant.getImageUrl())
                .originalPrice(productPrice)
                .quantity(quantity)
                .itemDiscountAmount(BigDecimal.ZERO)
                .discountedUnitPrice(productPrice)
                .finalSubtotal(totalProductAmount)
                .commissionRateSnapshot(commissionRate)
                .commissionAmount(commissionAmount)
                .sellerEarnings(sellerEarnings)
                .createdAt(orderTime)
                .build();
        orderItemRepository.save(orderItem);
        return order;
    }

    private void printSummaryLog() {
        User buyer = userRepository.findByEmail("buyer@techhub.vn").orElse(null);
        if (buyer == null) return;

        Address address = addressRepository.findDefaultAddressByUserId(buyer.getId()).orElse(null);
        Cart cart = cartRepository.findByUserId(buyer.getId()).orElse(null);
        List<CartItem> cartItems = cart != null ? cartItemRepository.findByCartIdWithDetails(cart.getId()) : Collections.emptyList();

        Shop appleShop = shopRepository.findBySeller_SellerName("apple-authorized-reseller").orElse(null);
        Shop ankerShop = shopRepository.findBySeller_SellerName("anker-official-store").orElse(null);
        Shop shop123 = shopRepository.findBySeller_SellerName("seller123-tech-store").orElse(null);

        Voucher platOrderVoucher = voucherRepository.findByCode("TECHHUB50K").orElse(null);
        Voucher platShipVoucher = voucherRepository.findByCode("FREESHIP30K").orElse(null);
        Voucher appleVoucher = voucherRepository.findByCode("APPLE20K").orElse(null);
        Voucher ankerVoucher = voucherRepository.findByCode("ANKER15K").orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================================================================\n");
        sb.append(" [TECHHUB SEED DATA READY FOR COMPREHENSIVE E-COMMERCE TESTING]\n");
        sb.append("========================================================================================\n");
        sb.append("1. USER ACCOUNTS (Mật khẩu chung: Password123@ - Riêng seller123: 123123):\n");
        sb.append("   - BUYER:    buyer@techhub.vn         | ID: ").append(buyer.getId()).append(" | Roles: [BUYER]\n");
        sb.append("   - BUYER 2:  buyer2@techhub.vn        | Tên: Trần Thị Lan       | Roles: [BUYER]\n");
        sb.append("   - BUYER 3:  buyer3@techhub.vn        | Tên: Lê Hoàng Nam       | Roles: [BUYER]\n");
        sb.append("   - BUYER 4:  buyer4@techhub.vn        | Tên: Phạm Quốc Bảo      | Roles: [BUYER]\n");
        sb.append("   - SELLER 1: seller.apple@techhub.vn  | Roles: [BUYER, SELLER]  | Shop: Apple Flagship Store (ID: ")
                .append(appleShop != null ? appleShop.getId() : "N/A").append(")\n");
        sb.append("   - SELLER 2: seller.anker@techhub.vn  | Roles: [BUYER, SELLER]  | Shop: Anker Official Store (ID: ")
                .append(ankerShop != null ? ankerShop.getId() : "N/A").append(")\n");
        sb.append("   - SELLER 3: seller123@techhub.vn     | Roles: [BUYER, SELLER]  | Pass: 123123 | Shop: TechHub Gear Store (ID: ")
                .append(shop123 != null ? shop123.getId() : "N/A").append(")\n");
        sb.append("   - ADMIN:    admin@techhub.vn         | Roles: [ADMIN]\n\n");

        sb.append("2. SHOPS & EXTENDED PRODUCT CATALOG (13 Hot Tech Products):\n");
        sb.append("   - Shop 1 (Apple Flagship Store):\n");
        sb.append("     + iPhone 15 Pro Max 256GB VN/A (Titan Tự Nhiên, Titan Xanh)\n");
        sb.append("     + Củ Sạc Nhanh Apple 20W Type-C Chính Hãng\n");
        sb.append("     + MacBook Pro 14 M3 Pro 18GB/512GB (Space Black, Silver)\n");
        sb.append("     + Tai Nghe Apple AirPods Pro 2 USB-C MagSafe\n");
        sb.append("     + iPad Air M2 11 inch Wi-Fi 128GB (Space Gray, Starlight)\n");
        sb.append("   - Shop 2 (Anker Official Store):\n");
        sb.append("     + Cáp Sạc Anker PowerLine III USB-C to Lightning 0.9m\n");
        sb.append("     + Củ Sạc Nhanh Anker 735 GaNPrime 65W 3 Cổng\n");
        sb.append("     + Trạm Pin Dự Phòng Anker 737 PowerCore 24,000mAh 140W\n");
        sb.append("     + Tai Nghe Chống Ồn Anker Soundcore Space One (Jet Black, Sky Blue)\n");
        sb.append("   - Shop 3 (TechHub Gear Store - seller123):\n");
        sb.append("     + Bàn Phím Cơ Keychron K2 Pro QMK/VIA (Red Switch, Brown Switch)\n");
        sb.append("     + Chuột Không Dây Logitech MX Master 3S 8000 DPI (Graphite, Pale Gray)\n");
        sb.append("     + Màn Hình Dell UltraSharp U2724D 27 inch 2K 120Hz IPS Black\n");
        sb.append("     + Samsung Galaxy S24 Ultra 12GB/256GB Khung Viền Titan (Gray, Black)\n\n");

        sb.append("3. COMPREHENSIVE ORDER DATASET (27 Đơn hàng phủ đầy đủ 11 trạng thái OrderStatus):\n");
        sb.append("   - PENDING_PAYMENT:     ORD-TEST-PEND-001 (Logitech MX Master 3S - Chờ thanh toán VNPAY)\n");
        sb.append("   - PENDING_CONFIRMATION: ORD-TEST-CONF-001 (Anker GaN 65W - Chờ shop duyệt đơn COD)\n");
        sb.append("   - CONFIRMED:            ORD-TEST-CONF-002 (Keychron K2 Pro - Shop đã xác nhận đơn)\n");
        sb.append("   - PREPARING:            ORD-TEST-PREP-001 (AirPods Pro 2 - Shop đang chuẩn bị hàng)\n");
        sb.append("   - WAITING_FOR_PICKUP:   ORD-TEST-PICK-001 (Cáp sạc Anker - Đang chờ shipper đến lấy)\n");
        sb.append("   - SHIPPING:             ORD-TEST-SHIP-001 (Anker 737 140W - Shipper đang giao hàng)\n");
        sb.append("   - DELIVERED:            ORD-TEST-DELV-001 (Dell UltraSharp - Đã giao hàng thành công)\n");
        sb.append("   - COMPLETED:            ORD-TEST-COMP-001 (MacBook Pro 14 M3 Pro - Hoàn tất)\n");
        sb.append("   - CANCELLED:            ORD-TEST-CANC-001 (Galaxy S24 Ultra - Đơn đã hủy)\n");
        sb.append("   - RETURN_REQUESTED:     ORD-TEST-RETN-001 (Soundcore Space One - Khách yêu cầu đổi trả)\n");
        sb.append("   - REFUNDED:             ORD-TEST-REFD-001 (Củ sạc Apple 20W - Đã hoàn tiền qua VNPAY)\n");
        sb.append("   + Các đơn hoàn tất & đánh giá: ORD-TEST-REV-001 -> ORD-TEST-REV-008\n\n");

        sb.append("4. REVIEW TESTING KEY ENDPOINTS & IDS:\n");
        Order order8 = orderRepository.findByOrderNumber("ORD-TEST-REV-008").orElse(null);
        if (order8 != null) {
            List<OrderItem> items8 = orderItemRepository.findByOrderId(order8.getId());
            if (!items8.isEmpty()) {
                sb.append("   - OrderItem ID chờ đánh giá mới (POST /api/buyer/reviews): ").append(items8.get(0).getId()).append("\n");
            }
        }
        Order order4 = orderRepository.findByOrderNumber("ORD-TEST-REV-004").orElse(null);
        if (order4 != null) {
            List<OrderItem> items4 = orderItemRepository.findByOrderId(order4.getId());
            if (!items4.isEmpty()) {
                Review rev4 = reviewRepository.findByOrderItemId(items4.get(0).getId()).orElse(null);
                if (rev4 != null) {
                    sb.append("   - Review ID của buyer@techhub.vn để sửa (PUT /api/buyer/reviews/{id}): ").append(rev4.getId()).append("\n");
                }
            }
        }
        Order order2 = orderRepository.findByOrderNumber("ORD-TEST-REV-002").orElse(null);
        if (order2 != null) {
            List<OrderItem> items2 = orderItemRepository.findByOrderId(order2.getId());
            if (!items2.isEmpty()) {
                Review rev2 = reviewRepository.findByOrderItemId(items2.get(0).getId()).orElse(null);
                if (rev2 != null) {
                    sb.append("   - Review ID cần Seller Apple phản hồi (POST /api/seller/reviews/{id}/response): ").append(rev2.getId()).append("\n");
                }
            }
        }
        sb.append("========================================================================================\n");
        log.info(sb.toString());
    }
}
