package com.example.demo.studentbackend.service;

import com.example.demo.studentbackend.model.User;
import com.example.demo.studentbackend.repository.UserRepository;
import com.example.demo.studentbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 🔐 SERVICE: UserService (Enhanced with Dual-Database Support)
 * 
 * Mục đích:
 *  - Xử lý logic đăng nhập (login)
 *  - Register user mới (sync to secondary DB)
 *  - Validate JWT token
 * 
 * Methods chính:
 *  - login(username, password) → Check username/password, return JWT token
 *  - register(username, password, role) → Thêm user mới vào cả 2 database
 *  - validateToken(token) → Check token hợp lệ
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private final JdbcTemplate secondaryJdbc;
    private JdbcTemplate primaryJdbc;
    
    @Autowired
    public void setPrimaryDataSource(DataSource primaryDataSource) {
        this.primaryJdbc = new JdbcTemplate(primaryDataSource);
    }
    
    @Value("${app.secondary.enabled:true}")
    private boolean secondaryEnabled;
    
    private final AtomicBoolean secondaryDbHealthy = new AtomicBoolean(true);

    public UserService(@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        this.secondaryJdbc = new JdbcTemplate(secondaryDataSource);
    }

    /**
     * Login - Đăng nhập người dùng
     * 
     * @param username - Tên đăng nhập
     * @param password - Mật khẩu (plain text)
     * @return JWT token nếu đăng nhập thành công
     * @throws RuntimeException nếu username không tồn tại hoặc password sai
     * 
     * Flow:
     *  1. Tìm user bằng username từ database
     *  2. Kiểm tra mật khẩu (so sánh plain text vs hash)
     *  3. Nếu đúng → generate JWT token
     *  4. Nếu sai → throw exception
     */
    public String login(String username, String password) {
        log.info("🔐 Login attempt for user: {}", username);

        // Step 1: Tìm user bằng username
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("⚠️ Login failed - user not found: {}", username);
            throw new RuntimeException("❌ Username không tồn tại");
        }

        User user = userOpt.get();

        // Step 2: Kiểm tra mật khẩu
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("⚠️ Login failed - wrong password for user: {}", username);
            throw new RuntimeException("❌ Mật khẩu sai");
        }

        // Step 3: Generate JWT token
        String token = jwtUtil.generateToken(username);
        log.info("✅ Login successful for user: {}", username);

        return token;
    }

    /**
     * Register - Đăng ký user mới (với dual-write)
     * 
     * @param username - Tên đăng nhập (unique)
     * @param password - Mật khẩu (plain text, sẽ được hash)
     * @param role - Vai trò (ADMIN, TEACHER, STUDENT)
     * @return User object vừa được tạo
     * @throws RuntimeException nếu username đã tồn tại
     */
    public User register(String username, String password, String role) {
        log.info("📝 Register attempt for user: {}", username);

        // Step 1: Check username chưa tồn tại
        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("⚠️ Register failed - username already exists: {}", username);
            throw new RuntimeException("❌ Username đã tồn tại");
        }

        // Step 2: Hash mật khẩu
        String hashedPassword = passwordEncoder.encode(password);

        // Step 3: Tạo user mới
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setRole(role != null ? role : "STUDENT");

        // Step 4: Lưu vào primary database
        User savedUser = userRepository.save(user);
        log.info("✅ User registered in primary DB: {}", username);

        // Step 5: Async write to secondary
        writeToSecondaryAsync(username, hashedPassword, user.getRole());

        return savedUser;
    }
    
    /**
     * Async write user to secondary database
     */
    @Async
    private void writeToSecondaryAsync(String username, String hashedPassword, String role) {
        if (!secondaryEnabled || !secondaryDbHealthy.get()) {
            log.warn("⚠️ Secondary DB not available, skipping user sync");
            return;
        }

        try {
            log.debug("📤 Syncing user to secondary DB: {}", username);
            secondaryJdbc.update(
                "INSERT INTO users (username, password, role) VALUES (?, ?, ?) ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password, role = EXCLUDED.role",
                username, hashedPassword, role
            );
            log.debug("✅ User synced to secondary DB: {}", username);
        } catch (Exception e) {
            log.warn("⚠️ Failed to sync user {} to secondary DB: {}", username, e.getMessage());
            secondaryDbHealthy.set(false);
        }
    }

    /**
     * Validate Token - Kiểm tra JWT token hợp lệ
     * 
     * @param token - JWT token string
     * @return true nếu token hợp lệ, false nếu expired/invalid
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    /**
     * Extract Username từ token
     * 
     * @param token - JWT token string
     * @return Username được lưu trong token
     */
    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    /**
     * Lấy user theo username
     * 
     * @param username - Tên đăng nhập
     * @return Optional<User> - User nếu tìm thấy
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Lấy tất cả user
     * 
     * @return List<User> - Danh sách tất cả user
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
