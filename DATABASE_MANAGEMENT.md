# 🛠️ DATABASE MANAGEMENT GUIDE

## 📋 Tổng Quan

Hệ thống hiện đã có các công cụ để quản lý database:

1. **🧹 Reset Database** - Xóa toàn bộ dữ liệu (giữ bảng & cấu trúc)
2. **🔍 Verify Auto-Increment** - Kiểm tra tự tăng ID trên cả 3 bảng
3. **🔄 Check Failover** - Kiểm tra DB phụ có thể thay thế DB chính không
4. **📊 Database Stats** - Xem thống kê dữ liệu hiện tại

---

## 🚀 API ENDPOINTS

### 1️⃣ RESET DATABASE

#### 🧹 Xóa TẤT CẢ dữ liệu ở cả 2 DB
```bash
curl -X POST http://localhost:8080/api/admin/reset-all
```

**Response (Thành công):**
```json
{
  "success": true,
  "message": "✅ Đã xóa tất cả dữ liệu ở cả 2 database",
  "timestamp": 1683619200000
}
```

**Khi dùng:**
- Gặp sự cố xung khắc dữ liệu giữa 2 DB
- Muốn khởi động lại hệ thống từ đầu
- Xóa hết test data

#### 🧹 Xóa dữ liệu ở DB CHÍNH (Railway)
```bash
curl -X POST http://localhost:8080/api/admin/reset-primary
```

#### 🧹 Xóa dữ liệu ở DB PHỤ (Neon)
```bash
curl -X POST http://localhost:8080/api/admin/reset-secondary
```

---

### 2️⃣ KIỂM TRA AUTO-INCREMENT

```bash
curl -X GET http://localhost:8080/api/admin/verify-autoincrement
```

**Response:**
```json
{
  "success": true,
  "message": "✅ Đã kiểm tra auto-increment. Xem logs chi tiết.",
  "tables": ["students", "grades", "users"]
}
```

**Log sẽ in ra:**
```
🔍 Kiểm tra auto-increment ở cả 2 database...
--- PRIMARY DATABASE ---
✅ Students: last_value=10, increment_by=1
✅ Grades: last_value=5, increment_by=1
✅ Users: last_value=3, increment_by=1

--- SECONDARY DATABASE ---
✅ Students: last_value=10, increment_by=1
✅ Grades: last_value=5, increment_by=1
✅ Users: last_value=3, increment_by=1
```

**Điều kiện OK:**
- Cả 2 DB có `last_value > 0`
- `increment_by = 1`
- Giá trị `last_value` giống nhau giữa 2 DB

---

### 3️⃣ KIỂM TRA FAILOVER

```bash
curl -X GET http://localhost:8080/api/admin/check-failover
```

**Response (Cả 2 DB bình thường):**
```json
{
  "primary_database": {
    "healthy": true,
    "status": "✅ OK"
  },
  "secondary_database": {
    "healthy": true,
    "status": "✅ OK"
  },
  "failover_status": "✅ Cả 2 DB khỏe. Failover sẵn sàng.",
  "success": true,
  "timestamp": 1683619200000
}
```

**Response (DB chính DOWN):**
```json
{
  "primary_database": {
    "healthy": false,
    "status": "❌ DOWN"
  },
  "secondary_database": {
    "healthy": true,
    "status": "✅ OK"
  },
  "failover_status": "⚠️  DB chính DOWN. Nên chuyển sang DB phụ!",
  "success": true,
  "timestamp": 1683619200000
}
```

**Giải thích status:**
- `✅ Cả 2 DB khỏe` → Failover sẵn sàng, có thể chuyển nếu cần
- `⚠️ DB chính DOWN` → Nên kích hoạt failover, dùng DB phụ
- `⚠️ DB phụ DOWN` → Cần khôi phục DB phụ
- `❌ Cả 2 DB DOWN` → TÌNH TRẠNG NGUY HIỂM, không thể hoạt động

---

### 4️⃣ XEM THỐNG KÊ DATABASE

```bash
curl -X GET http://localhost:8080/api/admin/database-stats
```

**Log sẽ in ra:**
```
📊 [PRIMARY] Students: 5, Grades: 12, Users: 2
📊 [SECONDARY] Students: 5, Grades: 12, Users: 2
```

