# 📚 Student Backend API - Spring Boot

REST API Backend cho ứng dụng **quản lý sinh viên** với kiến trúc **dual-database** trên cloud (Render + Railway) đảm bảo high availability và zero downtime.

**Stack:** Spring Boot 3.5 + PostgreSQL + Docker

---

## 🎯 Phần 1: Công dụng chi tiết Backend

### 🔑 Chức năng chính

Backend cung cấp các chức năng sau để quản lý dữ liệu sinh viên:

#### 1. **CRUD Operations - Quản lý Sinh Viên**
   - **GET /api/students** 
     - Lấy danh sách toàn bộ sinh viên từ database
     - Response: `List<Student>`
   
   - **GET /api/students/{id}**
     - Lấy thông tin chi tiết một sinh viên theo ID
     - Response: `Student` hoặc 404 nếu không tìm thấy
   
   - **POST /api/students**
     - Thêm sinh viên mới
     - Request body: `{ "name": "", "email": "", "phone": "", "age": 0 }`
     - Tự động đồng bộ sang database secondary (Railway)
   
   - **PUT /api/students/{id}**
     - Cập nhật thông tin sinh viên
     - Request body: Dữ liệu cần cập nhật
     - Dual-write: cập nhật cả Render và Railway
   
   - **DELETE /api/students/{id}**
     - Xóa sinh viên khỏi database
     - Xóa ở cả 2 database (Render + Railway)

#### 2. **Health Check & Monitor**
   - **GET /api/students/health/status**
     - Kiểm tra sức khỏe của backend và database connections
     - Response: `{ "status": "UP/DOWN", "timestamp": 123456 }`
     - Dùng để monitor từ Kubernetes, Docker, hoặc monitoring tools
   
   - **GET /api/students/health/ping**
     - Simple health check cho container orchestration (quá nhanh, không kiểm tra DB)
     - Response: `✅ Service is running`

#### 3. **Admin & Maintenance Endpoints**
   - **POST /api/students/admin/sync**
     - Đồng bộ dữ liệu từ Primary DB (Render) sang Secondary DB (Railway)
     - Dùng cho emergency sync hoặc disaster recovery
     - Response: `{ "message": "Sync completed", "timestamp": 123456 }`
   
   - **GET /api/students/debug/db-info**
     - Debug endpoint: kiểm tra kết nối database và số lượng records
     - Hiển thị: JPA count, direct JDBC count, database status
     - Response: `{ "jpa_count": 5, "direct_jdbc_count": 5, "primary_db_status": "connected" }`

### 🔄 Dual-Database Architecture (Render + Railway)

| Thành phần | Vai trò | Mô tả |
|-----------|--------|-------|
| **Render PostgreSQL** | Primary DB | Database chính - tất cả read/write mặc định đi đây |
| **Railway PostgreSQL** | Secondary DB | Database dự phòng - tự động sync & failover |
| **Dual-Write Pattern** | Write Logic | Mỗi write operation ghi vào cả 2 database |
| **Automatic Failover** | Failover | Nếu Render down, tự động fallback sang Railway (read-only) |
| **Auto Sync** | Sync Logic | Mỗi 5 giây kiểm tra sync status, tự động đồng bộ nếu có sự khác biệt |

### ⚙️ Async & Non-Blocking Operations

- **Initialization**: Khởi tạo secondary table + sync dữ liệu chạy **async** → không block app startup
- **Sync Operations**: Tất cả sync operations chạy async → request không phải chờ
- **Health Checks**: Chạy scheduled mỗi 5 giây (background)
- **Lợi ích**: App startup nhanh, request không bị delay

---

## 📂 Phần 2: Cấu trúc File và Công dụng từng File

### Project Structure

