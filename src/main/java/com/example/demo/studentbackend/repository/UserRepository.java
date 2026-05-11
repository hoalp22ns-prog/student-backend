package com.example.demo.studentbackend.repository;

import com.example.demo.studentbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * 🏦 REPOSITORY: UserRepository
 * 
 * Mục đích:
 *  - Data Access Layer cho User
 *  - Cung cấp các method để query user từ database
 * 
 * Methods:
 *  - findByUsername(username) → Tìm user bằng username
 *    ├─ Dùng cho login (kiểm tra user có tồn tại không)
 *    └─ Return Optional<User> (có thể rỗng)
 *  
 *  - Inherited từ JpaRepository:
 *    ├─ save(user) → Thêm/update user
 *    ├─ findById(id) → Lấy user theo ID
 *    ├─ findAll() → Lấy tất cả user
 *    └─ delete(user) → Xóa user
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Tìm user bằng username
     * 
     * @param username - Tên đăng nhập
     * @return Optional<User> - User nếu tìm thấy, rỗng nếu không
     * 
     * Spring Data JPA tự động implement method này dựa trên tên
     */
    Optional<User> findByUsername(String username);
}