---

## 💡 CÁC TÌNH HUỐNG SỬ DỤNG

### Tình huống 1: 🔴 Dữ liệu xung khắc giữa 2 DB

**Triệu chứng:**
- DB chính có 10 student, DB phụ có 8 student
- Dữ liệu không đồng bộ

**Giải pháp:**
```bash
# Bước 1: Reset tất cả dữ liệu
curl -X POST http://localhost:8080/api/admin/reset-all

# Bước 2: Kiểm tra đã trống chưa
curl -X GET http://localhost:8080/api/admin/database-stats

# Bước 3: Khởi động lại backend để sync lại từ đầu
# Restart application...

# Bước 4: Kiểm tra auto-increment
curl -X GET http://localhost:8080/api/admin/verify-autoincrement
```

---

### Tình huống 2: 🟡 DB chính bị sập

**Triệu chứng:**
- Không thể kết nối DB chính (Railway)
- Cần dùng DB phụ (Neon) tạm thời

**Giải pháp:**
```bash
# Bước 1: Kiểm tra failover status
curl -X GET http://localhost:8080/api/admin/check-failover

# Kết quả:
# "failover_status": "⚠️ DB chính DOWN. Nên chuyển sang DB phụ!"

# Bước 2: Tìm hiểu tình trạng DB phụ
curl -X GET http://localhost:8080/api/admin/database-stats

# Bước 3: Tạm chuyển các yêu cầu sang DB phụ (manual process)
# - Cập nhật application.properties để swapped datasources
# - Hoặc dùng environment variables để override
```

---

### Tình huống 3: ⚠️ Khởi động lại hệ thống

**Process:**
```bash
# Bước 1: Reset nếu cần
curl -X POST http://localhost:8080/api/admin/reset-all

# Bước 2: Khởi động backend
./mvnw spring-boot:run

# Bước 3: Backend sẽ tự:
# - Kết nối cả 2 DB
# - Tạo bảng (nếu chưa có)
# - Kiểm tra auto-increment
# - Sẵn sàng phục vụ request

# Bước 4: Kiểm tra lại
curl -X GET http://localhost:8080/api/admin/check-failover
curl -X GET http://localhost:8080/api/admin/verify-autoincrement
```

---

## 📝 LƯU Ý QUAN TRỌNG

### ✅ Auto-Increment được tự động reset

Khi chạy `reset-all` hoặc `reset-primary` / `reset-secondary`:
- ✅ Bảng `students` sequence reset về 1
- ✅ Bảng `grades` sequence reset về 1
- ✅ Bảng `users` sequence reset về 1

Điều này đảm bảo không có xung đột ID khi tạo dữ liệu mới.

### ✅ Foreign Key constraints được xử lý

Khi xóa dữ liệu:
- Foreign key constraints bị tắt tạm thời
- Xóa các bảng theo thứ tự: `grades` → `users` → `students`
- Foreign key constraints được bật lại

Điều này tránh lỗi "violates foreign key constraint".

### ✅ Bảng structure được giữ nguyên

**KHÔNG** xóa bảng, chỉ xóa **dữ liệu**:
- ✅ Giữ cột (columns)
- ✅ Giữ index
- ✅ Giữ constraints
- ✅ Giữ triggers (nếu có)

---

## 🔧 TẤT CẢ 3 BẢNG ĐÃ CÓ AUTO-INCREMENT

| Bảng | ID Column | Auto-Increment | Sequence |
|------|-----------|----------------|----------|
| students | id | BIGSERIAL | students_id_seq |
| grades | id | SERIAL | grades_id_seq |
| users | id | SERIAL | users_id_seq |

**Cả 3 bảng ở cả 2 database (Primary + Secondary) đều cấu hình tương tự.**

---

## 📞 DEBUG TIPS

Nếu gặp vấn đề, kiểm tra:

1. **Logs của Spring Boot** - Tìm `[RESET]` hoặc `[ADMIN API]`
2. **Endpoint `/api/admin/database-stats`** - Xem dữ liệu hiện tại
3. **Endpoint `/api/admin/check-failover`** - Xem health status
4. **PostgreSQL logs** - Kiểm tra lỗi từ database level

---

**Tạo bởi:** Database Management System  
**Ngày:** 2026-05-11
