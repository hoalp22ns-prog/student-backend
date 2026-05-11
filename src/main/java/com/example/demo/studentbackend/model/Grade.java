package com.example.demo.studentbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 📊 ENTITY: Grade (Điểm Số)
 * 
 * Mục đích:
 *  - Lưu điểm số của sinh viên (Toán, Văn, Anh)
 *  - Tính điểm tổng tự động
 * 
 * Fields:
 *  - id: Điểm ID (auto-increment)
 *  - studentId: ID sinh viên (foreign key)
 *  - math: Điểm Toán (0-10)
 *  - literature: Điểm Văn (0-10)
 *  - english: Điểm Anh (0-10)
 *  - total: Điểm tổng = (math + literature + english) / 3
 *  - createdAt: Thời gian tạo
 *  - updatedAt: Thời gian cập nhật
 * 
 * Relationships:
 *  - Mỗi sinh viên có thể có 1 record điểm
 *  - Foreign key: student_id → students.id
 *  - Khi xóa sinh viên → xóa điểm (ON DELETE CASCADE)
 */
@Entity
@Table(name = "grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key: Sinh viên (không null)
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    // Điểm Toán (0-10)
    @Column(name = "math")
    private Double math;

    // Điểm Văn (0-10)
    @Column(name = "literature")
    private Double literature;

    // Điểm Anh (0-10)
    @Column(name = "english")
    private Double english;

    // Điểm Tổng = (math + literature + english) / 3
    @Column(name = "total")
    private Double total;

    // Thời gian tạo
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Thời gian cập nhật
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * @PrePersist: Chạy trước khi lưu lần đầu tiên
     * - Set createdAt = now
     * - Set updatedAt = now
     * - Tính điểm tổng
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateTotal();
    }

    /**
     * @PreUpdate: Chạy trước khi update
     * - Update updatedAt
     * - Tính lại điểm tổng
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotal();
    }

    /**
     * Tính điểm tổng = (math + literature + english) / 3
     * Nếu thiếu điểm → total = null
     */
    private void calculateTotal() {
        if (math != null && literature != null && english != null) {
            this.total = Math.round(((math + literature + english) / 3.0) * 100.0) / 100.0;
        } else {
            this.total = null;
        }
    }
}
