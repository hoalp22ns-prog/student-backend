# Student Backend API

Backend Spring Boot cho hệ thống quản lý sinh viên. Ứng dụng cung cấp REST API để quản lý sinh viên, điểm số, tài khoản đăng nhập bằng JWT, đồng thời có cơ chế dùng 2 PostgreSQL database: primary database để đọc/ghi chính và secondary database để đồng bộ, dự phòng.

## Công nghệ sử dụng

- Java 17
- Spring Boot 3.5.11
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- PostgreSQL
- Lombok
- Maven Wrapper
- Docker

## Cây thư mục

```text
demo/
├── Dockerfile
├── HELP.md
├── README.md
├── API_QUICK_REFERENCE.md
├── DATABASE_MANAGEMENT.md
├── TESTING_GUIDE.md
├── application.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       ├── DemoApplication.java
│   │   │       └── studentbackend/
│   │   │           ├── config/
│   │   │           │   ├── CorsConfig.java
│   │   │           │   ├── DataSourceConfig.java
│   │   │           │   ├── DatabaseInitializer.java
│   │   │           │   ├── JwtAuthFilter.java
│   │   │           │   └── SecurityConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── AdminController.java
│   │   │           │   ├── GradeController.java
│   │   │           │   ├── StudentController.java
│   │   │           │   └── UserController.java
│   │   │           ├── model/
│   │   │           │   ├── Grade.java
│   │   │           │   ├── Student.java
│   │   │           │   └── User.java
│   │   │           ├── repository/
│   │   │           │   ├── GradeRepository.java
│   │   │           │   ├── StudentRepository.java
│   │   │           │   └── UserRepository.java
│   │   │           ├── service/
│   │   │           │   ├── GradeService.java
│   │   │           │   ├── StudentService.java
│   │   │           │   └── UserService.java
│   │   │           └── util/
│   │   │               ├── DatabaseResetService.java
│   │   │               ├── DiagnosticService.java
│   │   │               └── JwtUtil.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── schema.sql
│   └── test/
│       └── java/com/example/demo/
│           └── DemoApplicationTests.java
└── target/
    └── ... file build sinh ra bởi Maven
```

## Công dụng từng nhóm file

### File gốc dự án

| File | Công dụng |
| --- | --- |
| `pom.xml` | Khai báo dependency Maven, Java 17, Spring Boot plugin, cấu hình build JAR. |
| `mvnw`, `mvnw.cmd` | Maven Wrapper, dùng để chạy Maven mà không cần cài Maven global. |
| `Dockerfile` | Build Docker image theo 2 stage: build bằng JDK 17, chạy bằng JRE 17. |
| `application.yml` | File cấu hình mẫu theo YAML cho dual database và profile dev/prod. Code hiện tại chủ yếu đọc từ `application.properties`. |
| `API_QUICK_REFERENCE.md` | Tài liệu tham khảo nhanh API. |
| `DATABASE_MANAGEMENT.md` | Ghi chú quản lý/reset database. |
| `TESTING_GUIDE.md` | Hướng dẫn test API và backend. |
| `HELP.md` | Tài liệu trợ giúp sinh bởi Spring Initializr. |
| `target/` | Thư mục build output, chứa class đã compile và file JAR. Không cần sửa thủ công. |

### Entry point

| File | Công dụng |
| --- | --- |
| `DemoApplication.java` | Hàm `main`, khởi động Spring Boot, quét bean, mở embedded Tomcat. |

### `config/`

| File | Công dụng |
| --- | --- |
| `DataSourceConfig.java` | Tạo primary datasource, secondary datasource, `JdbcTemplate`, bật async, scheduling và transaction management. |
| `DatabaseInitializer.java` | Khi app khởi động, tạo các bảng `students`, `users`, `grades` nếu chưa tồn tại ở cả primary và secondary database. |
| `SecurityConfig.java` | Cấu hình Spring Security, JWT stateless, endpoint public/protected, password encoder BCrypt và CORS. |
| `JwtAuthFilter.java` | Đọc header `Authorization: Bearer <token>`, validate JWT và set authentication vào `SecurityContext`. |
| `CorsConfig.java` | Cấu hình CORS global để frontend có thể gọi API. |

### `model/`

| File | Công dụng |
| --- | --- |
| `Student.java` | Entity bảng `students`: `id`, `name`, `email`, `phone`, `age`. |
| `Grade.java` | Entity bảng `grades`: điểm toán, văn, anh, điểm trung bình `total`, thời gian tạo/cập nhật. |
| `User.java` | Entity bảng `users`: `username`, password đã hash, role, thời gian tạo. |

