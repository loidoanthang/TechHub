package com.techhub.service.seller;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateProductImageRequest;
import com.techhub.model.dto.request.CreateProductRequest;
import com.techhub.model.dto.request.CreateProductVariantRequest;
import com.techhub.model.dto.response.ProductDetailResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.seller.impl.SellerProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerProductServiceImpl (POST /api/seller/products)")
class SellerProductServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryAttributeRepository categoryAttributeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private SellerProductServiceImpl sellerProductService;

    private UUID userId;
    private UUID sellerId;
    private UUID shopId;
    private UUID categoryId;
    private Seller seller;
    private Shop shop;
    private Category category;
    private List<CategoryAttribute> categoryAttributes;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        seller = Seller.builder()
                .id(sellerId)
                .sellerName("TechSeller")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("123456789")
                .bankAccountHolder("NGUYEN VAN A")
                .build();

        shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name("TechHub Official Store")
                .status(ShopStatus.ACTIVE)
                .build();

        category = Category.builder()
                .id(categoryId)
                .name("Bàn phím & Chuột")
                .status(CategoryStatus.ACTIVE)
                .build();

        CategoryAttribute attr1 = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Kết nối")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("Bluetooth", "Wireless 2.4G", "Có dây"))
                .build();

        CategoryAttribute attr2 = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Trọng lượng")
                .dataType(CategoryAttributeDataType.NUMBER)
                .build();

        CategoryAttribute attr3 = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Đèn LED")
                .dataType(CategoryAttributeDataType.BOOLEAN)
                .build();

        CategoryAttribute attr4 = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Layout")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();

        categoryAttributes = List.of(attr1, attr2, attr3, attr4);
    }

    private Map<String, Object> createValidAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("Kết nối", "Bluetooth");
        attrs.put("Trọng lượng", "231");
        attrs.put("Đèn LED", "true");
        attrs.put("Layout", "Tenkeyless 75%");
        return attrs;
    }

    @Test
    @DisplayName("TC-01: createProduct_Success_SingleVariant - Tạo sản phẩm đơn lẻ thành công")
    void createProduct_Success_SingleVariant() {
        // Arrange
        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductVariantRequest variant = new CreateProductVariantRequest(
                "APL-KB-01", "Tiêu chuẩn", "Mô tả biến thể",
                new BigDecimal("2500000.00"), 100, "https://cdn.techhub.vn/img1.jpg", null
        );
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Bàn phím Apple Magic Keyboard", "Apple", "Mô tả sản phẩm",
                createValidAttributes(), List.of(img), List.of(variant)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productRepository.existsBySlug(any())).thenReturn(false);

        UUID productId = UUID.randomUUID();
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(productId);
            return p;
        });

        when(productImageRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<ProductImage> list = inv.getArgument(0);
            list.forEach(i -> i.setId(UUID.randomUUID()));
            return list;
        });

        when(productVariantRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<ProductVariant> list = inv.getArgument(0);
            list.forEach(v -> v.setId(UUID.randomUUID()));
            return list;
        });

        // Act
        ProductDetailResponse response = sellerProductService.createProduct(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(productId, response.id());
        assertEquals(shopId, response.shopId());
        assertEquals(categoryId, response.categoryId());
        assertEquals("Bàn phím & Chuột", response.categoryName());
        assertEquals("Apple", response.brandName());
        assertEquals("Bàn phím Apple Magic Keyboard", response.name());
        assertEquals(ProductApprovalStatus.DRAFT, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        assertFalse(response.isSuspended());
        assertEquals(1, response.images().size());
        assertEquals(0, response.images().get(0).displayOrder());
        assertEquals(1, response.variants().size());
        assertEquals("APL-KB-01", response.variants().get(0).sku());
        assertEquals(new BigDecimal("2500000.00"), response.variants().get(0).price());
        assertEquals(100, response.variants().get(0).stockQuantity());
        assertEquals(ProductVariantStatus.ACTIVE, response.variants().get(0).status());

        verify(productRepository).save(any(Product.class));
        verify(productImageRepository).saveAll(anyList());
        verify(productVariantRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-02: createProduct_Success_MultiVariants - Tạo sản phẩm nhiều biến thể thành công")
    void createProduct_Success_MultiVariants() {
        // Arrange
        CreateProductImageRequest img1 = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductImageRequest img2 = new CreateProductImageRequest("https://cdn.techhub.vn/img2.jpg", 1);
        CreateProductImageRequest img3 = new CreateProductImageRequest("https://cdn.techhub.vn/img3.jpg", 2);

        Map<String, Object> var1Attrs = Map.of("Màu sắc", "Trắng Bạc");
        Map<String, Object> var2Attrs = Map.of("Màu sắc", "Xám Không Gian");

        CreateProductVariantRequest variant1 = new CreateProductVariantRequest(
                "APL-KB-WHT", "Trắng Bạc", "Bản màu trắng",
                new BigDecimal("2500000.00"), 50, "https://cdn.techhub.vn/img1.jpg", var1Attrs
        );
        CreateProductVariantRequest variant2 = new CreateProductVariantRequest(
                "APL-KB-GRY", "Xám Không Gian", "Bản màu xám",
                new BigDecimal("2700000.00"), 30, "https://cdn.techhub.vn/img2.jpg", var2Attrs
        );

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Bàn phím Apple Magic Keyboard Multi", "Apple", "Mô tả",
                createValidAttributes(), List.of(img1, img2, img3), List.of(variant1, variant2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productRepository.existsBySlug(any())).thenReturn(false);

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        when(productImageRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<ProductImage> list = inv.getArgument(0);
            list.forEach(i -> i.setId(UUID.randomUUID()));
            return list;
        });

        when(productVariantRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<ProductVariant> list = inv.getArgument(0);
            list.forEach(v -> v.setId(UUID.randomUUID()));
            return list;
        });

        // Act
        ProductDetailResponse response = sellerProductService.createProduct(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.images().size());
        assertEquals(2, response.variants().size());
        assertEquals("APL-KB-WHT", response.variants().get(0).sku());
        assertEquals("APL-KB-GRY", response.variants().get(1).sku());
    }

    @Test
    @DisplayName("TC-03: createProduct_Success_WhenShopPaused - Cho phép tạo DRAFT khi Shop PAUSED (BR-07)")
    void createProduct_Success_WhenShopPaused() {
        // Arrange
        shop.setStatus(ShopStatus.PAUSED);

        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductVariantRequest variant = new CreateProductVariantRequest(
                "APL-KB-01", "Tiêu chuẩn", "Mô tả",
                new BigDecimal("2500000.00"), 100, null, null
        );
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Bàn phím khi Shop tạm nghỉ", "Apple", "Mô tả",
                createValidAttributes(), List.of(img), List.of(variant)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productRepository.existsBySlug(any())).thenReturn(false);

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(productImageRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        ProductDetailResponse response = sellerProductService.createProduct(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ProductApprovalStatus.DRAFT, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
    }

    @Test
    @DisplayName("TC-04: createProduct_Throws_SellerNotFound - Không tìm thấy hồ sơ Seller")
    void createProduct_Throws_SellerNotFound() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-05: createProduct_Throws_ShopNotFound - Seller chưa có Shop")
    void createProduct_Throws_ShopNotFound() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-06: createProduct_Throws_ShopNotActive_Suspended - Shop bị kỷ luật SUSPENDED")
    void createProduct_Throws_ShopNotActive_Suspended() {
        // Arrange
        shop.setStatus(ShopStatus.SUSPENDED);
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-07: createProduct_Throws_ShopNotActive_Banned - Shop bị cấm vĩnh viễn BANNED")
    void createProduct_Throws_ShopNotActive_Banned() {
        // Arrange
        shop.setStatus(ShopStatus.BANNED);
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-08: createProduct_Throws_CategoryNotFound - Danh mục không tồn tại")
    void createProduct_Throws_CategoryNotFound() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: createProduct_Throws_CategoryInactive - Danh mục đang INACTIVE (BR-11A)")
    void createProduct_Throws_CategoryInactive() {
        // Arrange
        category.setStatus(CategoryStatus.INACTIVE);
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: createProduct_Throws_MissingCategoryAttribute - Thiếu thuộc tính bắt buộc (Strict Schema BR-09)")
    void createProduct_Throws_MissingCategoryAttribute() {
        // Arrange
        Map<String, Object> attrs = createValidAttributes();
        attrs.remove("Kết nối"); // Thiếu 1 thuộc tính bắt buộc

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                attrs, Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.MISSING_CATEGORY_ATTRIBUTE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: createProduct_Throws_InvalidAttributeValue_NotNumber - Sai định dạng NUMBER")
    void createProduct_Throws_InvalidAttributeValue_NotNumber() {
        // Arrange
        Map<String, Object> attrs = createValidAttributes();
        attrs.put("Trọng lượng", "hai trăm gram"); // Không parse được thành số

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                attrs, Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: createProduct_Throws_InvalidAttributeValue_SelectOptionNotFound - Giá trị SELECT ngoài options")
    void createProduct_Throws_InvalidAttributeValue_SelectOptionNotFound() {
        // Arrange
        Map<String, Object> attrs = createValidAttributes();
        attrs.put("Kết nối", "Hồng ngoại"); // Không nằm trong options (Bluetooth, Wireless 2.4G, Có dây)

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                attrs, Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: createProduct_Throws_InvalidAttributeValue_UnknownKey - Thuộc tính lạ ngoài schema danh mục")
    void createProduct_Throws_InvalidAttributeValue_UnknownKey() {
        // Arrange
        Map<String, Object> attrs = createValidAttributes();
        attrs.put("KeyLạKhôngTồnTại", "Giá trị");

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                attrs, Collections.emptyList(), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: createProduct_Throws_MissingCoverImage - Thiếu ảnh có displayOrder = 0 (BR-11)")
    void createProduct_Throws_MissingCoverImage() {
        // Arrange
        CreateProductImageRequest img1 = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 1);
        CreateProductImageRequest img2 = new CreateProductImageRequest("https://cdn.techhub.vn/img2.jpg", 2);

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                createValidAttributes(), List.of(img1, img2), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: createProduct_Throws_MultipleCoverImages - Có 2 ảnh cùng mang displayOrder = 0")
    void createProduct_Throws_MultipleCoverImages() {
        // Arrange
        CreateProductImageRequest img1 = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductImageRequest img2 = new CreateProductImageRequest("https://cdn.techhub.vn/img2.jpg", 0);

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                createValidAttributes(), List.of(img1, img2), Collections.emptyList()
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: createProduct_Throws_DuplicateVariantSku - Trùng mã SKU giữa các biến thể")
    void createProduct_Throws_DuplicateVariantSku() {
        // Arrange
        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductVariantRequest v1 = new CreateProductVariantRequest(
                "DUPLICATE-SKU", "Var 1", "Desc",
                new BigDecimal("100000"), 10, null, null
        );
        CreateProductVariantRequest v2 = new CreateProductVariantRequest(
                "DUPLICATE-SKU", "Var 2", "Desc",
                new BigDecimal("200000"), 20, null, null
        );

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                createValidAttributes(), List.of(img), List.of(v1, v2)
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.DUPLICATE_VARIANT_SKU, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-17: createProduct_Success_VariantAttributeAllowedToShareKeyWithSpecs - Cho phép variant mang thuộc tính trùng tên với specs (vd: RAM, Kết nối)")
    void createProduct_Success_VariantAttributeAllowedToShareKeyWithSpecs() {
        // Arrange
        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        Map<String, Object> varAttrs = Map.of("Kết nối", "Bluetooth"); // Variant có cùng key với specs chung "Kết nối"

        CreateProductVariantRequest v = new CreateProductVariantRequest(
                "SKU-01", "Var 1", "Desc",
                new BigDecimal("100000"), 10, null, varAttrs
        );

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                createValidAttributes(), List.of(img), List.of(v)
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productRepository.existsBySlug(any())).thenReturn(false);

        UUID productId = UUID.randomUUID();
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(productId);
            return p;
        });
        when(productImageRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productVariantRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductDetailResponse response = sellerProductService.createProduct(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(productId, response.id());
        assertEquals(1, response.variants().size());
        assertEquals("Bluetooth", response.variants().get(0).attributes().get("Kết nối"));
    }

    @Test
    @DisplayName("TC-18: createProduct_Throws_CaseInsensitiveDuplicateSku - Trùng lặp SKU bất kể hoa thường (IP15-BLK vs ip15-blk)")
    void createProduct_Throws_CaseInsensitiveDuplicateSku() {
        // Arrange
        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductVariantRequest v1 = new CreateProductVariantRequest(
                "APL-KB-01", "Variant Trắng", "Desc",
                new BigDecimal("2500000.00"), 10, null, null
        );
        CreateProductVariantRequest v2 = new CreateProductVariantRequest(
                "apl-kb-01", "Variant Đen", "Desc",
                new BigDecimal("2500000.00"), 10, null, null
        );

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                createValidAttributes(), List.of(img), List.of(v1, v2)
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.DUPLICATE_VARIANT_SKU, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-19: createProduct_Throws_InvalidNumberAttribute_NaN - Chặn giá trị NaN hoặc Infinity cho NUMBER")
    void createProduct_Throws_InvalidNumberAttribute_NaN() {
        // Arrange
        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductVariantRequest v = new CreateProductVariantRequest(
                "APL-KB-01", "Tiêu chuẩn", "Desc",
                new BigDecimal("2500000.00"), 10, null, null
        );

        Map<String, Object> attrsWithNaN = new HashMap<>(createValidAttributes());
        attrsWithNaN.put("Trọng lượng", "NaN");

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Product Name", "Brand", "Desc",
                attrsWithNaN, List.of(img), List.of(v)
        );
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.createProduct(userId, request));
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_VALUE, ex.getErrorCode());
    }
}
