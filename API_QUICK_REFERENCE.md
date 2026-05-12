# 📱 QUICK API REFERENCE - Database Management

Tất cả các API endpoint dành cho admin tasks

---

## 🧹 RESET DATABASE ENDPOINTS

### 1️⃣ Reset Tất Cả Dữ Liệu (cả 2 DB)
```bash
curl -X POST http://localhost:8080/api/admin/reset-all
```

### 2️⃣ Reset Dữ Liệu DB Chính (Railway)
```bash
curl -X POST http://localhost:8080/api/admin/reset-primary
```

### 3️⃣ Reset Dữ Liệu DB Phụ (Neon)
```bash
curl -X POST http://localhost:8080/api/admin/reset-secondary
```

---

## 🔍 VERIFICATION ENDPOINTS

### 4️⃣ Kiểm Tra Auto-Increment
```bash
curl -X GET http://localhost:8080/api/admin/verify-autoincrement
```

**Thông tin trả về:**
- `last_value`: Giá trị hiện tại của sequence
- `increment_by`: Bước nhảy (thường = 1)

### 5️⃣ Xem Thống Kê Dữ Liệu
```bash
curl -X GET http://localhost:8080/api/admin/database-stats
```

**Thông tin trả về (logs):**
```
📊 [PRIMARY] Students: X, Grades: Y, Users: Z
📊 [SECONDARY] Students: X, Grades: Y, Users: Z
```

### 6️⃣ Kiểm Tra Failover Status
```bash
curl -X GET http://localhost:8080/api/admin/check-failover
```

**Response JSON:**
```json
{
  "primary_database": {
    "healthy": true/false,
    "status": "✅ OK" hoặc "❌ DOWN"
  },
  "secondary_database": {
    "healthy": true/false,
    "status": "✅ OK" hoặc "❌ DOWN"
  },
  "failover_status": "Mô tả tình trạng"
}
```

---

## 📚 STUDENT MANAGEMENT ENDPOINTS

### 7️⃣ Lấy Tất Cả Sinh Viên
```bash
curl -X GET http://localhost:8080/api/students
```

### 8️⃣ Tạo Sinh Viên Mới
```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nguyen Van A",
    "email": "a@example.com",
    "phone": "0123456789",
    "age": 20
  }'
```

### 9️⃣ Cập Nhật Sinh Viên
```bash
curl -X PUT http://localhost:8080/api/students/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nguyen Van A Updated",
    "email": "a-new@example.com",
    "phone": "0987654321",
    "age": 21
  }'
```

### 🔟 Xóa Sinh Viên
```bash
curl -X DELETE http://localhost:8080/api/students/1
```

---

## 🎓 GRADE MANAGEMENT ENDPOINTS

### 1️⃣1️⃣ Lấy Tất Cả Điểm
```bash
curl -X GET http://localhost:8080/api/grades
```

### 1️⃣2️⃣ Tạo Điểm Mới
```bash
curl -X POST http://localhost:8080/api/grades \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "math": 8.5,
    "literature": 7.5,
    "english": 8.0
  }'
```

### 1️⃣3️⃣ Lấy Điểm theo Sinh Viên
```bash
curl -X GET http://localhost:8080/api/grades/by-student/1
```

### 1️⃣4️⃣ Cập Nhật Điểm
```bash
curl -X PUT http://localhost:8080/api/grades/1 \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "math": 9.0,
    "literature": 8.0,
    "english": 8.5
  }'
```

### 1️⃣5️⃣ Xóa Điểm
```bash
curl -X DELETE http://localhost:8080/api/grades/1
```

---

## 🔐 USER MANAGEMENT ENDPOINTS

### 1️⃣6️⃣ Đăng Ký Người Dùng Mới
```bash
curl -X POST http://localhost:8080n \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "password": "password123",
    "role": "ROLE_USER"
  }'
```

### 1️⃣7️⃣ Đăng Nhập
```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "password": "password123"
  }'
```

---

## ⚡ COMMON WORKFLOWS

### Workflow 1: Khởi Động Lại Hệ Thống
```bash
# 1. Reset tất cả dữ liệu
curl -X POST http://localhost:8080/api/admin/reset-all

# 2. Kiểm tra auto-increment đã reset
curl -X GET http://localhost:8080/api/admin/verify-autoincrement

# 3. Kiểm tra dữ liệu đã trống
curl -X GET http://localhost:8080/api/admin/database-stats

# 4. Kiểm tra failover sẵn sàng
curl -X GET http://localhost:8080/api/admin/check-failover
```

### Workflow 2: Xử Lý Xung Khắc Dữ Liệu
```bash
# 1. Kiểm tra tình trạng hiện tại
curl -X GET http://localhost:8080/api/admin/database-stats

# 2. Kiểm tra tính nhất quán (consistency)
curl -X GET http://localhost:8080/api/admin/check-failover

# 3. Nếu xung khắc → reset tất cả
curl -X POST http://localhost:8080/api/admin/reset-all

# 4. Khởi động backend lại
# ./mvnw spring-boot:run

# 5. Tạo lại dữ liệu từ đầu
curl -X POST http://localhost:8080/api/students ...
```

### Workflow 3: Kiểm Tra Failover Ready
```bash
# Chạy định kỳ để đảm bảo failover sẵn sàng
curl -X GET http://localhost:8080/api/admin/check-failover

# Nếu một DB DOWN → kiểm tra thông tin
curl -X GET http://localhost:8080/api/admin/database-stats

# Khôi phục DB bị sập
# (tùy thuộc vào platform: Railway hoặc Neon)
```

---

## 💡 TIPS

### Tip 1: Sử dụng `jq` để format JSON
```bash
curl -s -X GET http://localhost:8080/api/admin/check-failover | jq .
```

### Tip 2: Kiểm tra response headers
```bash
curl -i -X POST http://localhost:8080/api/admin/reset-all
```

### Tip 3: Lưu response vào file
```bash
curl -X GET http://localhost:8080/api/students > students.json
```

### Tip 4: Với timeout lớn cho các query lâu
```bash
curl --max-time 30 -X GET http://localhost:8080/api/admin/database-stats
```

---

## 🚨 HTTP STATUS CODES

| Status | Ý Nghĩa |
|--------|---------|
| 200 | ✅ OK - Thành công |
| 201 | ✅ Created - Tạo mới thành công |
| 204 | ✅ No Content - Xóa thành công |
| 400 | ❌ Bad Request - Lỗi input |
| 404 | ❌ Not Found - Không tìm thấy |
| 500 | ❌ Server Error - Lỗi server |

---

## 📞 NEED HELP?

1. **Kiểm tra logs:**
   ```bash
   grep "[ADMIN API]" logs/spring.log
   ```

2. **Kiểm tra database connectivity:**
   ```bash
   curl -X GET http://localhost:8080/api/admin/check-failover
   ```

3. **Reset mọi thứ:**
   ```bash
   curl -X POST http://localhost:8080/api/admin/reset-all
   ```

---

**Last Updated:** 2026-05-11  
**Version:** 1.0  
**Status:** Production Ready ✅
