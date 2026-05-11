# 🧪 TESTING GUIDE - Database Management Features

## 🎯 Mục đích
Hướng dẫn test tất cả các tính năng mới được thêm vào:
1. ✅ **Auto-Increment** trên cả 3 bảng
2. ✅ **Database Reset** (xóa dữ liệu, giữ bảng)
3. ✅ **Failover** (DB phụ thay thế DB chính)

---

## 🚀 SETUP

### Bước 1: Khởi động Backend
```bash
cd d:\VS\DTDM\demo
./mvnw spring-boot:run
```

Backend sẽ:
- Kết nối cả 2 database
- Tạo 3 bảng (students, grades, users)
- In log về auto-increment configuration
- Sẵn sàng tại `http://localhost:8080`

### Bước 2: Kiểm tra khởi động
Tìm trong logs các dòng:
```
✅ Students table created (auto-increment: id BIGSERIAL)
✅ Users table created (auto-increment: id SERIAL)
✅ Grades table created (auto-increment: id SERIAL) with FK to students
✅ Secondary Students table created (auto-increment: id BIGSERIAL)
✅ Secondary Users table created (auto-increment: id SERIAL)
✅ Secondary Grades table created (auto-increment: id SERIAL)
```

---

## 📝 TEST 1: AUTO-INCREMENT CONFIGURATION

### Test 1.1: Verify Auto-Increment via API

```bash
curl -X GET http://localhost:8080/api/admin/verify-autoincrement
```

**Expected Output (in logs):**
```
🔍 Kiểm tra auto-increment ở cả 2 database...

--- PRIMARY DATABASE ---
✅ Students: last_value=1, increment_by=1
✅ Grades: last_value=1, increment_by=1
✅ Users: last_value=1, increment_by=1

--- SECONDARY DATABASE ---
✅ Students: last_value=1, increment_by=1
✅ Grades: last_value=1, increment_by=1
✅ Users: last_value=1, increment_by=1
```

**Check:**
- ✅ Cả 3 bảng có `last_value >= 1`
- ✅ Cả 3 bảng có `increment_by = 1`
- ✅ Giá trị giống nhau giữa PRIMARY và SECONDARY

---

### Test 1.2: Create Students and Verify ID Auto-Increment

**Create 3 students:**
```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Nguyen A","email":"a@test.com","age":20,"phone":"0123456789"}'

curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Tran B","email":"b@test.com","age":21,"phone":"0987654321"}'

curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Hoang C","email":"c@test.com","age":22,"phone":"0123456788"}'
```

**Get all students:**
```bash
curl -X GET http://localhost:8080/api/students
```

**Expected:**
```json
[
  {"id": 1, "name": "Nguyen A", "email": "a@test.com", ...},
  {"id": 2, "name": "Tran B", "email": "b@test.com", ...},
  {"id": 3, "name": "Hoang C", "email": "c@test.com", ...}
]
```

**Check:**
- ✅ ID là 1, 2, 3 (tự động tăng)
- ✅ Không cần set ID khi create
- ✅ ID liên tục, không bị lỗi

---

### Test 1.3: Add Grades and Verify ID Auto-Increment

**Add grades for student 1:**
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

**Get grades for student 1:**
```bash
curl -X GET http://localhost:8080/api/grades/by-student/1
```

**Expected:**
```json
{
  "id": 1,
  "studentId": 1,
  "math": 8.5,
  "literature": 7.5,
  "english": 8.0,
  "total": 8.0
}
```

**Check:**
- ✅ Grade ID tự động là 1
- ✅ Không lỗi FK constraint
- ✅ Student ID khớp

---

## 🧹 TEST 2: DATABASE RESET FUNCTIONALITY

### Test 2.1: Check Current Data

```bash
curl -X GET http://localhost:8080/api/admin/database-stats
```

**Expected (in logs):**
```
📊 [PRIMARY] Students: 3, Grades: 1, Users: 0
📊 [SECONDARY] Students: 3, Grades: 1, Users: 0
```

---

### Test 2.2: Reset All Data

```bash
curl -X POST http://localhost:8080/api/admin/reset-all
```

**Expected Response:**
```json
{
  "success": true,
  "message": "✅ Đã xóa tất cả dữ liệu ở cả 2 database",
  "timestamp": 1683619200000
}
```

**Expected (in logs):**
```
🧹 [RESET] Bắt đầu xóa tất cả dữ liệu ở cả 2 database...
🧹 [RESET PRIMARY] Bắt đầu xóa dữ liệu ở DB chính (Railway)...
✅ [RESET PRIMARY] Đã xóa dữ liệu ở DB chính + reset sequences!
🧹 [RESET SECONDARY] Bắt đầu xóa dữ liệu ở DB phụ (Neon)...
✅ [RESET SECONDARY] Đã xóa dữ liệu ở DB phụ + reset sequences!
✅ [RESET] Đã xóa tất cả dữ liệu ở cả 2 database thành công!
```

---

### Test 2.3: Verify Data is Empty