```
demo/
├── src/main/java/com/example/demo/
│   ├── DemoApplication.java
│   │   └── 🚀 ENTRY POINT: Khởi động Spring Boot app
│   │          - Quét @Component, @Service, @Controller
│   │          - Khởi tạo DataSourceConfig (Primary + Secondary DB)
│   │          - Khởi tạo StudentService (@PostConstruct)
│   │          - Chạy Tomcat server trên port 8080
│   │
│   └── studentbackend/
│       ├── config/
│       │   ├── DataSourceConfig.java
│       │   │   └── ⚙️ CẤU HÌNH DATABASE + ASYNC:
│       │   │      - @Bean primaryDatasource() → Render (Primary)
│       │   │      - @Bean secondaryDataSource() → Railway (Secondary)
│       │   │      - @Bean primaryJdbc & secondaryJdbc → JdbcTemplate
│       │   │      - @Bean taskExecutor() → ThreadPool (2-5 threads)
│       │   │      - @EnableAsync + @EnableScheduling
│       │   │
│       │   └── CorsConfig.java
│       │       └── 🔧 CORS CONFIGURATION:
│       │          - Cho phép frontend từ mọi domain gọi API
│       │          - allowedOrigins: "*"
│       │          - allowedMethods: GET, POST, PUT, DELETE, PATCH, OPTIONS
│       │
│       ├── model/
│       │   └── Student.java
│       │       └── 📊 ENTITY (JPA):
│       │          - @Entity @Table(name = "students")
│       │          - Fields: id, name, email, phone, age
│       │          - @Data (Lombok): tự generate getter/setter
│       │          - ánh xạ từ Java Object → SQL Table
│       │
│       ├── repository/
│       │   └── StudentRepository.java
│       │       └── 🏦 JPA REPOSITORY:
│       │          - Extends JpaRepository<Student, Long>
│       │          - Tự động hỗ trợ:
│       │            • findAll() → SELECT *
│       │            • findById(id) → SELECT WHERE id = ?
│       │            • save(student) → INSERT/UPDATE
│       │            • deleteById(id) → DELETE
│       │          - Chỉ dùng cho Primary DB (Render)
│       │
│       ├── service/
│       │   └── StudentService.java
│       │       └── 🧠 BUSINESS LOGIC (Dual-DB):
│       │          1️⃣  @PostConstruct initializeSecondaryDb():
│       │             - Tạo students table trên Railway
│       │             - Sync dữ liệu từ Render → Railway (async)
│       │          
│       │          2️⃣  READ Operations (Smart Routing):
│       │             - getAllStudents(): đọc Render → fallback Railway
│       │             - getStudentById(id): với smart failover
│       │             - fallbackToSecondary(): query Railway khi Render down
│       │          
│       │          3️⃣  WRITE Operations (Dual-Write):
│       │             - createStudent(): write Render (sync) + write Railway (async)
│       │             - updateStudent(): update cả 2 DB
│       │             - deleteStudent(): xóa cả 2 DB
│       │             - createStudentInSecondary(): failover khi Render down
│       │          
│       │          4️⃣  HEALTH CHECK (mỗi 5 giây):
│       │             - checkPrimaryDbHealth(): ping Render
│       │             - checkSecondaryDbHealth(): ping Railway
│       │             - primaryDbHealthy, secondaryDbHealthy (flags)
│       │          
│       │          5️⃣  ADMIN ENDPOINTS:
│       │             - manualSync(): đồng bộ thủ công
│       │             - resetSecondaryDb(): xóa Railway, sync lại
│       │             - checkDataConsistency(): kiểm tra dữ liệu
│       │          
│       │          6️⃣  ASYNC OPERATIONS:
│       │             - syncToSecondaryAsync(): chạy background
│       │             - writeToSecondaryAsync(): write Railway không block
│       │
│       ├── controller/
│       │   └── StudentController.java
│       │       └── 🎛️ REST API ENDPOINTS:
│       │          @RestController @RequestMapping("/api/students")
│       │          
│       │          📍 CRUD Endpoints:
│       │          ├─ GET    /api/students → getAllStudents()
│       │          ├─ GET    /api/students/{id} → getStudentById(id)
│       │          ├─ POST   /api/students → createStudent()
│       │          ├─ PUT    /api/students/{id} → updateStudent()
│       │          └─ DELETE /api/students/{id} → deleteStudent()
│       │          
│       │          🏥 Health Check Endpoints:
│       │          ├─ GET    /api/students/health/status → trạng thái chi tiết
│       │          ├─ GET    /api/students/health/ping → simple ping
│       │          └─ GET    /api/students/health/detailed → detailed health + consistency
│       │          
│       │          🔧 Admin Endpoints:
│       │          ├─ POST   /api/students/admin/sync → manual sync
│       │          ├─ POST   /api/students/admin/reset-secondary → reset Railway
│       │          ├─ GET    /api/students/admin/consistency-check → check dữ liệu
│       │          └─ GET    /api/students/debug/db-info → debug info
│       │          
│       │          @CrossOrigin(origins = "*"): cho phép tất cả origins
│       │
│       └── util/
│           └── DiagnosticService.java
│               └── 🔍 CHẨN ĐOÁN:
│                  - runFullDiagnostics(): report toàn diện
│                  - testPrimaryDatabase(): kiểm tra Render
│                  - testSecondaryDatabase(): kiểm tra Railway
│                  - checkTableStatus(): kiểm tra table
│                  - verifyData(): xác minh dữ liệu
│                  - generateRecommendations(): gợi ý sửa lỗi
│
├── src/main/resources/
│   ├── application.properties
│   │   └── 🔐 CONFIG DATABASE + JPA:
│   │      • spring.datasource.url (Render URL)
│   │      • spring.datasource.username/password
│   │      • secondary.datasource.* (Railway URL + credentials)
│   │      • spring.jpa.show-sql=true (log SQL)
│   │      • spring.jpa.hibernate.ddl-auto=update (auto create table)
│   │      • logging.level.* (log levels)
│   │      • server.port=8080
│   │
│   ├── application.yml (YAML alternative format)
│   ├── static/ (CSS, JS static files)
│   └── templates/ (Thymeleaf templates)
│
├── src/test/java/
│   └── DemoApplicationTests.java
│       └── 🧪 Unit tests (tạm thời empty)
│
├── pom.xml
│   └── 📦 MAVEN CONFIG:
│      • spring-boot-starter-web
│      • spring-boot-starter-data-jpa
│      • spring-boot-starter-validation
│      • postgresql (JDBC driver)
│      • lombok (code generator)
│      • maven plugins (build, shade, etc.)
│
├── Dockerfile
│   └── 🐳 DOCKER BUILD:
│      • Stage 1: maven:3.9-eclipse-temurin-17 (build)
│      • Stage 2: eclipse-temurin:17-jre (runtime)
│      • Expose port 8080
│      • CMD: java -jar app.jar
│
├── mvnw & mvnw.cmd
│   └── 📜 MAVEN WRAPPER:
│      • Cho phép chạy Maven mà không cần cài globally
│      • Linux/Mac: ./mvnw
│      • Windows: .\mvnw.cmd
│
└── README.md
    └── 📖 Tài liệu project này
```