### `repository/`

| File | Công dụng |
| --- | --- |
| `StudentRepository.java` | Repository JPA cho CRUD sinh viên. |
| `GradeRepository.java` | Repository JPA cho điểm số, có thêm `findByStudentId` và `deleteByStudentId`. |
| `UserRepository.java` | Repository JPA cho tài khoản, có `findByUsername` để login/register. |

### `service/`

| File | Công dụng |
| --- | --- |
| `StudentService.java` | Business logic sinh viên: CRUD, health check, fallback sang secondary DB, đồng bộ dữ liệu sang secondary DB, kiểm tra consistency. |
| `GradeService.java` | Business logic điểm số: CRUD, validate điểm 0-10, tính `total`, đồng bộ điểm sang secondary DB. |
| `UserService.java` | Business logic đăng ký/đăng nhập: hash password, kiểm tra password, tạo JWT, đồng bộ user sang secondary DB. |

### `controller/`

| File | Công dụng |
| --- | --- |
| `StudentController.java` | REST API `/api/students`: CRUD sinh viên, health, debug, sync dữ liệu sinh viên. |
| `GradeController.java` | REST API `/api/grades`: CRUD điểm số, yêu cầu JWT. |
| `UserController.java` | REST API `/api/auth`: đăng ký, đăng nhập, health auth. |
| `AdminController.java` | REST API `/api/admin`: reset dữ liệu, kiểm tra auto-increment, failover, thống kê database. |

### `util/`

| File | Công dụng |
| --- | --- |
| `JwtUtil.java` | Tạo JWT, validate JWT, lấy username từ token. |
| `DiagnosticService.java` | Chẩn đoán kết nối database, kiểm tra bảng, kiểm tra dữ liệu và health primary/secondary. |
| `DatabaseResetService.java` | Xóa dữ liệu các bảng ở primary/secondary, reset sequence, kiểm tra số lượng bản ghi. |

### `resources/`

| File | Công dụng |
| --- | --- |
| `application.properties` | Cấu hình chính của app: datasource, JPA, logging, port, JWT secret. |
| `schema.sql` | SQL tạo bảng `students`, `users`, `grades` và index. Hiện `spring.sql.init` đang bị comment nên file này không tự chạy mặc định. |

### Test

| File | Công dụng |
| --- | --- |
| `DemoApplicationTests.java` | Test mặc định `contextLoads()` để kiểm tra Spring context khởi động được. |

## Luồng hoạt động chính

1. App khởi động từ `DemoApplication.java`.
2. `DataSourceConfig` tạo kết nối primary và secondary database.
3. `DatabaseInitializer` tạo bảng nếu chưa có.
4. `StudentService` và `GradeService` tạo bảng secondary nếu cần và bắt đầu sync dữ liệu nền.
5. Client gọi API qua controller.
6. Service xử lý nghiệp vụ, ghi primary trước, sau đó ghi/sync secondary.
7. Spring Security dùng `JwtAuthFilter` để bảo vệ các endpoint cần đăng nhập.

## API chính

### Auth

| Method | Endpoint | Công dụng | Auth |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Đăng ký user mới | Không |
| `POST` | `/api/auth/login` | Đăng nhập, trả JWT token | Không |
| `GET` | `/api/auth/health` | Kiểm tra auth service | Không |

