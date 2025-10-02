---
description:
globs:
alwaysApply: false
---
# Brainstorm Workflow Rules

## Nguyên Tắc Cơ Bản
- ***BẮT BUỘC*** thực hiện brainstorm trước khi tạo bất kỳ planning nào
- ***BẮT BUỘC*** AI phải chủ động hỏi để thu thập đầy đủ thông tin
- ***BẮT BUỘC*** chia brainstorm thành 3 cấp độ: Cơ bản → Trung bình → Nâng cao
- ***BẮT BUỘC*** tạo file `Brainstorm_[TenDuAn].md` để lưu trữ kết quả brainstorm
- ***BẮT BUỘC*** xác nhận với người dùng trước khi chuyển sang phase planning
- ***NGHIÊM CẤM*** bắt đầu planning khi chưa hoàn thành brainstorm

## Cấu Trúc Brainstorm 3 Cấp Độ

### Cấp Độ 1: Cơ Bản (Foundation)
**Mục tiêu**: Hiểu ý tưởng cốt lõi và vấn đề cần giải quyết

**Câu hỏi bắt buộc AI phải hỏi:**
1. "Bạn có thể mô tả chi tiết ý tưởng của mình?"
2. "Vấn đề chính bạn muốn giải quyết là gì?"
3. "Ai là đối tượng người dùng chính?"
4. "Họ hiện tại đang giải quyết vấn đề này như thế nào?"
5. "Tại sao giải pháp hiện tại không đủ tốt?"

**Output cần thiết:**
- Problem statement rõ ràng
- Target audience được xác định
- Core value proposition
- Use cases chính (2-3 use cases)

### Cấp Độ 2: Trung Bình (Structure)
**Mục tiêu**: Xác định features và cấu trúc sản phẩm

**Câu hỏi bắt buộc AI phải hỏi (Max 5 cho Mobile Utility Apps):**

**Mobile Utility Apps (Health/Photo/Office) - Use mobile-utility-workflow.mdc:**
- Sử dụng 5 câu hỏi strategic specific cho từng app category
- Tự động detect app type và apply appropriate question set
- Focus vào revenue model và user behavior patterns

**Other App Types (General):**
1. "Những tính năng nào là MUST-HAVE cho MVP?"
2. "Những tính năng nào là NICE-TO-HAVE cho tương lai?"
3. "Bạn có thể mô tả workflow chính của người dùng?"
4. "Có yêu cầu đặc biệt về performance hay security không?"
5. "Bạn muốn deploy trên platform nào?"