### 📄 Chi tiết File quan trọng - Debugging Guide

#### **1. `DemoApplication.java` 🚀 (Entry Point)**

**Công dụng**: Khởi động toàn bộ Spring Boot application

**Luồng khởi động**:
```
1. main() gọi SpringApplication.run()
   ↓
2. Spring Boot quét classpath, tìm @Configuration, @Component, @Service
   ↓
3. DataSourceConfig được load:
   - Tạo Primary DataSource (Render PostgreSQL)
   - Tạo Secondary DataSource (Railway PostgreSQL)
   - Tạo ThreadPool cho @Async
   ↓
4. StudentService được load:
   - @PostConstruct initializeSecondaryDb() chạy
   - Tạo students table trên Railway
   - Sync dữ liệu từ Render → Railway (async)
   ↓
5. StudentController được đăng ký:
   - Các REST endpoints sẵn sàng
   ↓
6. Tomcat WebServer khởi động trên port 8080
   ↓
7. ✅ Application ready to accept requests
```

**Debug**: Nếu app không khởi động, kiểm tra logs:
- `Check1`: Java version (`java -version` → phải Java 17+)
- `Check2`: PostgreSQL driver có trong classpath không?
- `Check3`: DataSourceConfig có được load không? (tìm logs: "Creating/verifying secondary table")

**Chạy**:
```bash
./mvnw spring-boot:run              # Development
java -jar target/demo*.jar          # Production
```

---

#### **2. `DataSourceConfig.java` ⚙️ (Configuration)**

**Công dụng**: Cấu hình kết nối database (Primary + Secondary) + ThreadPool

**Các Bean quan trọng**:

| Bean Name | Dùng Cho | Mô Tả |
|-----------|---------|-------|
| `primaryDatasource` | Spring Data JPA | Kết nối Render PostgreSQL (Primary) |
| `primaryJdbc` | Direct SQL queries | JdbcTemplate cho Render |
| `secondaryDataSource` | Failover/Sync | Kết nối Railway PostgreSQL (Secondary) |
| `secondaryJdbcTemplate` | Dual-write | JdbcTemplate cho Railway |
| `taskExecutor` | @Async tasks | ThreadPool (2-5 threads, queue 100) |

