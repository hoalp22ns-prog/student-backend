package com.example.demo.studentbackend.controller;

import com.example.demo.studentbackend.model.User;
import com.example.demo.studentbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔐 CONTROLLER: UserController
 * 
 * Mục đích:
 *  - Xử lý authentication endpoints
 *  - Login: return JWT token
 *  - Register: tạo user mới
 * 
 * Endpoints:
 *  POST /api/auth/login → Login (return token)
 *  POST /api/auth/register → Register user mới
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * Login Endpoint - Đăng nhập
     * 
     * @param loginRequest - {username, password}
     * @return {token, message, timestamp}
     * 
     * Response 200 OK:
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "message": "✅ Đăng nhập thành công",
     *   "username": "admin",
     *   "timestamp": 1715400000000
     * }
     * 
     * Response 401 Unauthorized:
     * {
     *   "error": "❌ Username không tồn tại",
     *   "timestamp": 1715400000000
     * }
     * 
     * Usage:
     * curl -X POST http://localhost:8080/api/auth/login \
     *   -H "Content-Type: application/json" \
     *   -d '{"username":"admin","password":"admin123"}'
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "❌ Username và password không được trống",
                        "timestamp", System.currentTimeMillis()
                ));
            }

            log.info("🔐 Login request for user: {}", username);

            // Login service sẽ throw exception nếu sai
            String token = userService.login(username, password);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "✅ Đăng nhập thành công");
            response.put("username", username);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("❌ Login failed: {}", e.getMessage());

            return ResponseEntity.status(401).body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("❌ Login error: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "error", "❌ Lỗi server",
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Register Endpoint - Đăng ký user mới
     * 
     * @param registerRequest - {username, password, role}
     * @return {message, user, timestamp}
     * 
     * Response 201 Created:
     * {
     *   "message": "✅ Đăng ký thành công",
     *   "user": {
     *     "id": 1,
     *     "username": "newuser",
     *     "role": "STUDENT"
     *   },
     *   "timestamp": 1715400000000
     * }
     * 
     * Response 400 Bad Request:
     * {
     *   "error": "❌ Username đã tồn tại",
     *   "timestamp": 1715400000000
     * }
     * 
     * Usage:
     * curl -X POST http://localhost:8080/api/auth/register \
     *   -H "Content-Type: application/json" \
     *   -d '{"username":"newuser","password":"pass123","role":"STUDENT"}'
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String password = registerRequest.get("password");
            String role = registerRequest.get("role");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "❌ Username và password không được trống",
                        "timestamp", System.currentTimeMillis()
                ));
            }

            log.info("📝 Register request for user: {}", username);

            User user = userService.register(username, password, role);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Đăng ký thành công");
            response.put("user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole()
            ));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(201).body(response);

        } catch (RuntimeException e) {
            log.error("❌ Register failed: {}", e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("❌ Register error: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "error", "❌ Lỗi server",
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Simple health check untuk auth service
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "✅ Auth service is running",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
