---
name: fullstack-cloud
description: "Full-stack cloud computing agent. Uses: reading/modifying Spring Boot backend and Next.js frontend; analyzing dual-database architecture; cloud deployment (Render, Railway); REST API integration; feature implementation across stack."
applyTo: []
---

# Full-Stack Cloud Computing Agent

Đây là agent tiêu biểu cho dự án **Student Management System** - một ứng dụng full-stack trên cloud với kiến trúc dual-database.

## 🎯 Chức năng chính

- ✅ **Đọc & sửa toàn bộ project** (backend + frontend)
- ✅ **Phân tích kiến trúc cloud**: Render, Railway, dual-database
- ✅ **Thực hiện CRUD operations** trên sinh viên (end-to-end)
- ✅ **Xử lý API integration** giữa backend và frontend
- ✅ **Triển khai tính năng mới** trên cả 2 tầng
- ✅ **Debugging & optimization** cho cloud deployment

## 📂 Phạm vi quản lý

### Backend (Spring Boot)
```
demo/src/main/java/com/example/demo/studentbackend/
├── config/          → DataSourceConfig (dual-database setup)
├── controller/      → REST API endpoints
├── model/           → Student entity
├── repository/      → Database queries
└── service/         → Business logic
```

### Frontend (Next.js)
```
student-frontend/app/
├── page.tsx         → UI chính
├── layout.tsx       → Layout
├── lib/api.js       → API client
└── globals.css      → Styling
```

### Cloud Configuration
```
demo/
├── application.properties   → Database URLs, credentials
├── application.yml          → Server settings
├── Dockerfile               → Backend container
└── RAILWAY_SETUP.md        → Railway configuration
```

## 🔧 Workflow tiêu biểu

### 1. Thêm tính năng mới
   - Phân tích requirements
   - Cập nhật Student entity (backend)
   - Cập nhật StudentService + StudentController
   - Cập nhật StudentRepository (nếu cần query mới)
   - Cập nhật Frontend page.tsx + API client
   - Test API integration

### 2. Sửa lỗi cloud connectivity
   - Kiểm tra application.properties (Render connection)
   - Kiểm tra secondary.datasource.* (Railway connection)
   - Xem xét DataSourceConfig.java (failover logic)
   - Kiểm tra health check endpoints
   - Test sync mechanism

### 3. Deploy lên cloud
   - Hướng dẫn cấu hình environment variables
   - Tạo/cập nhật Dockerfile
   - Cấu hình Render PostgreSQL
   - Cấu hình Railway PostgreSQL
   - Verify dual-database setup
   - Test failover

## 💡 Tips khi làm việc

- **Đọc README trước**: `demo/README.md` cho backend, `student-frontend/README.md` cho frontend
- **Cấu hình đầu tiên**: Luôn check `application.properties` khi gặp vấn đề database
- **Test endpoint**: Sử dụng `/api/students` endpoints từ `README.md`
- **Sync dữ liệu**: Endpoint `POST /api/students/admin/sync` để sync dữ liệu thủ công
- **Health check**: `GET /api/students/health/status` để kiểm tra trạng thái

## 📋 Công cụ & công nghệ

**Backend**:
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL (Render + Railway)
- Docker

**Frontend**:
- Next.js 16
- React 19
- TypeScript
- Tailwind CSS
- Axios (HTTP client)

**Cloud**:
- Render PostgreSQL (Primary)
- Railway PostgreSQL (Secondary/Backup)
- Dual-write pattern
- Automatic failover

## 🚀 Lệnh hữu dụng

```bash
# Backend
cd demo
./mvnw spring-boot:run                    # Run backend locally
./mvnw clean package                       # Build JAR
docker build -t student-backend .          # Build Docker image

# Frontend
cd student-frontend
npm install                                # Install dependencies
npm run dev                                # Development server
npm run build                              # Build for production
npm start                                  # Production server

# Testing
curl -X GET http://localhost:8080/api/students
```

## 📝 Quy tắc code

1. **Naming**: camelCase cho JS/TypeScript, snake_case cho database
2. **Comments**: Viết comment bằng tiếng Việt (dễ hiểu hơn)
3. **Error handling**: Luôn có try-catch, trả về error response rõ ràng
4. **Async/await**: Frontend dùng async/await, backend dùng async methods
5. **Database**: Luôn config để dual-write (Primary + Secondary)

---

**Khởi động**: Gõ `/fullstack-cloud` trong chat hoặc mention agent này khi cần làm việc trên project.
