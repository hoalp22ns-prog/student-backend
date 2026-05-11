package com.example.demo.studentbackend.repository;

import com.example.demo.studentbackend.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * 🏦 REPOSITORY: GradeRepository
 * 
 * Mục đích:
 *  - Data Access Layer cho Grade
 *  - Cung cấp các method để query grades từ database
 * 
 * Custom Methods:
 *  - findByStudentId(studentId) → Tìm điểm của 1 sinh viên
 *  - deleteByStudentId(studentId) → Xóa điểm khi xóa sinh viên
 *  
 * Inherited từ JpaRepository:
 *  - save(grade) → Thêm/update điểm
 *  - findById(id) → Lấy điểm theo ID
 *  - findAll() → Lấy tất cả điểm
 *  - delete(grade) → Xóa điểm
 */
@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    
    /**
     * Tìm điểm của một sinh viên
     * 
     * @param studentId - ID sinh viên
     * @return Optional<Grade> - Điểm nếu tìm thấy
     */
    Optional<Grade> findByStudentId(Long studentId);
    
    /**
     * Lấy danh sách tất cả grades của nhiều sinh viên
     * 
     * @return List<Grade> - Danh sách grades
     */
    List<Grade> findAll();
    
    /**
     * Xóa điểm của một sinh viên
     * (Dùng khi xóa sinh viên để xóa grades liên quan)
     * 
     * @param studentId - ID sinh viên
     */
    void deleteByStudentId(Long studentId);
}