**Debug**:
- Nếu Secondary DB không kết nối: kiểm tra `secondary.datasource.url`, username, password trong `application.properties`
- Nếu threads không chạy: kiểm tra `@EnableAsync`, `@EnableScheduling` có được kích hoạt không?
- Log: tìm "Creating/verifying secondary table" → nếu không có = DataSourceConfig chưa load

---

#### **3. `Student.java` 📊 (Entity Model)**

**Công dụng**: Ánh xạ Java Object ↔ SQL Table

**Database Schema**:
```sql
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,      -- tự tăng
    name VARCHAR(255),              -- Họ tên
    email VARCHAR(255),             -- Email
    phone VARCHAR(255),             -- Điện thoại
    age INTEGER                      -- Tuổi
);
```

**Annotations**:
- `@Entity`: đánh dấu đây là JPA entity
- `@Table(name="students")`: tên table trong DB
- `@Id @GeneratedValue(IDENTITY)`: Primary Key, tự tăng
- `@Data`: Lombok → auto generate getter/setter/toString

**Debug**:
- Nếu table không được tạo: kiểm tra `spring.jpa.hibernate.ddl-auto=update` trong `application.properties`
- Nếu data lưu nhưng không có trong DB: kiểm tra transaction commit (xem logs)

---

#### **4. `StudentRepository.java` 🏦 (Data Access Layer)**

**Công dụng**: Giao tiếp với Primary Database (Render)

**Methods tự động có**:
```java
findAll()              // SELECT * FROM students
findById(Long id)      // SELECT * FROM students WHERE id = ?
save(Student s)        // INSERT/UPDATE
deleteById(Long id)    // DELETE FROM students WHERE id = ?
count()                // SELECT COUNT(*) ...
```

**Debug**:
- Nếu `save()` không lưu: check transaction (@Transactional)
- Nếu `findAll()` trả về empty list: DB có data không?
- Query logs: enable `logging.level.org.hibernate.SQL=DEBUG` trong `application.properties`

---

#### **5. `StudentService.java` 🧠 (Business Logic - QUAN TRỌNG)**

**Công dụng**: Xử lý:
1. Dual-database writes (Render + Railway)
2. Smart failover (Render down → fallback Railway)
3. Automatic health checks (mỗi 5s)
4. Data consistency checks

**Các phần chính**:

**A) Initialization (@PostConstruct)**:
```java
initializeSecondaryDb() 
  ├─ Tạo students table trên Railway (CREATE TABLE IF NOT EXISTS)
  └─ Sync dữ liệu từ Render → Railway (async)
```

**B) Health Checks (@Scheduled fixedDelay=5000)**:
```java
checkPrimaryDbHealth()
  └─ Chạy "SELECT 1" trên Render → update primaryDbHealthy flag

checkSecondaryDbHealth()
  └─ Chạy "SELECT 1" trên Railway → update secondaryDbHealthy flag
```

**C) Read Operations**:
```java
getAllStudents()
  ├─ Nếu primaryDbHealthy=true → đọc từ Render
  └─ Nếu primaryDbHealthy=false → fallback Railway

fallbackToSecondary(sql)
  └─ Query trực tiếp Railway nếu Render down
```

**D) Write Operations**:
```java
createStudent(student)
  ├─ Ghi vào Render (sync) → lấy ID
  ├─ Verify data saved
  └─ Ghi vào Railway (async) → không block request

createStudentInSecondary(student)
  └─ Failover: ghi vào Railway nếu Render down
```

**Debug - Logs cần nhìn**:
```
✅ Secondary table created/verified    → Railway table OK
✅ Found X students in primary DB     → Sync thành công
❌ Primary DB health check failed      → Render down!
❌ Secondary DB write failed           → Railway down!
⚠️  Primary DB read failed, falling back to secondary → Failover active
```

---

#### **6. `StudentController.java` 🎛️ (REST Endpoints)**

**Công dụng**: Xử lý HTTP requests, định nghĩa REST API

**CRUD Endpoints**:
```
GET    /api/students              → lấy all
GET    /api/students/1            → lấy 1
POST   /api/students              → thêm
PUT    /api/students/1            → update
DELETE /api/students/1            → xóa
```

**Health Endpoints**:
```
GET    /api/students/health/status          → trạng thái (UP/DOWN)
GET    /api/students/health/ping            → simple check
GET    /api/students/health/detailed        → status + consistency
```

