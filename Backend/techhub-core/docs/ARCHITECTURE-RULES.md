# TechHub - Quy tắc kiến trúc & thiết kế API

Tài liệu bắt buộc khi thêm/sửa tính năng backend (`Backend/techhub-core`). Mô hình tham chiếu: TradeSphere (persona-based, package-by-feature). Nếu quy tắc ở đây xung đột với thói quen cũ, **quy tắc này thắng**; muốn đổi quy tắc phải sửa tài liệu này trong cùng PR.

## 1. Cấu trúc package (package-by-feature)

```
com.techhub
  common/        tiện ích dùng chung (ApiResponse, PageResponse, ...), không chứa nghiệp vụ
  config/        cấu hình Spring (SecurityConfig, ...)
  security/      JWT, filter, UserPrincipal
  exception/     exception + GlobalExceptionHandler
  task/          job/scheduler
  modules/<feature>/
     controller/ REST controller (chỉ HTTP: nhận request, gọi service, bọc ApiResponse)
     dto/        request/response
     entity/ enums/ projection/ repository/ specification/
     service/    interface + (impl) nghiệp vụ
```

Quy tắc:
- R1. Một tính năng = một `modules/<feature>`. Không tạo package theo tầng ở gốc (`com.techhub.controller` v.v.).
- R2. Phụ thuộc: `controller -> service -> repository`. Controller **không** gọi repository, không chứa nghiệp vụ.
- R3. Feature A dùng feature B qua `service` của B, không truy cập `repository`/`entity` nội bộ của B nếu có service tương ứng.
- R4. Entity không bao giờ trả ra ngoài API; luôn qua DTO.
- R5. Code dùng chung giữa >= 2 feature mới vào `common`.

## 2. Ba bề mặt URL (persona)

| Bề mặt | Prefix | Ai dùng | Guard |
|---|---|---|---|
| Public / Consumer | `/api/<resource>` | guest + buyer | GET public: permitAll; còn lại `@PreAuthorize("hasRole('BUYER')")` hoặc `isAuthenticated()` |
| Seller | `/api/seller/<resource>` | SELLER | `@PreAuthorize("hasRole('SELLER')")` |
| Admin | `/api/admin/<resource>` | ADMIN | `@PreAuthorize("hasRole('ADMIN')")` |

- U1. **Cấm** `/api/buyer/**`. Buyer/guest dùng URL không tiền tố.
- U2. Seller luôn có thêm BUYER: URL không tiền tố = góc nhìn người mua; `/api/seller/**` = góc nhìn người bán. Không dùng `?perspective=`, không suy luận theo role để đổi dữ liệu cùng một URL.
- U3. Mọi controller **bắt buộc** có `@PreAuthorize` cấp class (trừ controller hoàn toàn public). Không dựa vào "URL không ai biết".
- U4. Cùng một resource được phép có 2-3 controller (một cho mỗi persona), mỗi controller có DTO riêng. Không gộp DTO nhiều role vào một DTO nullable.
- U5. Admin: `/api/admin/<resource>` cho CRUD/kiểm duyệt, `/api/admin/dashboard`... cho tổng hợp. Không có `/moderation`.
- U6. Không dùng `/me` để thay thế persona. `/me` chỉ cho hồ sơ định danh của chính người dùng (`/api/users/profile`, `/api/auth/me`).
- U7. Tích hợp bên thứ ba có URL cố định, không đổi: `/api/payment/vnpay/ipn`, `/api/payment/vnpay/callback`.
- U8. Không thêm `/v1` (trừ khi tài liệu này được cập nhật).

## 3. Đặt tên endpoint

- N1. Segment resource: danh từ số nhiều, kebab-case (`return-requests`, `payment-methods`).
- N2. HTTP method theo nghĩa: GET đọc, POST tạo/hành động, PUT thay thế, PATCH cập nhật một phần, DELETE xóa.
- N3. Hành động nghiệp vụ (command/transition) được phép dưới dạng sub-path động từ/danh từ hành động: `POST /orders/{id}/cancel`, `PATCH /orders/{id}/complete`, `POST /orders/{id}/reorder`. Chỉ dùng khi không biểu diễn được bằng CRUD.
- N4. Resource con lồng tối đa 2 cấp: `/orders/{id}/return-request`.
- N5. Không tạo collection vô nghĩa nghiệp vụ. Không đặt path trùng template với `/{slug}`/`/{id}` của resource cha (vd **cấm** `/shops/followed`, vì đụng `/shops/{sellerName}`).
- N6. Path tĩnh (`/recently-viewed`, `/platform`) phải đặt cùng controller với path động và không được bị che bởi `/{id}`.
- N7. Phân biệt `payment` (VNPay) và `payments`/`payment-methods` (tra cứu của buyer): không dùng gần giống nhau cho mục đích khác.

