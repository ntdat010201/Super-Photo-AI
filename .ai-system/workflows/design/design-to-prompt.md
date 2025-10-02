#design-to-prompt
---
description: 
globs: 
  - "**/*.{png,jpg,jpeg,webp}"
alwaysApply: false
---
# Design to Prompt Analysis

## Nguyên Tắc Cơ Bản
- ***BẮT BUỘC*** phân tích hình ảnh ứng dụng di động một cách chi tiết và có cấu trúc
- ***BẮT BUỘC*** xác định tất cả tính năng và khả năng kết nối giữa chúng
- ***BẮT BUỘC*** đề xuất tích hợp AI để tạo ra giá trị mới
- ***BẮT BUỘC*** phân tích theo góc độ marketing và monetization
- ***KHUYẾN NGHỊ*** tập trung vào gamification để giữ chân người dùng

## Quy Trình Phân Tích 4 Bước

### Bước 1: Trích xuất Tính năng (Feature Extraction)
1. **Xác định:** Dựa vào hình ảnh, xác định tất cả các tính năng mà ứng dụng cung cấp
2. **Đặt tên:** Đặt tên cho từng tính năng một cách ngắn gọn, rõ ràng, thể hiện được chức năng chính
3. **Mô tả:** Viết một câu mô tả ngắn gọn về chức năng của từng tính năng

### Bước 2: Phân tích Chuyên sâu Tính năng (Feature Deep Dive)
Đối với mỗi tính năng được xác định ở Bước 1, thực hiện:

1. **Mô tả chi tiết:**
   - Mô tả chi tiết hơn về **chức năng** của tính năng
   - **Cách thức hoạt động** của tính năng (các bước thực hiện, xử lý...)

2. **Màn hình:**
   - Liệt kê tất cả các **màn hình** liên quan đến tính năng này
   - Mô tả sơ lược nội dung và các thành phần chính trên từng màn hình

3. **Luồng người dùng (User Flow):**
   - Vẽ sơ đồ hoặc mô tả chi tiết **các bước** người dùng thực hiện khi sử dụng tính năng
   - Bao gồm các hành động của người dùng và phản hồi của ứng dụng

4. **Logic:**
   - Giải thích **logic hoạt động** của tính năng
   - Các **quy tắc, điều kiện** để tính năng hoạt động
   - Mô tả **cách thức xử lý dữ liệu** của tính năng

5. **Dữ liệu:**
   - Xác định **các loại dữ liệu** mà tính năng sử dụng và cung cấp
   - Nguồn gốc của dữ liệu (từ người dùng, từ hệ thống, từ API bên ngoài...)

6. **Yêu cầu:**
   - Liệt kê các **yêu cầu cần thiết** để tính năng có thể hoạt động
   - Ví dụ: quyền truy cập vị trí, kết nối internet, quyền truy cập danh bạ, quyền thông báo...

### Bước 3: Kết nối Tính năng (Feature Connection)
1. **Xác định liên kết:**
   - Dựa vào phân tích ở Bước 2, tìm ra các **mối liên hệ** giữa các tính năng
   - Tính năng nào sử dụng dữ liệu từ tính năng nào?
   - Tính năng nào bổ sung, hỗ trợ cho tính năng nào?

2. **Phân tích kết hợp:**
   - Với mỗi cặp tính năng có liên quan, mô tả **cách chúng kết hợp với nhau** để mang lại giá trị cho người dùng

3. **Đề xuất kết hợp:**
   - Dựa vào dữ liệu và logic của các tính năng, đề xuất **các cách kết hợp tiềm năng** khác

4. **Các tính năng phụ:**
   - Dựa vào các tính năng chính sẽ mô tả các tính năng phụ kèm theo
   - Ví dụ: profile thì sẽ có cập nhật avatar, các fields cần thiết của profile phù hợp với app

5. **API bên thứ 3:**
   - Đề xuất các tính năng mà có thể thu thập được hoặc sử dụng được thông qua API của bên thứ 3 với giá rẻ
   - Ví dụ như API thời tiết, API về tỷ giá...

6. **Phân tích nhu cầu người dùng:**
   - Nhìn thấy điểm đau của người dùng (nhu cầu)
   - Ví dụ một người cao huyết áp họ khao khát điều gì, một người thường xuyên phải nằm trên giường bệnh thì ngoài bệnh tật họ cần hỗ trợ các vấn đề như tâm lý, giảm căng thẳng...
   - Từ đó đưa ra các tính năng hỗ trợ họ

7. **Tham khảo thị trường:**
   - Tham khảo các app cùng loại để bổ sung thêm các tính năng cần thiết và có thể thực hiện được

### Bước 4: Tiềm năng AI (AI Potential)
1. **Phân tích AI cho từng tính năng:**
   - Với mỗi tính năng, đề xuất cách ứng dụng AI để **cải thiện** tính năng đó
   - Sử dụng AI API server dify với URL ai.dreamapi.net (gpt, claude, gemini)

2. **Phân tích AI cho kết hợp tính năng:**
   - Với mỗi kết hợp tính năng, đề xuất cách ứng dụng AI để tạo ra **tính năng/giá trị mới**