**Admin Endpoints**:
```
POST   /api/students/admin/sync                      → manual sync
POST   /api/students/admin/reset-secondary          → reset Railway
GET    /api/students/admin/consistency-check        → verify data
GET    /api/students/debug/db-info                  → debug info
```

**Debug - Test APIs**:
```bash
# Kiểm tra service chạy
curl http://localhost:8080/api/students/health/ping

# Xem trạng thái chi tiết
curl http://localhost:8080/api/students/health/status

# Xem debug info
curl http://localhost:8080/api/students/debug/db-info

# Xem danh sách sinh viên
curl http://localhost:8080/api/students

# Manual sync
curl -X POST http://localhost:8080/api/students/admin/sync
```

---

#### **7. `application.properties` 🔐 (Configuration - QUAN TRỌNG)**

**Công dụng**: Cấu hình database URLs, JPA settings, logging

**Primary Database (Render)**:
```properties
spring.datasource.url=jdbc:postgresql://viaduct.proxy.rlwy.net:53388/railway?sslmode=require&TimeZone=UTC
spring.datasource.username=postgres
spring.datasource.password=UroOHCbEVypYZtJsLuhApYbpXRHgBeen
```

**Secondary Database (Railway)**:
```properties
secondary.datasource.url=jdbc:postgresql://ep-lively-mode-ao1u72zd-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&TimeZone=UTC
secondary.datasource.username=neondb_owner
secondary.datasource.password=npg_jJTmrv56Qyid
```

**JPA Settings**:
```properties
spring.jpa.show-sql=true                                    # Log SQL queries
spring.jpa.hibernate.ddl-auto=update                        # Auto create/update table
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

**Logging**:
```properties
logging.level.root=INFO
logging.level.com.example.demo=DEBUG                        # App logs (DEBUG)
logging.level.org.hibernate.SQL=DEBUG                       # SQL logs
```

**Debug**:
- Nếu database connection fail: kiểm tra URL, username, password
- Nếu table không tạo: kiểm tra `spring.jpa.hibernate.ddl-auto=update`
- Để xem SQL queries: bật `logging.level.org.hibernate.SQL=DEBUG`

---

#### **8. `pom.xml` 📦 (Maven Dependencies)**

**Công dụng**: Quản lý dependencies và build config

**Main Dependencies**:
```xml
spring-boot-starter-web              <!-- Web MVC, REST -->
spring-boot-starter-data-jpa         <!-- JPA, Hibernate -->
spring-boot-starter-actuator         <!-- Health checks -->
postgresql                           <!-- PostgreSQL JDBC driver -->
lombok                               <!-- Code generation -->
```

**Debug**:
- Nếu build fail: kiểm tra Java version (phải 17+)
- Nếu missing dependency: chạy `./mvnw clean compile`
- POM errors: xem logs chi tiết từ `mvnw`

---

#### **9. `Dockerfile` 🐳 (Docker Container)**

**Công dụng**: Build image để deploy lên cloud

**Build Process**:
```dockerfile
Stage 1 (Builder):
  - Base: maven:3.9-eclipse-temurin-17
  - COPY source code
  - Run: mvn clean package
  - Output: target/demo-0.0.1-SNAPSHOT.jar

Stage 2 (Runtime):
  - Base: eclipse-temurin:17-jre (nhẹ hơn)
  - COPY JAR từ Stage 1
  - EXPOSE 8080
  - CMD: java -jar app.jar