**Output cần thiết:**
- Feature priority matrix (Must/Should/Could/Won't)
- User journey mapping
- Technical requirements
- Platform và deployment strategy

### Cấp Độ 3: Nâng Cao (Advanced)
**Mục tiêu**: Nghiên cứu thị trường và tối ưu hóa

**Câu hỏi bắt buộc AI phải hỏi:**
1. "Bạn đã nghiên cứu đối thủ cạnh tranh chưa?"
2. "Có integration nào với hệ thống bên ngoài không?"
3. "Kế hoạch scale và maintain sản phẩm như thế nào?"
4. "Có yêu cầu về analytics và tracking không?"
5. "Business model và monetization strategy?"

**AI phải tự động thực hiện:**
- Nghiên cứu 3-5 đối thủ cạnh tranh
- Phân tích market trends
- Đề xuất technical architecture
- Risk assessment

## Quy Trình Brainstorm

### Phase 0: Context7 Auto-Check (Bắt buộc)
**AI phải tự động thực hiện:**

1. **Kiểm tra Context7 knowledge base**
   - Tìm kiếm các dự án tương tự đã thực hiện
   - Phân tích market trends và competitor solutions
   - Thu thập best practices cho domain này

2. **Tech stack research**
   - Đề xuất công nghệ phù hợp dựa trên requirements
   - Kiểm tra compatibility và ecosystem support
   - Phân tích pros/cons của các options

3. **Industry insights**
   - Market research cho domain/industry
   - User behavior patterns và expectations
   - Regulatory requirements nếu có

### Phase 1: Discovery (Cấp độ Cơ bản)
1. AI thực hiện Context7 auto-check
2. AI hỏi 5 câu hỏi foundation (enhanced với Context7 insights)
3. Phân tích và tóm tắt lại hiểu biết
4. Xác nhận với người dùng
5. Tạo problem statement draft
6. Chuyển sang Phase 2

### Phase 2: Structure (Cấp độ Trung bình)
1. AI hỏi 6 câu hỏi về features và structure
2. Tạo feature priority matrix
3. Vẽ user journey map cơ bản
4. Xác định tech stack recommendation
5. Chuyển sang Phase 3

### Phase 3: Advanced Analysis (Cấp độ Nâng cao)
1. AI thực hiện competitor research
2. Hỏi 5 câu hỏi về business và scaling
3. Tạo market analysis report
4. Đề xuất architecture và technology choices
5. Tạo risk assessment

## Template File Brainstorm

```markdown
# Brainstorm: [Tên Dự Án]

## Phase 1: Foundation ✅/⏳/❌
### Problem Statement
[Mô tả vấn đề cần giải quyết]

### Target Audience
[Đối tượng người dùng chính]

### Core Value Proposition
[Giá trị cốt lõi sản phẩm mang lại]

### Main Use Cases
1. [Use case 1]
2. [Use case 2]
3. [Use case 3]

## Phase 2: Structure ✅/⏳/❌
### Feature Priority Matrix
**Must Have (MVP):**
- [Feature 1]
- [Feature 2]

**Should Have (Version 2):**
- [Feature 3]
- [Feature 4]

**Could Have (Future):**
- [Feature 5]

**Won't Have (Out of scope):**
- [Feature 6]

### User Journey
[Mô tả workflow chính của người dùng]

### Technical Requirements
- Platform: [Web/Mobile/Desktop]
- Performance: [Yêu cầu về tốc độ]
- Security: [Yêu cầu bảo mật]
- Scalability: [Khả năng mở rộng]

## Phase 3: Advanced Analysis ✅/⏳/❌
### Competitor Analysis
1. **[Đối thủ 1]**
   - Strengths: [Điểm mạnh]
   - Weaknesses: [Điểm yếu]
   - Key Features: [Tính năng chính]

2. **[Đối thủ 2]**
   - [Tương tự như trên]

### Market Insights
[Phân tích thị trường và xu hướng]

### Recommended Architecture
[Kiến trúc đề xuất]

### Technology Stack
- Frontend: [Framework đề xuất]
- Backend: [Framework đề xuất]
- Database: [Database đề xuất]
- Deployment: [Platform đề xuất]

### Risk Assessment
1. **Technical Risks:**
   - [Risk 1] - [Mitigation strategy]
   
2. **Business Risks:**
   - [Risk 2] - [Mitigation strategy]

## Brainstorm Completion ✅/⏳/❌
- [ ] Phase 1 completed and confirmed
- [ ] Phase 2 completed and confirmed  
- [ ] Phase 3 completed and confirmed
- [ ] All information validated with user
- [ ] Ready for planning phase

## Next Steps
[Kế hoạch chuyển sang planning phase]
```

## AI Behavior Rules

### Khi Người Dùng Đưa Ra Ý Tưởng Mới
1. ***BẮT BUỘC*** thông báo rằng cần thực hiện brainstorm trước
2. ***BẮT BUỘC*** bắt đầu với Phase 1 questions
3. ***BẮT BUỘC*** không nhảy sang planning ngay lập tức
4. ***BẮT BUỘC*** tạo file Brainstorm_[TenDuAn].md

### Khi Đang Trong Quá Trình Brainstorm
1. ***BẮT BUỘC*** hỏi từng câu một, không hỏi hàng loạt
2. ***BẮT BUỘC*** phân tích và feedback sau mỗi câu trả lời
3. ***BẮT BUỘC*** xác nhận hiểu biết trước khi chuyển phase
4. ***BẮT BUỘC*** cập nhật file brainstorm real-time

### Completion Criteria
Chỉ được chuyển sang planning khi:
- ✅ Tất cả 3 phases đã hoàn thành
- ✅ Người dùng đã xác nhận thông tin
- ✅ File Brainstorm_[TenDuAn].md đã được tạo và đầy đủ
- ✅ Risk assessment đã được thực hiện

## Quality Checks
- Mỗi phase phải có ít nhất 80% thông tin cần thiết
- Competitor research phải có ít nhất 3 đối thủ
- User journey phải rõ ràng và logic
- Technical requirements phải realistic và specific
