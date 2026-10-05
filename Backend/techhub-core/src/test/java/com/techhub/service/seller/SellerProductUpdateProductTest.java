package com.techhub.service.seller;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateProductImageRequest;
import com.techhub.model.dto.request.UpdateProductRequest;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerProductServiceImpl (PUT /api/seller/products/{id})")
class SellerProductUpdateProductTest {

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
    private UUID newCategoryId;
    private UUID productId;

    private Seller seller;
    private Shop shop;
    private Category category;
    private Category newCategory;
    private List<CategoryAttribute> categoryAttributes;
    private Product sampleProduct;
    private List<ProductVariant> sampleVariants;
    private UpdateProductRequest validRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        newCategoryId = UUID.randomUUID();
        productId = UUID.randomUUID();

        seller = Seller.builder()
                .id(sellerId)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name("Apple Store Official")
                .status(ShopStatus.ACTIVE)
                .build();

        category = Category.builder()
                .id(categoryId)
                .name("Điện thoại thông minh")
                .status(CategoryStatus.ACTIVE)
                .build();

        newCategory = Category.builder()
                .id(newCategoryId)
                .name("Máy tính bảng")
                .status(CategoryStatus.ACTIVE)
                .build();

        CategoryAttribute attrRam = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("8GB", "16GB", "32GB"))
                .build();

        CategoryAttribute attrChip = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Chipset")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();

        categoryAttributes = List.of(attrRam, attrChip);

        sampleProduct = Product.builder()
                .id(productId)
                .shop(shop)
                .category(category)
                .name("iPhone 15 Pro Max 256GB")
                .slug("iphone-15-pro-max-256gb-abc12345")
                .brandName("Apple")
                .description("<p>Mô tả ban đầu</p>")
                .attributes(Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"))
                .approvalStatus(ProductApprovalStatus.DRAFT)
                .visibilityStatus(ProductVisibilityStatus.UNPUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build();

        ProductVariant variant1 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .sku("IP15PM-256-NAT")
                .variantName("Titan Tự Nhiên 256GB")
                .price(BigDecimal.valueOf(29990000))
                .stockQuantity(100)
                .imageUrl("https://cdn.techhub.com/ip15-nat.jpg")
                .attributes(Map.of("Màu sắc", "Titan Tự Nhiên"))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        sampleVariants = List.of(variant1);

        validRequest = new UpdateProductRequest(
                categoryId,
                "iPhone 15 Pro Max 256GB",
                "Apple",
                "<p>Mô tả cập nhật mới</p>",
                Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"),
                List.of(
                        new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0),
                        new CreateProductImageRequest("https://cdn.techhub.com/sub.jpg", 1)
                )
        );
    }

    private void mockCommonSuccessDependencies() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("TC-01: Cập nhật sản phẩm DRAFT thành công -> Giữ nguyên DRAFT và UNPUBLISHED")
    void updateProduct_Success_WhenDraft_KeepsDraftAndUnpublished() {
        mockCommonSuccessDependencies();

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, validRequest);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.DRAFT, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        assertEquals("<p>Mô tả cập nhật mới</p>", response.description());
        assertEquals(2, response.images().size());
        verify(productImageRepository).deleteByProductId(productId);
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-02: Cập nhật sản phẩm APPROVED thành công -> Reset sang PENDING_APPROVAL và UNPUBLISHED (BR-08)")
    void updateProduct_Success_WhenApproved_TriggersContentChangePendingApprovalAndUnpublished() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.APPROVED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        mockCommonSuccessDependencies();

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, validRequest);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
    }

    @Test
    @DisplayName("TC-03: Cập nhật sản phẩm REJECTED thành công -> Giữ nguyên REJECTED chờ gửi duyệt lại")
    void updateProduct_Success_WhenRejected_KeepsRejected() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.REJECTED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);

        mockCommonSuccessDependencies();

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, validRequest);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.REJECTED, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
    }

    @Test
    @DisplayName("TC-04: Cập nhật sản phẩm PENDING_APPROVAL thành công -> Giữ nguyên PENDING_APPROVAL")
    void updateProduct_Success_WhenPendingApproval_KeepsPendingApproval() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);

        mockCommonSuccessDependencies();

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, validRequest);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
    }

    @Test
    @DisplayName("TC-05: Cập nhật tên mới -> Tái sinh Slug mới (BR-10)")
    void updateProduct_Success_WhenNameChanged_RegeneratesSlug() {
        mockCommonSuccessDependencies();
        when(productRepository.existsBySlug(any())).thenReturn(false);

        UpdateProductRequest requestWithNameChange = new UpdateProductRequest(
                categoryId,
                "iPhone 15 Pro Max Titanium Edition",
                "Apple",
                "<p>Mô tả</p>",
                Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"),
                List.of(new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0))
        );

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, requestWithNameChange);

        assertNotNull(response);
        assertTrue(response.slug().startsWith("iphone-15-pro-max-titanium-edition-"));
        assertNotEquals("iphone-15-pro-max-256gb-abc12345", response.slug());
    }

    @Test
    @DisplayName("TC-06: Giữ nguyên tên cũ -> Bảo tồn Slug cũ không bị thay đổi")
    void updateProduct_Success_WhenNameUnchanged_PreservesExistingSlug() {
        mockCommonSuccessDependencies();

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, validRequest);

        assertNotNull(response);
        assertEquals("iphone-15-pro-max-256gb-abc12345", response.slug());
        verify(productRepository, never()).existsBySlug(any());
    }

    @Test
    @DisplayName("TC-07: Cập nhật danh mục mới ACTIVE thành công")
    void updateProduct_Success_WhenCategoryChanged() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));

        CategoryAttribute tabletScreenAttr = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(newCategory)
                .name("Kích thước màn hình")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();
        when(categoryAttributeRepository.findByCategoryId(newCategoryId)).thenReturn(List.of(tabletScreenAttr));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductRequest changeCatRequest = new UpdateProductRequest(
                newCategoryId,
                "iPhone 15 Pro Max 256GB",
                "Apple",
                "<p>Đổi sang danh mục tablet</p>",
                Map.of("Kích thước màn hình", "11 inch"),
                List.of(new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0))
        );

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, changeCatRequest);

        assertNotNull(response);
        assertEquals(newCategoryId, response.categoryId());
        assertEquals("Máy tính bảng", response.categoryName());
    }

    @Test
    @DisplayName("TC-08: Shop đang PAUSED được phép cập nhật sản phẩm (BR-07)")
    void updateProduct_Success_WhenShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);
        mockCommonSuccessDependencies();

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, validRequest);

        assertNotNull(response);
        assertEquals("iPhone 15 Pro Max 256GB", response.name());
    }

    @Test
    @DisplayName("TC-09: Shop bị BANNED -> Chặn tuyệt đối với mã SHOP_NOT_ACTIVE (HTTP 400)")
    void updateProduct_Fail_ShopBanned_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, validRequest));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: Shop bị SUSPENDED -> Chặn thao tác ghi với mã SHOP_NOT_ACTIVE (HTTP 400)")
    void updateProduct_Fail_ShopSuspended_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, validRequest));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: Không tìm thấy Product ID -> Ném PRODUCT_NOT_FOUND (HTTP 404)")
    void updateProduct_Fail_ProductNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, validRequest));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: Truy cập chéo Product của Shop khác -> Ném PRODUCT_NOT_FOUND (HTTP 404 - Anti-ID Enumeration)")
    void updateProduct_Fail_CrossTenantAccess_ProductBelongsToAnotherShop() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, validRequest));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: Sản phẩm đã bị xóa mềm (isDeleted = true) -> Ném PRODUCT_NOT_FOUND (HTTP 404)")
    void updateProduct_Fail_ProductSoftDeleted() {
        sampleProduct.setDeleted(true);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, validRequest));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: Danh mục mới không tồn tại -> Ném CATEGORY_NOT_FOUND (HTTP 404)")
    void updateProduct_Fail_NewCategoryNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.empty());

        UpdateProductRequest req = new UpdateProductRequest(
                newCategoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of(),
                List.of(new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0))
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: Danh mục mới đang INACTIVE -> Ném CATEGORY_INACTIVE (HTTP 400)")
    void updateProduct_Fail_NewCategoryInactive() {
        newCategory.setStatus(CategoryStatus.INACTIVE);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));

        UpdateProductRequest req = new UpdateProductRequest(
                newCategoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of(),
                List.of(new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0))
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: Thiếu thuộc tính bắt buộc của danh mục -> Ném MISSING_CATEGORY_ATTRIBUTE (HTTP 400)")
    void updateProduct_Fail_MissingCategoryAttribute() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        // Chỉ gửi RAM, thiếu Chipset
        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("RAM", "8GB"),
                List.of(new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0))
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.MISSING_CATEGORY_ATTRIBUTE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-17: Thuộc tính sai kiểu dữ liệu -> Ném INVALID_ATTRIBUTE_VALUE (HTTP 400)")
    void updateProduct_Fail_InvalidAttributeValue_WrongDataType() {
        CategoryAttribute numAttr = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Dung lượng pin")
                .dataType(CategoryAttributeDataType.NUMBER)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(List.of(numAttr));

        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("Dung lượng pin", "ba nghìn mAh"), // Không phải số
                List.of(new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0))
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.INVALID_ATTRIBUTE_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-18: Thuộc tính SELECT có giá trị không thuộc options -> Ném INVALID_ATTRIBUTE_VALUE (HTTP 400)")
    void updateProduct_Fail_InvalidAttributeValue_OptionNotInSelect() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);

        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("RAM", "128GB", "Chipset", "Apple A17 Pro"), // 128GB không có trong [8GB, 16GB, 32GB]
                List.of(new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0))
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.INVALID_ATTRIBUTE_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-19: updateProduct_Success_SpecsKeyAllowedToMatchVariantAttributes - Cho phép thuộc tính specs trùng tên với variant (vd: Màu sắc, RAM)")
    void updateProduct_Success_SpecsKeyAllowedToMatchVariantAttributes() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        CategoryAttribute colorAttr = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Màu sắc") // Variant1 attributes cũng có {"Màu sắc": "Titan Tự Nhiên"}
                .dataType(CategoryAttributeDataType.TEXT)
                .build();
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(List.of(colorAttr));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("Màu sắc", "Titan Xanh"),
                List.of(new CreateProductImageRequest("https://cdn.techhub.com/cover.jpg", 0))
        );

        ProductDetailResponse response = sellerProductService.updateProduct(userId, productId, req);

        assertNotNull(response);
        assertEquals("Titan Xanh", response.attributes().get("Màu sắc"));
        assertEquals("Titan Tự Nhiên", response.variants().get(0).attributes().get("Màu sắc"));
    }

    @Test
    @DisplayName("TC-20: Bộ ảnh không có ảnh nào có displayOrder = 0 -> Ném INVALID_PRODUCT_IMAGES (HTTP 400)")
    void updateProduct_Fail_InvalidImages_NoCoverImage() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"),
                List.of(
                        new CreateProductImageRequest("https://cdn.techhub.com/img1.jpg", 1),
                        new CreateProductImageRequest("https://cdn.techhub.com/img2.jpg", 2)
                )
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-21: Bộ ảnh có nhiều hơn 1 ảnh Cover (displayOrder = 0) -> Ném INVALID_PRODUCT_IMAGES (HTTP 400)")
    void updateProduct_Fail_InvalidImages_MultipleCoverImages() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"),
                List.of(
                        new CreateProductImageRequest("https://cdn.techhub.com/img1.jpg", 0),
                        new CreateProductImageRequest("https://cdn.techhub.com/img2.jpg", 0)
                )
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-22: Bộ ảnh có displayOrder bị trùng lặp -> Ném INVALID_PRODUCT_IMAGES (HTTP 400)")
    void updateProduct_Fail_InvalidImages_DuplicateDisplayOrder() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"),
                List.of(
                        new CreateProductImageRequest("https://cdn.techhub.com/img1.jpg", 0),
                        new CreateProductImageRequest("https://cdn.techhub.com/img2.jpg", 1),
                        new CreateProductImageRequest("https://cdn.techhub.com/img3.jpg", 1) // Trùng order 1
                )
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-23: Bộ ảnh rỗng -> Ném INVALID_PRODUCT_IMAGES (HTTP 400)")
    void updateProduct_Fail_InvalidImages_EmptyList() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"),
                List.of()
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-24: Bộ ảnh vượt quá 6 ảnh -> Ném INVALID_PRODUCT_IMAGES (HTTP 400)")
    void updateProduct_Fail_InvalidImages_MoreThan6() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(categoryAttributeRepository.findByCategoryId(categoryId)).thenReturn(categoryAttributes);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        List<CreateProductImageRequest> sevenImages = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            sevenImages.add(new CreateProductImageRequest("https://cdn.techhub.com/img" + i + ".jpg", i));
        }

        UpdateProductRequest req = new UpdateProductRequest(
                categoryId,
                "iPhone 15",
                "Apple",
                "Desc",
                Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"),
                sevenImages
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, req));

        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-25: Seller chưa APPROVED (PENDING/REJECTED) -> Ném NOT_A_SELLER (HTTP 403)")
    void updateProduct_Fail_SellerNotApproved() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, validRequest));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-26: Không tìm thấy hồ sơ Seller -> Ném SELLER_NOT_FOUND (HTTP 404)")
    void updateProduct_Fail_SellerNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, validRequest));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-27: Không tìm thấy Shop của Seller -> Ném SHOP_NOT_FOUND (HTTP 404)")
    void updateProduct_Fail_ShopNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProduct(userId, productId, validRequest));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }
}