```

**Debug**:
- Nếu Docker build fail: kiểm tra internet (maven download slow)
- Image size quá lớn: dùng multi-stage build (đã được tối ưu)

---

#### **10. `DiagnosticService.java` 🔍 (Diagnostics - Advanced)**

**Công dụng**: Chẩn đoán toàn diện hệ thống

**Methods**:
```java
runFullDiagnostics()      // Report toàn diện
testPrimaryDatabase()      // Kiểm tra Render
testSecondaryDatabase()    // Kiểm tra Railway
checkTableStatus()         // Kiểm tra table structure
verifyData()               // So sánh dữ liệu 2 DB
generateRecommendations()  // Gợi ý sửa lỗi
```

**Sử dụng**: Tìm endpoint nào gọi DiagnosticService trong StudentController

---

#### **11. `CorsConfig.java` 🔧 (CORS Configuration)**

**Công dụng**: Cho phép frontend từ any domain gọi API

**Cấu hình**:
```java
allowedOrigins("*")                  // Mọi domain
allowedMethods("GET", "POST", ...)   // Các HTTP methods
maxAge(3600)                          // Cache 1 giờ
```

**Debug**:
- Nếu frontend CORS error: check CorsConfig + @CrossOrigin annotations

---

## 🚀 Phần 3: Chạy Backend Locally và Deploy lên Cloud qua GitHub

### 📌 Yêu cầu hệ thống

Để chạy backend locally, bạn cần:
- **Java 17+**: `java -version`
- **Maven** (hoặc dùng `mvnw` - Maven wrapper đã kèm theo project)
- **PostgreSQL** (hoặc dùng Render/Railway connection string) - ✅ Không cần cài local nếu dùng cloud DB

### 🖥️ Chạy Backend Locally

#### **Step 1: Configure Database (Optional - nếu muốn test local)**

Nếu muốn test với local PostgreSQL:
1. Cài PostgreSQL
2. Tạo database: `createdb student_db`
3. Update `application.properties`:
   ```properties
   # Local PostgreSQL
   spring.datasource.url=jdbc:postgresql://localhost:5432/student_db?sslmode=disable
   spring.datasource.username=postgres
   spring.datasource.password=your_local_password
   
   # Secondary (có thể trỏ sang local khác port hoặc comment out)
   secondary.datasource.url=...
   ```

**Hoặc** (Dễ hơn) - Dùng cloud database URLs trực tiếp:
- Bỏ qua bước này, `application.properties` đã có Render + Railway URLs

#### **Step 2: Clone Repository từ GitHub**

```bash
# Clone project
git clone https://github.com/YOUR_USERNAME/DTDM.git
cd DTDM/demo

# Xem branch hiện tại
git status
```

#### **Step 3: Build Project**

```bash
# Option 1: Dùng Maven wrapper (recommended - không cần cài Maven)
./mvnw clean package          # Windows: .\mvnw.cmd clean package

# Option 2: Dùng Maven (cần cài Maven trước)
mvn clean package
```

**Output**: `target/demo-0.0.1-SNAPSHOT.jar` ✅

#### **Step 4: Chạy Backend**

```bash
# Cách 1: Chạy trực tiếp từ Spring Boot
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run

# Cách 2: Chạy từ JAR đã build
java -jar target/demo-0.0.1-SNAPSHOT.jar

# Cách 3: Chạy với environment variables (cho cloud URLs)
SET SPRING_DATASOURCE_URL=jdbc:postgresql://render-url/db  # Windows
export SPRING_DATASOURCE_URL="..."                          # Linux/Mac
```

**Output**:
```
  ╔═════════════════════════════════════╗
  ║   Demo Application Started         ║
  ╚═════════════════════════════════════╝
  
Tomcat started on port: 8080 ✅
```

#### **Step 5: Test Backend Locally**

```bash
# Lấy danh sách sinh viên
curl http://localhost:8080/api/students

# Thêm sinh viên mới
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Nguyễn Văn A","email":"a@test.com","phone":"0123456789","age":20}'

# Health check
curl http://localhost:8080/api/students/health/status

# View ở browser
Open: http://localhost:8080/api/students
```

---

### 🌐 Deploy lên Cloud (Render / Railway) qua GitHub

#### **Step 1: Prepare GitHub Repository**

```bash
# 1.1: Initialize git (nếu chưa có)
cd d:\VS\DTDM
git init
git add .
git commit -m "Initial commit: Student Backend with dual-database"

# 1.2: Add remote (thay YOUR_USERNAME, REPO_NAME)
git remote add origin https://github.com/YOUR_USERNAME/DTDM.git

# 1.3: Push lên GitHub
git branch -M main
git push -u origin main
```

✅ **GitHub Repository ready!**

---

#### **Step 2: Deploy lên Render (Primary Database)**

##### **2.1: Tạo PostgreSQL Database trên Render**

1. Đăng nhập [render.com](https://render.com)
2. Dashboard → **New** → **PostgreSQL**
3. Điền thông tin:
   - **Name**: `student-db`
   - **Region**: Singapore (gần hơn)
   - **PostgreSQL Version**: 16
   - Để default khác
4. Click **Create Database**
5. **Copy connection string**:
   - URL format: `postgresql://user:password@host:port/dbname?sslmode=require`
   - **Save lại** cho Step 2.3

##### **2.2: Deploy Spring Boot Backend lên Render**