## 4. Controller, DTO, đặt tên class

- C1. Tên controller: `<Resource>Controller` (buyer/public), `Seller<Resource>Controller`, `Admin<Resource>Controller`. **Không** `Buyer*Controller`.
- C2. Một `@RequestMapping` cấp class duy nhất cho mỗi resource/persona; không có hai controller cùng prefix + cùng persona.
- C3. DTO: `<Persona><Resource>Response` khi khác nội dung theo persona (`SellerOrderResponse`, `AdminOrderResponse`); DTO dùng chung khi dữ liệu y hệt. Request: `<Action><Resource>Request`.
- C4. Mọi response bọc `ApiResponse<T>`; phân trang dùng `PageResponse<T>`.
- C5. Không đưa dữ liệu nhạy cảm (hoa hồng, thu nhập, PII người khác, payment nội bộ) vào DTO của persona không có quyền. Thêm trường cho persona nào thì chỉ thêm vào DTO persona đó.
- C6. Validation bằng `@Valid` + Bean Validation; lỗi ném qua exception chung, không tự trả mã lỗi trong controller.

## 5. Bảo mật

- S1. `SecurityConfig` chỉ chứa: health, auth public, GET public (shops/categories/products/vouchers), VNPay IPN/callback, rồi `anyRequest().authenticated()`.
- S2. Endpoint mới mặc định **không** public. Muốn public phải thêm matcher GET cụ thể, có test anonymous.
- S3. Phân quyền theo role nằm ở `@PreAuthorize` cấp class; kiểm quyền sở hữu (buyer chỉ đơn của mình, seller chỉ shop của mình) nằm ở service.
- S4. Không nới matcher rộng (`/**`) cho path mới; thêm matcher hẹp trước matcher rộng.

## 6. Checklist khi thêm tính năng mới

1. Tính năng thuộc `modules/<feature>` nào? Chưa có -> tạo mới đủ cấu trúc mục 1.
2. Ai dùng? Chọn bề mặt URL (mục 2); nếu nhiều persona -> mỗi persona một controller + DTO.
3. Đặt tên path/method theo mục 3; kiểm tra trùng template với endpoint hiện có.
4. Gắn `@PreAuthorize` cấp class; nếu public -> matcher GET hẹp.
5. Controller chỉ gọi service; nghiệp vụ trong service.
6. Viết test: happy path, 401 (anonymous), 403 (sai role), và (nếu dùng path tĩnh/động) test không ambiguous mapping.
7. Cập nhật bảng endpoint trong tài liệu API nếu có.

## 7. Anti-pattern (bị từ chối khi review)

- `/api/buyer/...`, `/api/moderation/...`, `?role=`/`?perspective=` để đổi dữ liệu.
- Một DTO dùng cho 3 role với trường nullable "chỉ admin mới có".
- Controller gọi repository trực tiếp; trả entity.
- Gộp action seller (ship/deliver) vào controller buyer.
- Thêm `permitAll` rộng để "cho tiện".
- Đổi URL VNPay.

## 8. Quirk hiện có (không sửa nếu không có yêu cầu riêng)

- `GET /api/products/recently-viewed`, `GET /api/shops/**` nằm trong matcher permitAll nhưng class có `@PreAuthorize` BUYER -> anonymous bị từ chối bởi method security.
- Một số controller (Address, User, Conversation) dùng `isAuthenticated()`/không class-level role.
- Test `CheckoutControllerTest.controller_placeOrderMethodAnnotations_Correct` đang lỗi từ trước.

## 9. Nhật ký quyết định
- Chọn persona-based (TradeSphere) thay vì REST thuần canonical (`REFACTORING/07`) vì seller có đồng thời BUYER+SELLER, trùng template `{slug}`/`{id}`, DTO khác nhau theo role, và để không đổi nghiệp vụ.
- Giữ `controller` làm tên package (không đổi sang `web`).
