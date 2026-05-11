package com.example.demo.studentbackend.controller;

import com.example.demo.studentbackend.model.Grade;
import com.example.demo.studentbackend.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📊 CONTROLLER: GradeController
 * 
 * Mục đích:
 *  - CRUD operations cho Grade
 *  - Chỉ user đã login mới access
 * 
 * Endpoints:
 *  GET    /api/grades              → Lấy tất cả grades
 *  GET    /api/grades/{id}         → Lấy grade theo ID
 *  GET    /api/grades/student/{studentId} → Lấy grade của 1 sinh viên
 *  POST   /api/grades              → Thêm grade mới
 *  PUT    /api/grades/{id}         → Cập nhật grade
 *  DELETE /api/grades/{id}         → Xóa grade
 * 
 * Security:
 *  - Tất cả endpoints đều yêu cầu authentication (JWT token)
 *  - Client phải gửi: Authorization: Bearer <token>
 *  - Spring Security sẽ validate token bằng JwtAuthFilter
 */
@RestController
@RequestMapping("/api/grades")
@CrossOrigin(origins = "*")
public class GradeController {
    private static final Logger log = LoggerFactory.getLogger(GradeController.class);

    @Autowired
    private GradeService gradeService;

    /**
     * Get All Grades - Lấy tất cả grades
     * 
     * @param auth - Authentication object (từ SecurityContext)
     * @return List<Grade>
     * 
     * Security: ✅ Cần login (JWT token)
     * 
     * Usage:
     * curl -X GET http://localhost:8080/api/grades \
     *   -H "Authorization: Bearer <token>"
     */
    @GetMapping
    public ResponseEntity<List<Grade>> getAllGrades(Authentication auth) {
        if (auth == null) {
            log.warn("⚠️ Unauthorized access attempt to GET /api/grades");
            return ResponseEntity.status(403).build();
        }

        log.info("📖 User {} requested all grades", auth.getName());
        List<Grade> grades = gradeService.getAllGrades();

        return ResponseEntity.ok(grades);
    }

    /**
     * Get Grade By ID - Lấy grade theo ID
     * 
     * @param id - Grade ID
     * @param auth - Authentication object
     * @return Grade object
     * 
     * Security: ✅ Cần login
     * 
     * Usage:
     * curl -X GET http://localhost:8080/api/grades/1 \
     *   -H "Authorization: Bearer <token>"
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGradeById(
            @PathVariable Long id,
            Authentication auth
    ) {
        if (auth == null) {
            return ResponseEntity.status(403).body(Map.of("error", "❌ Unauthorized"));
        }

        log.info("📖 User {} requested grade: {}", auth.getName(), id);
        Grade grade = gradeService.getGradeById(id);

        if (grade == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(grade);
    }

    /**
     * Get Grade By Student ID - Lấy grade của 1 sinh viên
     * 
     * @param studentId - Student ID
     * @param auth - Authentication object
     * @return Grade object
     * 
     * Security: ✅ Cần login
     * 
     * Usage:
     * curl -X GET http://localhost:8080/api/grades/student/1 \
     *   -H "Authorization: Bearer <token>"
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getGradeByStudent(
            @PathVariable Long studentId,
            Authentication auth
    ) {
        if (auth == null) {
            return ResponseEntity.status(403).body(Map.of("error", "❌ Unauthorized"));
        }

        log.info("📖 User {} requested grade for student: {}", auth.getName(), studentId);
        Grade grade = gradeService.getGradeByStudent(studentId);

        if (grade == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(grade);
    }

    /**
     * Create Grade - Thêm grade mới
     * 
     * @param grade - Grade object {studentId, math, literature, english}
     * @param auth - Authentication object
     * @return Created Grade object
     * 
     * Security: ✅ Cần login
     * 
     * Request body:
     * {
     *   "studentId": 1,
     *   "math": 8.5,
     *   "literature": 9.0,
     *   "english": 7.5
     * }
     * 
     * Response:
     * {
     *   "id": 1,
     *   "studentId": 1,
     *   "math": 8.5,
     *   "literature": 9.0,
     *   "english": 7.5,
     *   "total": 8.33,
     *   "createdAt": "2026-05-11T12:00:00"
     * }
     * 
     * Usage:
     * curl -X POST http://localhost:8080/api/grades \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer <token>" \
     *   -d '{"studentId":1,"math":8.5,"literature":9.0,"english":7.5}'
     */
    @PostMapping
    public ResponseEntity<?> createGrade(
            @RequestBody Grade grade,
            Authentication auth
    ) {
        if (auth == null) {
            return ResponseEntity.status(403).body(Map.of("error", "❌ Unauthorized"));
        }

        try {
            log.info("📝 User {} creating grade for student: {}", auth.getName(), grade.getStudentId());
            Grade created = gradeService.createGrade(grade);

            return ResponseEntity.status(201).body(created);

        } catch (RuntimeException e) {
            log.error("❌ Error creating grade: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update Grade - Cập nhật grade
     * 
     * @param id - Grade ID
     * @param grade - Updated grade data
     * @param auth - Authentication object
     * @return Updated Grade object
     * 
     * Security: ✅ Cần login
     * 
     * Request body:
     * {
     *   "math": 9.0,
     *   "literature": 8.5,
     *   "english": 8.0
     * }
     * 
     * Usage:
     * curl -X PUT http://localhost:8080/api/grades/1 \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer <token>" \
     *   -d '{"math":9.0,"literature":8.5,"english":8.0}'
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGrade(
            @PathVariable Long id,
            @RequestBody Grade grade,
            Authentication auth
    ) {
        if (auth == null) {
            return ResponseEntity.status(403).body(Map.of("error", "❌ Unauthorized"));
        }

        try {
            log.info("✏️ User {} updating grade: {}", auth.getName(), id);
            Grade updated = gradeService.updateGrade(id, grade);

            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            log.error("❌ Error updating grade: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete Grade - Xóa grade
     * 
     * @param id - Grade ID
     * @param auth - Authentication object
     * @return Empty response
     * 
     * Security: ✅ Cần login
     * 
     * Usage:
     * curl -X DELETE http://localhost:8080/api/grades/1 \
     *   -H "Authorization: Bearer <token>"
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGrade(
            @PathVariable Long id,
            Authentication auth
    ) {
        if (auth == null) {
            return ResponseEntity.status(403).body(Map.of("error", "❌ Unauthorized"));
        }

        try {
            log.info("🗑️ User {} deleting grade: {}", auth.getName(), id);
            gradeService.deleteGrade(id);

            return ResponseEntity.ok().body(Map.of(
                    "message", "✅ Grade deleted successfully",
                    "id", id
            ));

        } catch (RuntimeException e) {
            log.error("❌ Error deleting grade: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Manual sync endpoint for grades
     * - Synchronizes all grades from primary to secondary database
     * - Useful for emergency data sync
     */
    @PostMapping("/admin/sync")
    public ResponseEntity<Map<String, String>> manualGradesSync() {
        String result = gradeService.manualSync();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Reset secondary database endpoint for grades
     * - Clears secondary database
     * - Triggers fresh sync from primary
     */
    @PostMapping("/admin/reset-secondary")
    public ResponseEntity<Map<String, String>> resetSecondaryDb() {
        String result = gradeService.resetSecondaryDb();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        response.put("action", "Secondary grades DB truncated and fresh sync initiated");
        
        return ResponseEntity.ok(response);
    }
}
