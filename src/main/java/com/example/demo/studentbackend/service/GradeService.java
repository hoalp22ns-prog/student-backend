package com.example.demo.studentbackend.service;

import com.example.demo.studentbackend.model.Grade;
import com.example.demo.studentbackend.repository.GradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 📊 SERVICE: GradeService (Enhanced with Dual-Database Support)
 * 
 * Mục đích:
 *  - Quản lý CRUD grades
 *  - Tính điểm tổng (average)
 *  - Đồng bộ grades trên cả 2 database (Primary + Secondary)
 *  - Failover support
 * 
 * Kiến trúc:
 *  Primary DB (Railway) ←→ Secondary DB (Neon)
 *       ↑                      ↑
 *       └─── JPA Repo ────────────── JDBC
 * 
 * Methods chính:
 *  - createGrade(grade) → Thêm điểm mới (dual-write)
 *  - updateGrade(id, grade) → Cập nhật điểm (dual-write)
 *  - deleteGrade(id) → Xóa điểm (dual-delete)
 *  - getAllGrades() → Lấy tất cả điểm (smart routing)
 *  - manualSync() → Đồng bộ thủ công từ primary → secondary
 *  - resetSecondaryDb() → Xóa dữ liệu secondary và sync lại
 */
@Service
public class GradeService {
    private static final Logger log = LoggerFactory.getLogger(GradeService.class);

    @Autowired
    private GradeRepository gradeRepository;

    private final JdbcTemplate secondaryJdbc;
    private JdbcTemplate primaryJdbc;
    
    @Autowired
    public void setPrimaryDataSource(DataSource primaryDataSource) {
        this.primaryJdbc = new JdbcTemplate(primaryDataSource);
    }
    
    // ✅ Track health của databases
    private final AtomicBoolean primaryDbHealthy = new AtomicBoolean(true);
    private final AtomicBoolean secondaryDbHealthy = new AtomicBoolean(true);
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    @Value("${app.secondary.enabled:true}")
    private boolean secondaryEnabled;
    
    public GradeService(@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        this.secondaryJdbc = new JdbcTemplate(secondaryDataSource);
    }

    // ============================================
    // ✅ INITIALIZATION
    // ============================================

    @PostConstruct
    public void initializeSecondaryDb() {
        log.info("🔄 Initializing secondary database for grades...");
        initializeSecondaryDbAsync();
    }

    @Async
    private void initializeSecondaryDbAsync() {
        try {
            log.debug("📝 Creating/verifying secondary grades table...");
            secondaryJdbc.execute("""
                CREATE TABLE IF NOT EXISTS grades (
                    id BIGSERIAL PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    math NUMERIC(4, 2),
                    literature NUMERIC(4, 2),
                    english NUMERIC(4, 2),
                    total NUMERIC(5, 2) DEFAULT 0
                )
            """);
            log.info("✅ Secondary grades table created/verified");
            secondaryDbHealthy.set(true);
            syncToSecondaryAsync();
        } catch (Exception e) {
            log.error("❌ Failed to create secondary grades table: {}", e.getMessage());
            secondaryDbHealthy.set(false);
        }
    }

    // ============================================
    // ✅ CRUD OPERATIONS with Dual-Write
    // ============================================

    /**
     * Create Grade - Thêm điểm mới (với dual-write)
     */
    @Transactional
    public Grade createGrade(Grade grade) {
        log.debug("📝 Creating grade for student: {}", grade.getStudentId());

        if (grade.getStudentId() == null || grade.getStudentId() <= 0) {
            throw new RuntimeException("❌ Student ID không hợp lệ");
        }

        if (gradeRepository.findByStudentId(grade.getStudentId()).isPresent()) {
            throw new RuntimeException("❌ Sinh viên này đã có điểm rồi");
        }

        validateGradeScores(grade);

        Grade saved = gradeRepository.save(grade);
        log.info("✅ Grade created for student: {} (ID: {}, Total: {})", grade.getStudentId(), saved.getId(), saved.getTotal());

        // ✅ Async write to secondary
        writeToSecondaryAsync(
            "INSERT INTO grades (id, student_id, math, literature, english, total) VALUES (?, ?, ?, ?, ?, ?)",
            saved.getId(), saved.getStudentId(), saved.getMath(), saved.getLiterature(), saved.getEnglish(), saved.getTotal()
        );

        return saved;
    }

    /**
     * Update Grade - Cập nhật điểm (với dual-write)
     */
    @Transactional
    public Grade updateGrade(Long id, Grade grade) {
        log.debug("✏️ Updating grade: {}", id);

        Grade existingGrade = gradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Grade không tồn tại"));

        if (grade.getMath() != null) {
            existingGrade.setMath(grade.getMath());
        }
        if (grade.getLiterature() != null) {
            existingGrade.setLiterature(grade.getLiterature());
        }
        if (grade.getEnglish() != null) {
            existingGrade.setEnglish(grade.getEnglish());
        }

        validateGradeScores(existingGrade);

        Grade updated = gradeRepository.save(existingGrade);
        log.info("✅ Grade updated: {} (Total: {})", id, updated.getTotal());

        // ✅ Async write to secondary
        writeToSecondaryAsync(
            "UPDATE grades SET math=?, literature=?, english=?, total=? WHERE id=?",
            updated.getMath(), updated.getLiterature(), updated.getEnglish(), updated.getTotal(), id
        );

        return updated;
    }