1. Dashboard → **New** → **Web Service**
2. Kết nối GitHub:
   - **Connect account** → GitHub
   - **Select repository** → `DTDM`
   - **Branch**: `main`
3. Điền thông tin:
   - **Name**: `student-backend`
   - **Environment**: `Docker` (Render sẽ dùng Dockerfile)
   - **Region**: Singapore
   - **Pricing**: Free tier (tạm được)
4. **Advanced**:
   - **Auto-deploy**: ✅ Enable (mỗi push đến main → auto deploy)
5. Click **Create Web Service** → Render tự build & deploy
6. **Wait** ~2-3 phút → Backend link sẵn sàng

##### **2.3: Cấu hình Environment Variables (Quan trọng!)**

Sau khi service được tạo:
1. Vào **Settings** → **Environment**
2. Thêm biến:
   ```
   SPRING_DATASOURCE_URL = postgresql://user:password@host:port/dbname?sslmode=require
   SPRING_DATASOURCE_USERNAME = user
   SPRING_DATASOURCE_PASSWORD = password
   SPRING_DATASOURCE_DRIVER_CLASS_NAME = org.postgresql.Driver
   ```
3. Click **Save** → Auto redeploy với biến mới

⚠️ **HOẶC** Update `application.properties` cục bộ → commit → push:
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}  # Environment variable
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
```

---

#### **Step 3: Deploy Secondary Database lên Railway**

##### **3.1: Tạo PostgreSQL trên Railway**

1. Đăng nhập [railway.app](https://railway.app)
2. **New Project** → **Deploy from GitHub** → Chọn DTDM repo
3. **Add PostgreSQL plugin**:
   - Railway → **Plugins** → **Add** → **PostgreSQL**
4. **Get connection string**:
   - Plugin → **PostgreSQL** → **Logs** → Copy connection URL
   - Format: `postgresql://user:password@host:port/db`

##### **3.2: Update Secondary Datasource**

Update `application.properties`:
```properties
# Secondary DB (Railway)
secondary.datasource.url=postgresql://user:password@railway.host:port/railway?sslmode=require
secondary.datasource.username=user
secondary.datasource.password=password
```

**Commit & Push**:
```bash
git add application.properties
git commit -m "Configure Railway secondary database"
git push origin main
```

✅ Render sẽ auto-redeploy với config mới!

---

#### **Step 4: Verify Cloud Deployment**

Kiểm tra backend chạy trên cloud:

```bash
# Test API (thay YOUR_RENDER_URL)
curl https://student-backend-xxx.onrender.com/api/students

# Health check
curl https://student-backend-xxx.onrender.com/api/students/health/status

# Debug info
curl https://student-backend-xxx.onrender.com/api/students/debug/db-info
```

**Expected Response**:
```json
{
  "status": "UP",
  "timestamp": 1712500000000,
  "service": "StudentService"
}
```

✅ **Backend running on cloud!**

---

### 📋 Workflow Update Code & Redeploy

Mỗi khi bạn thay đổi code:

```bash
# 1. Edit code locally
# 2. Build & test local
./mvnw clean package
./mvnw spring-boot:run

# 3. Commit & push GitHub
git add .
git commit -m "Update: [describe changes]"
git push origin main

# 4. Auto-deploy (Render)
# ✅ Render detects push → Auto rebuild & deploy (2-3 phút)
# ✅ Railway detects push → Auto rebuild & deploy

# 5. Verify
curl https://student-backend-xxx.onrender.com/api/students
```

---

### 🔧 Troubleshooting

| Vấn đề | Giải pháp |
|--------|----------|
| **Build fail trên Render** | Kiểm tra logs: Render dashboard → Logs tab; Thường là Java version hoặc dependency issue |
| **Database connection fail** | Check `application.properties` → Database URL, username, password phải đúng; Verify SSL mode |
| **Port conflict local (8080)** | `./mvnw spring-boot:run -Dserver.port=8081` |
| **Git push fail** | `git pull origin main` trước, resolve conflicts, rồi push lại |
| **Secondary DB không sync** | Đủ RAM/CPU không? Render/Railway quota hết không? Kiểm tra `POST /api/students/admin/sync` |

---

### 📚 Lệnh hữu dụng

