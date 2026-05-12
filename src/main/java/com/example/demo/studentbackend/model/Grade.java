package com.example.demo.studentbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "math")
    private Double math;

    @Column(name = "literature")
    private Double literature;

    @Column(name = "english")
    private Double english;

    @Column(name = "total")
    private Double total;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        calculateTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotal();
    }

    private void calculateTotal() {
        if (math != null && literature != null && english != null) {
            this.total = Math.round(((math + literature + english) / 3.0) * 100.0) / 100.0;
        } else {
            this.total = null;
        }
    }
}
