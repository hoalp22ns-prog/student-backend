---
name: full-stack-student-management
description: "Full-stack student management system with cloud deployment (Render + Railway). Use when: implementing features, debugging cloud connectivity, managing dual-database setup, or working on REST API integration."
---

# 🎓 Student Management System - Full-Stack Cloud

## 📌 Tổng quan Project

Đây là ứng dụng **quản lý sinh viên** được xây dựng trên nền tảng cloud với kiến trúc dual-database để đảm bảo high availability.

### Stack công nghệ
- **Backend**: Spring Boot 3.x + PostgreSQL (Render + Railway)
- **Frontend**: Next.js 16 + React 19 + TypeScript + Tailwind CSS
- **Cloud**: Render (primary DB) + Railway (backup DB)
- **API**: REST API với dual-write pattern

### Đặc điểm nổi bật
- ✅ Dual-database architecture (primary + backup)
- ✅ Automatic failover & sync
- ✅ Health check tự động (5s intervals)
- ✅ Zero downtime deployment
- ✅ Full CRUD operations

## 📂 Cấu trúc Project

```
d:\VS\DTDM\
├── demo/                          # Backend (Spring Boot)
│   ├── src/main/java/.../demo/studentbackend/
│   │   ├── config/                # Database config (dual-DB setup)
│   │   ├── controller/            # REST endpoints
│   │   ├── model/                 # Student entity
│   │   ├── repository/            # JPA repository
│   │   └── service/               # Business logic
│   ├── src/main/resources/
│   │   └── application.properties # DB credentials, URLs
│   ├── pom.xml                    # Maven dependencies
│   ├── Dockerfile                 # Container config
│   └── README.md                  # Backend guide
│
└── student-frontend/              # Frontend (Next.js)
    ├── app/
    │   ├── page.tsx               # Student management UI
    │   ├── layout.tsx             # Layout wrapper
    │   ├── lib/api.js             # Axios client
    │   └── globals.css            # Tailwind styles
    ├── package.json               # npm dependencies
    ├── Dockerfile                 # Frontend container
    └── README.md                  # Frontend guide
```

## 🎯 Workflow & Task phổ biến

### 1️⃣ Thêm field mới cho Student
**Files cần sửa:**
- `demo/src/main/java/.../model/Student.java` - Thêm @Column, getter/setter
- `demo/src/main/java/.../controller/StudentController.java` - Cập nhật POST/PUT logic
- `student-frontend/app/page.tsx` - Thêm input field trong form
- `student-frontend/app/lib/api.js` - (optional) Validate dữ liệu

**Flow**: Model → Service → Controller → API → Frontend

### 2️⃣ Xử lý lỗi database connectivity
**Check list:**
1. Kiểm tra `application.properties` - Render URL đúng không?
2. Kiểm tra secondary datasource - Railway URL, credentials
3. Xem `DataSourceConfig.java` - Failover logic hoạt động?
4. Test endpoint: `GET /api/students/health/status`
5. Manual sync: `POST /api/students/admin/sync`

### 3️⃣ Deploy lên cloud
**Các bước:**
1. Cấu hình Render PostgreSQL (tạo database, user, password)
2. Cấu hình Railway PostgreSQL (tương tự)
3. Cập nhật `application.properties` với credentials
4. Dockerfile backend & frontend ready
5. Push lên Render/Railway/Vercel

### 4️⃣ Debugging API Integration
**Công cụ:**
```bash
# Test backend endpoints
curl -X GET http://localhost:8080/api/students
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Nguyen Van A","email":"a@test.com","age":20}'

# Check frontend API calls
Browser DevTools → Network tab → Check requests/responses
```

## 🔗 API Endpoints

| Method | Endpoint | Mục đích |
|--------|----------|---------|
| GET | `/api/students` | Lấy danh sách sinh viên |
| POST | `/api/students` | Thêm sinh viên mới |
| PUT | `/api/students/{id}` | Cập nhật sinh viên |
| DELETE | `/api/students/{id}` | Xóa sinh viên |
| GET | `/api/students/health/status` | Health check |
| POST | `/api/students/admin/sync` | Manual sync dữ liệu |

## 💾 Database Fields (Student)

```sql
-- Current schema
id          BIGINT PRIMARY KEY AUTO_INCREMENT
name        VARCHAR(255) NOT NULL
email       VARCHAR(255) UNIQUE
phone       VARCHAR(20)
age         INT
created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
```

## 🛠️ Quy tắc & Best Practices

### Backend (Spring Boot)
- Tất cả services phải support async operations
- Dual-write logic: write to Primary, try Secondary
- Error handling: catch, log, trả về meaningful response
- Repository method naming: `findBy*`, `deleteBy*`

### Frontend (Next.js)
- API calls trong `useEffect` hook
- Error state management với useState
- Loading states: disable buttons, show spinners
- Form validation trước submit
- Responsive design cho mobile/tablet/desktop

### Cloud Configuration
- Always use SSL/TLS (`sslmode=require`)
- Store credentials trong environment variables
- Test failover scenario regularly
- Monitor database logs từ Render & Railway

## 📚 Tài liệu quan trọng

- `demo/README.md` - Backend setup & API guide
- `student-frontend/README.md` - Frontend setup
- `demo/RAILWAY_SETUP.md` - Railway configuration
- `demo/DEBUG.md` - Debugging guide (nếu có)

## 🚀 Lệnh Startup

```bash
# Terminal 1: Backend
cd demo
./mvnw spring-boot:run

# Terminal 2: Frontend
cd student-frontend
npm install
npm run dev

# Application đã sẵn sàng
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080/api/students
```

## 💬 Gợi ý cho Agent khi làm việc

Khi bắt đầu task, agent sẽ:
1. **Đọc requirements** kỹ lưỡng
2. **Xác định files cần sửa** (backend, frontend, hoặc cả 2)
3. **Hiểu flow dữ liệu**: Model → Service → Controller → API → UI
4. **Test changes** trước khi hoàn thành
5. **Verify cloud connectivity** nếu liên quan database

---

**Khởi động**: Gõ `/fullstack-cloud` hoặc yêu cầu help về dự án này để được hỗ trợ.