```bash
# Build
./mvnw clean package              # Build JAR
./mvnw clean compile             # Chỉ compile (không build JAR)

# Run local
./mvnw spring-boot:run           # Development mode
java -jar target/demo*.jar       # Production mode

# Test
./mvnw test                      # Run unit tests
curl http://localhost:8080/api/students  # Manual test API

# Docker (tùy chọn - nếu chạy local)
docker build -t student-backend .
docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=... student-backend

# Git
git status                        # Check status
git add .                        # Stage changes
git commit -m "..."              # Commit
git push origin main             # Push to GitHub
```

---

## 💾 Database Schema (PostgreSQL)

```sql
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    age INTEGER
);
```

**Auto-tạo**: Spring Boot tự động tạo bảng khi `spring.jpa.hibernate.ddl-auto=update` ✅

---

## ✅ Checklist Deployment

- [ ] code pushed to GitHub
- [ ] Render PostgreSQL database created
- [ ] Render Web Service created + configured
- [ ] Environment variables set on Render
- [ ] Railway PostgreSQL created (backup)
- [ ] Secondary datasource configured
- [ ] Health check endpoint responding: `/api/students/health/status`
- [ ] CRUD operations tested
- [ ] Frontend connected to backend URL

---

**Status**: 🟢 Backend Production Ready on Cloud!

Cho bất kỳ câu hỏi, xem logs trên **Render Dashboard** hoặc **Railway Dashboard**.


- **Spring Boot 3.5.11**
- **Spring Data JPA**
- **PostgreSQL** (Render + Railway)
- **Lombok**
- **Maven**

## 📖 Hướng dẫn chi tiết

### Cách hoạt động

1. **Khởi động**:
   - Spring Boot tự động cấu hình Primary DB (Render) từ `spring.datasource.*`
   - `DataSourceConfig` tạo Secondary DB (Railway) từ `secondary.datasource.*`
   - `StudentService` tự động sync dữ liệu từ Render sang Railway (async)

2. **Đọc dữ liệu**:
   - Ưu tiên đọc từ Render (Primary)
   - Nếu Render down → tự động fallback sang Railway
   - Health check mỗi 5 giây để phát hiện sự cố

3. **Ghi dữ liệu**:
   - Luôn ghi vào Render trước (Primary)
   - Async ghi vào Railway (Secondary)
   - Nếu Railway fail → không ảnh hưởng request, data sẽ sync sau

### Lưu ý quan trọng

- **Timezone**: Phải set `TimeZone=UTC` trong connection URL hoặc JVM argument để tránh lỗi với PostgreSQL
- **Không commit** thông tin database lên GitHub
- Luôn sử dụng environment variables khi deploy
- Railway free tier có giới hạn (500 hours/tháng, 1GB storage)

## 🔍 Monitoring & Logs

### ✅ Logs bình thường khi ứng dụng chạy:
```
[main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port 8080
[main] INFO  com.example.demo.DemoApplication - Started DemoApplication in X seconds
[main] INFO  c.e.d.s.service.StudentService - Found X students in primary DB
[main] INFO  c.e.d.s.service.StudentService - Sync completed successfully
[scheduling-1] DEBUG c.e.d.s.service.StudentService - Primary DB is healthy
[scheduling-1] DEBUG c.e.d.s.service.StudentService - Secondary DB is healthy
```

### ⚠️ Warning cần xử lý:
```
WARN  o.s.b.a.o.j.JpaBaseConfiguration$JpaWebConfiguration - 
spring.jpa.open-in-view is enabled by default. 
Therefore, database queries may be performed during view rendering. 
Explicitly configure spring.jpa.open-in-view to disable this warning
```

**Cách fix**: Thêm vào `application.properties`:
```properties
spring.jpa.open-in-view=false
```

**Giải thích**: Cài đặt này tắt auto-opening của Hibernate session trong view layer, 
tránh lazy-loading issues và cải thiện performance.

### 📊 Logs theo dõi khác:
- `✅ Secondary table created/verified` - Railway table đã được tạo
- `✅ Found X students in primary DB` - Sync dữ liệu thành công
- `✅ Primary DB is healthy` - Render DB hoạt động tốt
- `⚠️ Primary DB health check failed` - Render DB có vấn đề
- `⚠️ Secondary DB write failed` - Railway write fail (sẽ sync sau)

### 🔗 Health endpoints:
- `GET /api/students/health/status` - Trạng thái chi tiết
- `GET /api/students/health/ping` - Simple ping check
- `POST /api/students/admin/sync` - Manual sync (emergency)

## 📄 License

MIT License

---

**Developed with ❤️ using Spring Boot**