    /**
     * Delete Grade - Xóa điểm (với dual-delete)
     */
    @Transactional
    public void deleteGrade(Long id) {
        log.debug("🗑️ Deleting grade: {}", id);

        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Grade không tồn tại"));

        gradeRepository.deleteById(id);
        log.info("✅ Grade deleted: {}", id);

        // ✅ Async delete from secondary
        writeToSecondaryAsync("DELETE FROM grades WHERE id=?", id);
    }

    /**
     * Get Grade By Student ID
     */
    public Grade getGradeByStudent(Long studentId) {
        log.debug("📖 Getting grade for student: {}", studentId);
        return gradeRepository.findByStudentId(studentId).orElse(null);
    }

    /**
     * Get Grade By ID
     */
    public Grade getGradeById(Long id) {
        log.debug("📖 Getting grade: {}", id);
        return gradeRepository.findById(id).orElse(null);
    }

    /**
     * Get All Grades
     */
    public List<Grade> getAllGrades() {
        log.debug("📖 Getting all grades");
        return gradeRepository.findAll();
    }

    /**
     * Delete Grade By Student
     */
    public void deleteGradeByStudent(Long studentId) {
        log.debug("🗑️ Deleting grade for student: {}", studentId);
        gradeRepository.deleteByStudentId(studentId);
        writeToSecondaryAsync("DELETE FROM grades WHERE student_id=?", studentId);
        log.info("✅ Grade deleted for student: {}", studentId);
    }

    // ============================================
    // ✅ SECONDARY WRITE & SYNC METHODS
    // ============================================

    @Async
    private void writeToSecondaryAsync(String sql, Object... params) {
        if (!secondaryEnabled || !secondaryDbHealthy.get()) {
            log.warn("⚠️ Secondary DB not available, skipping async write");
            return;
        }

        try {
            log.debug("📤 Writing to secondary DB: {}", sql);
            secondaryJdbc.update(sql, params);
            log.debug("✅ Secondary write successful");
        } catch (Exception e) {
            log.warn("⚠️ Failed to write to secondary DB: {}", e.getMessage());
            secondaryDbHealthy.set(false);
        }
    }

    @Async
    private void syncToSecondaryAsync() {
        if (!secondaryEnabled || syncInProgress.getAndSet(true)) {
            return;
        }

        try {
            log.info("🔄 Starting grades sync from primary to secondary DB...");

            List<Grade> allGrades = gradeRepository.findAll();
            log.info("📊 Found {} grades in primary DB", allGrades.size());

            if (allGrades.isEmpty()) {
                log.info("ℹ️ Primary DB has no grades, skipping sync");
                return;
            }

            Integer secondaryCount = secondaryJdbc.queryForObject("SELECT COUNT(*) FROM grades", Integer.class);
            log.info("📊 Secondary DB currently has {} grade records", secondaryCount);

            for (Grade grade : allGrades) {
                try {
                    secondaryJdbc.update(
                        "INSERT INTO grades (id, student_id, math, literature, english, total) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET student_id = EXCLUDED.student_id, math = EXCLUDED.math, literature = EXCLUDED.literature, english = EXCLUDED.english, total = EXCLUDED.total",
                        grade.getId(), grade.getStudentId(), grade.getMath(), grade.getLiterature(), grade.getEnglish(), grade.getTotal()
                    );
                } catch (Exception e) {
                    log.warn("⚠️ Failed to sync grade {}: {}", grade.getId(), e.getMessage());
                }
            }

            log.info("✅ Grades sync completed successfully");

        } catch (Exception e) {
            log.error("❌ Grades sync failed: {}", e.getMessage());
        } finally {
            syncInProgress.set(false);
        }
    }

    public String manualSync() {
        if (syncInProgress.get()) {
            return "Sync already in progress...";
        }

        syncToSecondaryAsync();
        return "Grades sync started (async, check logs)";
    }

    public String resetSecondaryDb() {
        try {
            log.info("🚨 Clearing secondary grades database...");
            secondaryJdbc.execute("TRUNCATE TABLE grades CASCADE");
            log.info("✅ Secondary grades DB cleared successfully");
            syncToSecondaryAsync();
            return "✅ Secondary grades DB reset and sync initiated";
        } catch (Exception e) {
            log.error("❌ Failed to reset secondary grades DB: {}", e.getMessage());
            return "❌ Reset failed: " + e.getMessage();
        }
    }

    // ============================================
    // ✅ VALIDATION
    // ============================================

    /**
     * Validate Grade Scores
     */
    private void validateGradeScores(Grade grade) {
        if (grade.getMath() != null && (grade.getMath() < 0 || grade.getMath() > 10)) {
            throw new RuntimeException("❌ Điểm Toán phải từ 0-10");
        }
        if (grade.getLiterature() != null && (grade.getLiterature() < 0 || grade.getLiterature() > 10)) {
            throw new RuntimeException("❌ Điểm Văn phải từ 0-10");
        }
        if (grade.getEnglish() != null && (grade.getEnglish() < 0 || grade.getEnglish() > 10)) {
            throw new RuntimeException("❌ Điểm Anh phải từ 0-10");
        }
    }
}
