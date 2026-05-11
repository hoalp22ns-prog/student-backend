package com.example.demo.studentbackend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * 👤 ENTITY: User (Người Dùng)
 * 
 * Mục đích:
 *  - Lưu thông tin tài khoản đăng nhập
 *  - Username + password (hashed) + role
 * 
 * Fields:
 *  - id: User ID (auto-increment)
 *  - username: Tên đăng nhập (unique, không duplicate)
 *  - password: Mật khẩu đã hash (bcrypt)
 *  - role: Vai trò (ADMIN, TEACHER, STUDENT)
 *  - createdAt: Thời gian tạo
 * 
 * Annotations:
 *  - @Entity: Đánh dấu là JPA entity
 *  - @Data: Lombok auto-generate getter/setter
 *  - @Column(unique=true): Username không được trùng
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username (unique, không trùng)
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    // Mật khẩu (đã hash bằng bcrypt)
    @Column(nullable = false)
    private String password;

    // Vai trò (ADMIN, TEACHER, STUDENT)
    @Column(nullable = false, length = 20)
    private String role;

    // Thời gian tạo
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private java.time.LocalDateTime createdAt;
}