3. **Mô tả lợi ích:**
   - Với mỗi đề xuất AI, mô tả **lợi ích cụ thể** mà nó mang lại cho người dùng
   - Chính xác hơn, cá nhân hóa hơn, tiện lợi hơn, an toàn hơn...

4. **Kết hợp dữ liệu:**
   - Kết hợp các dữ liệu khác để cá nhân hóa phân tích AI

## Marketing & Monetization Analysis

### Phân Tích Thị Trường Target
- ***BẮT BUỘC*** phân tích theo tiêu chí eCPM của các quốc gia
- ***BẮT BUỘC*** ưu tiên các thị trường có eCPM cao: Mỹ, Châu Âu, Hàn Quốc, Nhật Bản
- ***KHUYẾN NGHỊ*** bổ sung thị trường tier 3 để giảm chi phí marketing

### Phân Tích Chi Phí vs Doanh Thu
- Tập trung vào lợi ích về doanh thu quảng cáo vs chi phí vận hành (đặc biệt chi phí AI)
- Phân tích ROI của từng tính năng AI được đề xuất
- Đề xuất các tính năng có tỷ lệ engagement cao nhưng chi phí thấp

### Phân Tích Người Dùng Target
- Phân tích thói quen sinh hoạt dẫn đến nhu cầu sử dụng app
- Ví dụ: ứng dụng blood pressure → phân tích nhóm quốc gia có thói quen sinh hoạt dẫn tới nhu cầu blood pressure cao
- Kết hợp với eCPM để chọn lọc nhóm người dùng tối ưu

### Tính Năng Cross-sell
- Phân tích sở thích của target user để bổ sung các tính năng liên quan
- Ví dụ: nhóm người dùng thích chụp ảnh → bổ sung tính năng thời tiết, mẹo chụp ảnh

## Ví Dụ Phân Tích: Tính Năng "Thời tiết"

### Thông Tin Cơ Bản
- **Tên tính năng:** Thời tiết
- **Mô tả:** Cung cấp thông tin thời tiết hiện tại và dự báo
- **Yêu cầu:** Quyền truy cập vị trí, kết nối internet

### Màn Hình
- **Overview:** Hiển thị thời tiết hiện tại, thời tiết theo giờ, thời tiết theo ngày, chất lượng không khí
- **Detail:** Hiển thị thông tin chi tiết về các yếu tố thời tiết (nhiệt độ, độ ẩm, sức gió, tầm nhìn...)
- **Cảnh báo:** Hiển thị cảnh báo về các hiện tượng thời tiết nguy hiểm (mưa to, bão...)

### Luồng Người Dùng
1. Mở ứng dụng → Xem thời tiết hiện tại ở màn hình Overview
2. Vuốt sang trái/phải để xem thời tiết theo giờ/ngày
3. Nhấn vào một yếu tố thời tiết để xem thông tin chi tiết ở màn hình Detail
4. Nhận thông báo khi có cảnh báo thời tiết

### Logic
- Cập nhật dữ liệu thời tiết mỗi 1h, 6h, 12h (tùy cấu hình)
- Hiển thị cảnh báo khi có mưa to, bão, hoặc các điều kiện thời tiết nguy hiểm khác
- Yêu cầu quyền truy cập vị trí để cung cấp thông tin thời tiết chính xác

### Dữ Liệu
- Nhiệt độ, độ ẩm, sức gió, hướng gió, lượng mưa, tầm nhìn, chỉ số UV, chất lượng không khí
- Nguồn dữ liệu: từ API của nhà cung cấp dữ liệu thời tiết

### Kết Nối
- **Chụp ảnh:** Cung cấp thông tin về điều kiện ánh sáng dựa trên thời tiết để hỗ trợ chụp ảnh
- **Chạy bộ:** Cung cấp thông tin về nhiệt độ, độ ẩm, sức gió để đưa ra gợi ý về thời điểm và cường độ chạy bộ phù hợp
- **Sức khỏe:** Cung cấp cảnh báo về các nguy cơ sức khỏe liên quan đến thời tiết (say nắng, cảm lạnh...)

### Tiềm Năng AI
- Dự báo thời tiết chính xác hơn dựa trên dữ liệu lịch sử và các mô hình học máy
- Cá nhân hóa thông tin thời tiết dựa trên sở thích và thói quen của người dùng
- Phân tích dữ liệu thời tiết và dữ liệu sức khỏe để đưa ra cảnh báo về nguy cơ say nắng, đột quỵ

## Output Format

### Phân tích bằng tiếng Việt
- Sử dụng tiếng Việt cho toàn bộ phân tích và giải thích
- Giọng điệu chuyên nghiệp nhưng dễ hiểu
- Cấu trúc rõ ràng theo 4 bước đã định nghĩa

### Instruction bằng tiếng Anh
- Đặt instruction cuối cùng trong code block
- Sử dụng tiếng Anh chuẩn cho instruction
- Bao gồm đầy đủ thông tin cần thiết để AI khác có thể hiểu và thực hiện
