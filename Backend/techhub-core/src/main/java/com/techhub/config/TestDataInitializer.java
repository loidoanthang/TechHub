package com.techhub.config;

import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@Order(1)
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

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking initial test dataset for Postman testing...");

        if (userRepository.existsByEmail("buyer@techhub.vn")) {
            log.info("Test dataset already exists in database. Printing Postman testing details...");
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
        seller1User.setRoles(new HashSet<>(Set.of(Role.SELLER)));
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
        seller2User.setRoles(new HashSet<>(Set.of(Role.SELLER)));
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
                .name("Điện thoại & Phụ kiện")
                .description("Điện thoại thông minh, củ sạc, cáp sạc, phụ kiện cao cấp")
                .status(CategoryStatus.ACTIVE)
                .isDeleted(false)
                .build();
        categoryRepository.save(categoryPhone);

        CategoryAttribute attrColor = CategoryAttribute.builder()
                .category(categoryPhone)
                .name("Màu sắc")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("Titan Tự Nhiên", "Titan Xanh", "Trắng", "Đen"))
                .build();
        categoryAttributeRepository.save(attrColor);

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
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
        productRepository.save(productIphone);

        ProductImage imgIphone = ProductImage.builder()
                .product(productIphone)
                .imageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569")
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
                .imageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569")
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
                .imageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569")
                .isDeleted(false)
                .build();
        productVariantRepository.save(variantIphoneBlue);

        // Product 2: Củ sạc Apple 20W (Shop 1 - Apple)
        Product productCharger = Product.builder()
                .shop(shop1)
                .category(categoryPhone)
                .brandName("Apple")
                .name("Củ Sạc Nhanh Apple 20W Type-C Chính Hãng")
                .slug("cu-sac-nhanh-apple-20w-type-c")
                .description("Củ sạc nhanh Apple 20W cổng USB-C tương thích iPhone, iPad.")
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
                .isDeleted(false)
                .build();
        productVariantRepository.save(variantCharger);

        // Product 3: Cáp sạc Anker PowerLine (Shop 2 - Anker)
        Product productAnkerCable = Product.builder()
                .shop(shop2)
                .category(categoryPhone)
                .brandName("Anker")
                .name("Cáp Sạc Anker PowerLine III USB-C to Lightning 0.9m")
                .slug("cap-sac-anker-powerline-iii-usb-c-to-lightning-0-9m")
                .description("Cáp sạc siêu bền Anker chuẩn MFi chứng nhận bởi Apple.")
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

        log.info("Comprehensive test dataset created successfully!");
        printSummaryLog();
    }

    private void printSummaryLog() {
        User buyer = userRepository.findByEmail("buyer@techhub.vn").orElse(null);
        if (buyer == null) return;

        Address address = addressRepository.findDefaultAddressByUserId(buyer.getId()).orElse(null);
        Cart cart = cartRepository.findByUserId(buyer.getId()).orElse(null);
        List<CartItem> cartItems = cart != null ? cartItemRepository.findByCartIdWithDetails(cart.getId()) : Collections.emptyList();

        Shop appleShop = shopRepository.findBySeller_SellerName("apple-authorized-reseller").orElse(null);
        Shop ankerShop = shopRepository.findBySeller_SellerName("anker-official-store").orElse(null);

        Voucher platOrderVoucher = voucherRepository.findByCode("TECHHUB50K").orElse(null);
        Voucher platShipVoucher = voucherRepository.findByCode("FREESHIP30K").orElse(null);
        Voucher appleVoucher = voucherRepository.findByCode("APPLE20K").orElse(null);
        Voucher ankerVoucher = voucherRepository.findByCode("ANKER15K").orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================================================================\n");
        sb.append(" [TECHHUB SEED DATA READY FOR POSTMAN TESTING]\n");
        sb.append("========================================================================================\n");
        sb.append("1. USER ACCOUNTS (Mật khẩu chung: Password123@):\n");
        sb.append("   - BUYER:  buyer@techhub.vn         | ID: ").append(buyer.getId()).append("\n");
        sb.append("   - SELLER 1: seller.apple@techhub.vn  | Shop: Apple Flagship Store (ID: ")
                .append(appleShop != null ? appleShop.getId() : "N/A").append(")\n");
        sb.append("   - SELLER 2: seller.anker@techhub.vn  | Shop: Anker Official Store (ID: ")
                .append(ankerShop != null ? ankerShop.getId() : "N/A").append(")\n");
        sb.append("   - ADMIN:  admin@techhub.vn\n\n");

        sb.append("2. BUYER DEFAULT DELIVERY ADDRESS:\n");
        sb.append("   - Address ID: ").append(address != null ? address.getId() : "N/A")
                .append(" (Số 144 Xuân Thủy, Cầu Giấy, Hà Nội)\n\n");

        sb.append("3. CART ITEMS PRE-POPULATED IN BUYER'S CART:\n");
        for (CartItem ci : cartItems) {
            sb.append("   - CartItem ID: ").append(ci.getId())
                    .append(" | ").append(ci.getProduct().getName())
                    .append(" (").append(ci.getProductVariant().getVariantName()).append(")")
                    .append(" | Qty: ").append(ci.getQuantity())
                    .append(" | Đơn giá: ").append(ci.getProductVariant().getPrice()).append("đ")
                    .append(" | Shop: ").append(ci.getProduct().getShop().getName())
                    .append(" (Shop ID: ").append(ci.getProduct().getShop().getId()).append(")\n");
        }
        sb.append("\n4. ACTIVE VOUCHERS:\n");
        sb.append("   - Platform Order Voucher:    ").append(platOrderVoucher != null ? platOrderVoucher.getId() : "N/A")
                .append(" (Code: TECHHUB50K - Giảm 50K cho đơn từ 500K)\n");
        sb.append("   - Platform Shipping Voucher: ").append(platShipVoucher != null ? platShipVoucher.getId() : "N/A")
                .append(" (Code: FREESHIP30K - Giảm 30K ship cho đơn từ 200K)\n");
        sb.append("   - Shop Apple Voucher:        ").append(appleVoucher != null ? appleVoucher.getId() : "N/A")
                .append(" (Code: APPLE20K - Giảm 20K cho đơn từ 500K)\n");
        sb.append("   - Shop Anker Voucher:        ").append(ankerVoucher != null ? ankerVoucher.getId() : "N/A")
                .append(" (Code: ANKER15K - Giảm 15K cho đơn từ 200K)\n\n");

        sb.append("5. HƯỚNG DẪN TEST POSTMAN (CHECKOUT FLOW):\n");
        sb.append("   Bước 1: Login lấy JWT Access Token:\n");
        sb.append("           POST /api/auth/login\n");
        sb.append("           Body: { \"email\": \"buyer@techhub.vn\", \"password\": \"Password123@\" }\n\n");

        if (!cartItems.isEmpty() && address != null && appleShop != null) {
            CartItem chargerItem = cartItems.stream()
                    .filter(ci -> ci.getProduct().getSlug().contains("cu-sac"))
                    .findFirst().orElse(cartItems.get(0));

            sb.append("   Bước 2: Xem trước thanh toán (Checkout Preview - Đơn 1 Shop Apple):\n");
            sb.append("           POST /api/checkout/preview\n");
            sb.append("           Header: Authorization: Bearer <access_token>\n");
            sb.append("           Body:\n");
            sb.append("           {\n");
            sb.append("             \"selectedCartItemIds\": [\"").append(chargerItem.getId()).append("\"],\n");
            sb.append("             \"addressId\": \"").append(address.getId()).append("\",\n");
            sb.append("             \"shopVoucherIds\": [\"").append(appleVoucher != null ? appleVoucher.getId() : "").append("\"],\n");
            sb.append("             \"platformVoucherId\": \"").append(platOrderVoucher != null ? platOrderVoucher.getId() : "").append("\",\n");
            sb.append("             \"platformShippingVoucherId\": \"").append(platShipVoucher != null ? platShipVoucher.getId() : "").append("\"\n");
            sb.append("           }\n\n");

            sb.append("   Bước 3: Đặt hàng thanh toán COD (Place Order COD):\n");
            sb.append("           POST /api/checkout\n");
            sb.append("           Header: Authorization: Bearer <access_token>\n");
            sb.append("           Body:\n");
            sb.append("           {\n");
            sb.append("             \"addressId\": \"").append(address.getId()).append("\",\n");
            sb.append("             \"paymentMethod\": \"COD\",\n");
            sb.append("             \"selectedCartItemIds\": [\"").append(chargerItem.getId()).append("\"],\n");
            sb.append("             \"platformVoucherId\": \"").append(platOrderVoucher != null ? platOrderVoucher.getId() : "").append("\",\n");
            sb.append("             \"platformShippingVoucherId\": \"").append(platShipVoucher != null ? platShipVoucher.getId() : "").append("\",\n");
            sb.append("             \"shopOrders\": [\n");
            sb.append("               {\n");
            sb.append("                 \"shopId\": \"").append(appleShop.getId()).append("\",\n");
            sb.append("                 \"shopVoucherIds\": [\"").append(appleVoucher != null ? appleVoucher.getId() : "").append("\"],\n");
            sb.append("                 \"note\": \"Giao hàng trong giờ hành chính\"\n");
            sb.append("               }\n");
            sb.append("             ]\n");
            sb.append("           }\n\n");

            sb.append("   Bước 4: Đặt hàng thanh toán VNPAY Online (Nhận URL thanh toán sandbox):\n");
            sb.append("           POST /api/checkout\n");
            sb.append("           Header: Authorization: Bearer <access_token>\n");
            sb.append("           Body:\n");
            sb.append("           {\n");
            sb.append("             \"addressId\": \"").append(address.getId()).append("\",\n");
            sb.append("             \"paymentMethod\": \"VNPAY\",\n");
            sb.append("             \"bankCode\": \"NCB\",\n");
            sb.append("             \"selectedCartItemIds\": [\"").append(chargerItem.getId()).append("\"],\n");
            sb.append("             \"shopOrders\": [\n");
            sb.append("               {\n");
            sb.append("                 \"shopId\": \"").append(appleShop.getId()).append("\",\n");
            sb.append("                 \"shopVoucherIds\": [],\n");
            sb.append("                 \"note\": \"Thanh toán online qua VNPAY NCB\"\n");
            sb.append("               }\n");
            sb.append("             ]\n");
            sb.append("           }\n");
        }
        sb.append("========================================================================================\n");
        log.info(sb.toString());
    }
}