Ví dụ đăng ký:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","role":"ADMIN"}'
```

Ví dụ đăng nhập:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Students

| Method | Endpoint | Công dụng | Auth |
| --- | --- | --- | --- |
| `GET` | `/api/students` | Lấy danh sách sinh viên | Không |
| `GET` | `/api/students/{id}` | Lấy sinh viên theo id | Có |
| `POST` | `/api/students` | Thêm sinh viên | Có |
| `PUT` | `/api/students/{id}` | Cập nhật sinh viên | Có |
| `DELETE` | `/api/students/{id}` | Xóa sinh viên | Có |
| `GET` | `/api/students/health/status` | Health status | Không |
| `GET` | `/api/students/health/detailed` | Health + consistency | Không |
| `GET` | `/api/students/debug/diagnostics` | Diagnostic đầy đủ | Không |
| `POST` | `/api/students/admin/sync` | Sync sinh viên thủ công | Tùy cấu hình security hiện tại |

Ví dụ thêm sinh viên:

```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"name":"Nguyen Van A","email":"a@example.com","phone":"0123456789","age":20}'
```

### Grades

Tất cả endpoint `/api/grades/**` yêu cầu JWT.

| Method | Endpoint | Công dụng |
| --- | --- | --- |
| `GET` | `/api/grades` | Lấy tất cả điểm |
| `GET` | `/api/grades/{id}` | Lấy điểm theo id |
| `GET` | `/api/grades/student/{studentId}` | Lấy điểm theo sinh viên |
| `POST` | `/api/grades` | Tạo điểm mới |
| `PUT` | `/api/grades/{id}` | Cập nhật điểm |
| `DELETE` | `/api/grades/{id}` | Xóa điểm |
| `POST` | `/api/grades/admin/sync` | Sync điểm sang secondary |
| `POST` | `/api/grades/admin/reset-secondary` | Reset bảng điểm ở secondary rồi sync lại |

Ví dụ tạo điểm:

```bash
curl -X POST http://localhost:8080/api/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"studentId":1,"math":8.5,"literature":9.0,"english":7.5}'
```

### Admin

| Method | Endpoint | Công dụng |
| --- | --- | --- |
| `POST` | `/api/admin/reset-all` | Xóa dữ liệu ở cả 2 DB và reset sequence |
| `POST` | `/api/admin/reset-primary` | Xóa dữ liệu primary DB |
| `POST` | `/api/admin/reset-secondary` | Xóa dữ liệu secondary DB |
| `GET` | `/api/admin/verify-autoincrement` | Kiểm tra sequence auto-increment |
| `GET` | `/api/admin/check-failover` | Kiểm tra trạng thái primary/secondary |
| `GET` | `/api/admin/database-stats` | Kiểm tra thống kê dữ liệu |

Lưu ý: các endpoint admin hiện đang được permit public trong `SecurityConfig`. Nếu deploy thật, nên bảo vệ bằng quyền admin/JWT.

## Cấu hình database

File cấu hình chính:

```text
src/main/resources/application.properties
```

Các key quan trọng:

```properties
spring.datasource.url=...
spring.datasource.username=...
spring.datasource.password=...

secondary.datasource.url=...
secondary.datasource.username=...
secondary.datasource.password=...

server.port=${PORT:8080}
jwt.secret=...
jwt.expiration=86400000
```

Lưu ý bảo mật: không nên commit password database và JWT secret thật lên GitHub. Khi deploy, nên chuyển sang environment variables.

## Cách chạy chương trình

### Yêu cầu

- Java 17 trở lên
- Có kết nối tới PostgreSQL primary và secondary trong `application.properties`
- Windows dùng `mvnw.cmd`, Linux/macOS dùng `./mvnw`

Kiểm tra Java:

```bash
java -version
```

### Chạy bằng Maven Wrapper

Trên Windows PowerShell:

```powershell
cd D:\VS\DTDM\demo
.\mvnw.cmd spring-boot:run
```

Trên Linux/macOS:

```bash
cd demo
./mvnw spring-boot:run
```

Mặc định API chạy ở:

```text
http://localhost:8080
```

Health check:

```bash
curl http://localhost:8080/api/students/health/ping
```

### Build JAR

Windows:

```powershell
cd D:\VS\DTDM\demo
.\mvnw.cmd clean package
java -jar target\demo-0.0.1-SNAPSHOT.jar
```

Linux/macOS:

```bash
cd demo
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Chạy test

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

### Chạy bằng Docker

Build image:

```bash
docker build -t student-backend .
```

Run container:

```bash
docker run -p 8080:8080 student-backend
```

Nếu cần truyền biến môi trường:

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host:5432/db" \
  -e SPRING_DATASOURCE_USERNAME="postgres" \
  -e SPRING_DATASOURCE_PASSWORD="password" \
  student-backend
```

## Ghi chú khi phát triển

- `target/` là thư mục sinh ra sau khi build, không sửa trực tiếp.
- `schema.sql` có lệnh `DROP TABLE`, chỉ dùng cẩn thận trong môi trường test.
- `spring.jpa.hibernate.ddl-auto=validate` nghĩa là Hibernate chỉ kiểm tra schema, không tự sửa bảng.
- `DatabaseInitializer` mới là phần đang tạo bảng nếu chưa tồn tại.
- Secondary DB có thể lỗi mà app vẫn chạy, nhưng một số chức năng sync/failover sẽ bị ảnh hưởng.
- Endpoint `/api/grades/**` cần token JWT trong header `Authorization: Bearer <TOKEN>`.