```bash
curl -X GET http://localhost:8080/api/admin/database-stats
```

**Expected (in logs):**
```
📊 [PRIMARY] Students: 0, Grades: 0, Users: 0
📊 [SECONDARY] Students: 0, Grades: 0, Users: 0
```

---

### Test 2.4: Verify Auto-Increment Reset

```bash
curl -X GET http://localhost:8080/api/admin/verify-autoincrement
```

**Expected (in logs):**
```
--- PRIMARY DATABASE ---
✅ Students: last_value=1, increment_by=1
✅ Grades: last_value=1, increment_by=1
✅ Users: last_value=1, increment_by=1

--- SECONDARY DATABASE ---
✅ Students: last_value=1, increment_by=1
✅ Grades: last_value=1, increment_by=1
✅ Users: last_value=1, increment_by=1
```

**Check:**
- ✅ Auto-increment reset về 1
- ✅ Sẵn sàng tạo dữ liệu mới từ ID 1

---

### Test 2.5: Create Data Again After Reset

```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"New Student","email":"new@test.com","age":20,"phone":"0123456789"}'

curl -X GET http://localhost:8080/api/students
```

**Expected:**
```json
[
  {"id": 1, "name": "New Student", "email": "new@test.com", ...}
]
```

**Check:**
- ✅ ID bắt đầu từ 1 lại
- ✅ Không xung đột ID

---

## 🔄 TEST 3: FAILOVER FUNCTIONALITY

### Test 3.1: Check Both Databases Healthy

```bash
curl -X GET http://localhost:8080/api/admin/check-failover
```

**Expected Response:**
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

**Check:**
- ✅ Cả 2 database `healthy = true`
- ✅ Failover sẵn sàng

---

### Test 3.2: Simulate Primary Database Down (Manual Test)

**Option 1: Stop primary database manually**
- Tắt service Railway database
- Hoặc chặn kết nối firewall

```bash
# Sau khi tắt primary DB, kiểm tra failover:
curl -X GET http://localhost:8080/api/admin/check-failover
```

**Expected Response:**
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

**Check:**
- ✅ Primary `healthy = false`
- ✅ Secondary `healthy = true`
- ✅ Failover status warn về cần chuyển

---

### Test 3.3: Recover Primary and Check Again

**Khôi phục primary database:**
```bash
# Bật lại service Railway database
# Hoặc remove firewall rule

# Kiểm tra lại failover:
curl -X GET http://localhost:8080/api/admin/check-failover
```

**Expected:**
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
  "success": true
}
```

**Check:**
- ✅ Cả 2 database khỏe lại
- ✅ Failover sẵn sàng

---

## ✅ CHECKLISTS

### ✅ Hoàn thành TEST 1 (Auto-Increment)
- [ ] API `/api/admin/verify-autoincrement` trả về đúng thông tin
- [ ] Tạo student mới → ID tự động tăng (1, 2, 3...)
- [ ] Tạo grade mới → ID tự động tăng (1, 2, 3...)
- [ ] Reset toàn bộ → Auto-increment reset về 1
- [ ] Tạo dữ liệu mới sau reset → ID lại từ 1

### ✅ Hoàn thành TEST 2 (Reset Database)
- [ ] API `/api/admin/reset-all` xóa tất cả dữ liệu
- [ ] API `/api/admin/reset-primary` xóa dữ liệu ở Primary
- [ ] API `/api/admin/reset-secondary` xóa dữ liệu ở Secondary
- [ ] `/api/admin/database-stats` show dữ liệu trống
- [ ] Bảng structure không bị xóa (tạo dữ liệu mới được)

### ✅ Hoàn thành TEST 3 (Failover)
- [ ] `/api/admin/check-failover` show cả 2 DB healthy
- [ ] Tắt primary → failover show primary DOWN
- [ ] Secondary vẫn khỏe khi primary tắt
- [ ] Bật primary → failover show cả 2 khỏe

---

## 🐛 DEBUG TIPS

### Nếu Auto-Increment không hoạt động:
```bash
# Kiểm tra sequence trong PostgreSQL:
psql -U neondb_owner -h ... -c "SELECT * FROM students_id_seq"

# Có thể reset manually:
psql -U postgres -h ... -c "ALTER SEQUENCE students_id_seq RESTART WITH 1"
```

### Nếu Reset không thành công:
```bash
# Kiểm tra logs cho lỗi:
grep "[RESET]" logs/spring.log

# Kiểm tra Foreign Key constraints:
curl -X GET http://localhost:8080/api/admin/database-stats
```

### Nếu Failover không detect chính xác:
```bash
# Kiểm tra database connectivity:
psql -U postgres -h <primary-host> -c "SELECT 1"
psql -U neondb_owner -h <secondary-host> -c "SELECT 1"

# Kiểm tra network connectivity:
ping <primary-host>
ping <secondary-host>
```

---

**Test Date:** 2026-05-11  
**Status:** Ready for Testing  
**All 3 Features:** Auto-Increment ✅ | Reset ✅ | Failover ✅
