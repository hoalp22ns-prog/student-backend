package com.example.demo.studentbackend.service;

import com.example.demo.studentbackend.model.Student;
import com.example.demo.studentbackend.repository.StudentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    @Autowired
    private StudentRepository studentRepository;

    private final JdbcTemplate secondaryJdbc;
    private JdbcTemplate primaryJdbc;

    @Value("${app.primary.enabled:true}")
    private boolean primaryEnabled;

    @Value("${spring.datasource.url:}")
    private String primaryUrl;

    @Value("${app.secondary.enabled:true}")
    private boolean secondaryEnabled;

    private final AtomicBoolean primaryDbHealthy = new AtomicBoolean(false);
    private final AtomicBoolean secondaryDbHealthy = new AtomicBoolean(true);
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private final AtomicBoolean reverseSyncInProgress = new AtomicBoolean(false);
    private final AtomicBoolean primaryWasDown = new AtomicBoolean(false);

    @Autowired
    public void setPrimaryDataSource(DataSource primaryDataSource) {
        this.primaryJdbc = new JdbcTemplate(primaryDataSource);
    }

    public StudentService(@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        this.secondaryJdbc = new JdbcTemplate(secondaryDataSource);
    }

    @PostConstruct
    public void init() {
        boolean configured = isPrimaryConfigured();
        primaryDbHealthy.set(configured);
        primaryWasDown.set(!configured);
        initializeSecondaryDb();
    }

    @Async
    public void initializeSecondaryDb() {
        try {
            secondaryJdbc.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255),
                    email VARCHAR(255),
                    phone VARCHAR(255),
                    age INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            secondaryDbHealthy.set(true);
        } catch (Exception e) {
            secondaryDbHealthy.set(false);
            log.warn("Cannot initialize secondary students table: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void checkPrimaryDbHealth() {
        if (!isPrimaryConfigured()) {
            primaryDbHealthy.set(false);
            primaryWasDown.set(true);
            return;
        }

        try {
            Integer result = primaryJdbc.queryForObject("SELECT 1", Integer.class);
            if (result != null) {
                boolean recovered = primaryWasDown.getAndSet(false);
                primaryDbHealthy.set(true);

                if (recovered) {
                    log.warn("Primary DB recovered. Start reverse sync students secondary -> primary.");
                    syncFromSecondaryToPrimaryAsync();
                }
            }
        } catch (Exception e) {
            primaryDbHealthy.set(false);
            primaryWasDown.set(true);
            log.warn("Primary DB is down: {}", e.getClass().getSimpleName());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void checkSecondaryDbHealth() {
        if (!secondaryEnabled) {
            secondaryDbHealthy.set(false);
            return;
        }

        try {
            Integer result = secondaryJdbc.queryForObject("SELECT 1", Integer.class);
            secondaryDbHealthy.set(result != null);
        } catch (Exception e) {
            secondaryDbHealthy.set(false);
            log.warn("Secondary DB is down: {}", e.getClass().getSimpleName());
        }
    }

    public List<Student> getAllStudents() {
        if (primaryDbHealthy.get()) {
            try {
                return studentRepository.findAll();
            } catch (Exception e) {
                primaryDbHealthy.set(false);
                primaryWasDown.set(true);
                log.warn("Read primary failed. Fallback to secondary: {}", e.getMessage());
            }
        }

        return queryStudentsFromSecondary("SELECT id, name, email, phone, age FROM students ORDER BY id");
    }

    public Student getStudentById(Long id) {
        if (primaryDbHealthy.get()) {
            try {
                return studentRepository.findById(id).orElse(null);
            } catch (Exception e) {
                primaryDbHealthy.set(false);
                primaryWasDown.set(true);
            }
        }

        try {
            return secondaryJdbc.queryForObject(
                    "SELECT id, name, email, phone, age FROM students WHERE id = ?",
                    (rs, rowNum) -> new Student(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getInt("age")
                    ),
                    id
            );
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Student createStudent(Student student) {
        if (primaryDbHealthy.get()) {
            try {
                Student saved = studentRepository.save(student);
                writeStudentToSecondary(saved);
                return saved;
            } catch (Exception e) {
                primaryDbHealthy.set(false);
                primaryWasDown.set(true);
                log.warn("Create primary failed. Write to secondary: {}", e.getMessage());
            }
        }

        return createStudentInSecondary(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student student) {
        student.setId(id);

        if (primaryDbHealthy.get()) {
            try {
                Student updated = studentRepository.save(student);
                writeStudentToSecondary(updated);
                return updated;
            } catch (Exception e) {
                primaryDbHealthy.set(false);
                primaryWasDown.set(true);
                log.warn("Update primary failed. Update secondary: {}", e.getMessage());
            }
        }

        updateStudentInSecondary(id, student);
        return student;
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (primaryDbHealthy.get()) {
            try {
                studentRepository.deleteById(id);
                deleteStudentInSecondary(id);
                return;
            } catch (Exception e) {
                primaryDbHealthy.set(false);
                primaryWasDown.set(true);
                log.warn("Delete primary failed. Delete secondary: {}", e.getMessage());
            }
        }

        deleteStudentInSecondary(id);
    }

    private Student createStudentInSecondary(Student student) {
        if (!secondaryEnabled || !secondaryDbHealthy.get()) {
            throw new RuntimeException("Both primary and secondary DB are unavailable");
        }

        Long id = secondaryJdbc.queryForObject(
                "INSERT INTO students (name, email, phone, age) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getAge()
        );

        student.setId(id);
        primaryWasDown.set(true);
        return student;
    }

    private void updateStudentInSecondary(Long id, Student student) {
        if (!secondaryEnabled || !secondaryDbHealthy.get()) {
            throw new RuntimeException("Secondary DB is unavailable");
        }

        secondaryJdbc.update(
                "UPDATE students SET name = ?, email = ?, phone = ?, age = ? WHERE id = ?",
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getAge(),
                id
        );
    }

    private void deleteStudentInSecondary(Long id) {
        if (!secondaryEnabled || !secondaryDbHealthy.get()) {
            throw new RuntimeException("Secondary DB is unavailable");
        }

        secondaryJdbc.update("DELETE FROM students WHERE id = ?", id);
    }

    @Async
    public void writeStudentToSecondary(Student student) {
        if (!secondaryEnabled || !secondaryDbHealthy.get()) {
            return;
        }

        try {
            secondaryJdbc.update("""
                INSERT INTO students (id, name, email, phone, age)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id)
                DO UPDATE SET
                    name = EXCLUDED.name,
                    email = EXCLUDED.email,
                    phone = EXCLUDED.phone,
                    age = EXCLUDED.age
                """,
                    student.getId(),
                    student.getName(),
                    student.getEmail(),
                    student.getPhone(),
                    student.getAge()
            );
        } catch (Exception e) {
            secondaryDbHealthy.set(false);
            log.warn("Write student to secondary failed: {}", e.getMessage());
        }
    }

    @Async
    public void syncFromSecondaryToPrimaryAsync() {
        if (!isPrimaryConfigured() || reverseSyncInProgress.getAndSet(true)) {
            return;
        }

        try {
            List<Student> students = queryStudentsFromSecondary(
                    "SELECT id, name, email, phone, age FROM students ORDER BY id"
            );

            for (Student student : students) {
                primaryJdbc.update("""
                    INSERT INTO students (id, name, email, phone, age)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (id)
                    DO UPDATE SET
                        name = EXCLUDED.name,
                        email = EXCLUDED.email,
                        phone = EXCLUDED.phone,
                        age = EXCLUDED.age
                    """,
                        student.getId(),
                        student.getName(),
                        student.getEmail(),
                        student.getPhone(),
                        student.getAge()
                );
            }

            log.warn("Reverse sync students completed: {}", students.size());
        } catch (Exception e) {
            primaryDbHealthy.set(false);
            primaryWasDown.set(true);
            log.error("Reverse sync students failed: {}", e.getMessage());
        } finally {
            reverseSyncInProgress.set(false);
        }
    }

    @Async
    public void syncToSecondaryAsync() {
        if (!secondaryEnabled || syncInProgress.getAndSet(true)) {
            return;
        }

        try {
            List<Student> students = studentRepository.findAll();
            for (Student student : students) {
                writeStudentToSecondary(student);
            }
        } finally {
            syncInProgress.set(false);
        }
    }

    public String manualSync() {
        if (primaryDbHealthy.get()) {
            syncToSecondaryAsync();
            return "Sync primary -> secondary started";
        }

        if (isPrimaryConfigured()) {
            syncFromSecondaryToPrimaryAsync();
            return "Reverse sync secondary -> primary started";
        }

        return "Primary DB is disabled. Nothing to sync to primary.";
    }

    public String resetSecondaryDb() {
        try {
            secondaryJdbc.execute("TRUNCATE TABLE students CASCADE");
            if (primaryDbHealthy.get()) {
                syncToSecondaryAsync();
            }
            return "Secondary students reset completed";
        } catch (Exception e) {
            return "Reset failed: " + e.getMessage();
        }
    }

    public Map<String, Object> checkDataConsistency() {
        long primaryCount = -1;
        long secondaryCount = -1;

        try {
            if (primaryDbHealthy.get()) {
                primaryCount = studentRepository.count();
            }
        } catch (Exception e) {
            primaryDbHealthy.set(false);
            primaryWasDown.set(true);
        }

        try {
            secondaryCount = secondaryJdbc.queryForObject("SELECT COUNT(*) FROM students", Long.class);
        } catch (Exception e) {
            secondaryDbHealthy.set(false);
        }

        boolean consistent = primaryCount >= 0 && secondaryCount >= 0 && primaryCount == secondaryCount;

        return Map.of(
                "primary_enabled", isPrimaryConfigured(),
                "primary_healthy", primaryDbHealthy.get(),
                "secondary_healthy", secondaryDbHealthy.get(),
                "primary_count", primaryCount,
                "secondary_count", secondaryCount,
                "consistent", consistent
        );
    }

    public String getHealthStatus() {
        return String.format(
                "Primary enabled: %s | Primary: %s | Secondary: %s",
                isPrimaryConfigured(),
                primaryDbHealthy.get() ? "UP" : "DOWN",
                secondaryDbHealthy.get() ? "UP" : "DOWN"
        );
    }

    public List<Student> queryPrimaryDirect() {
        if (!primaryDbHealthy.get()) {
            return List.of();
        }

        return primaryJdbc.query(
                "SELECT id, name, email, phone, age FROM students ORDER BY id",
                (rs, rowNum) -> new Student(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getInt("age")
                )
        );
    }

    public int getPrimaryCountDirect() {
        if (!primaryDbHealthy.get()) {
            return -1;
        }

        return primaryJdbc.queryForObject("SELECT COUNT(*) FROM students", Integer.class);
    }

    private List<Student> queryStudentsFromSecondary(String sql) {
        if (!secondaryEnabled || !secondaryDbHealthy.get()) {
            return List.of();
        }

        try {
            return secondaryJdbc.query(
                    sql,
                    (rs, rowNum) -> new Student(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getInt("age")
                    )
            );
        } catch (Exception e) {
            secondaryDbHealthy.set(false);
            log.warn("Query secondary failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isPrimaryConfigured() {
        return primaryEnabled && primaryUrl != null && !primaryUrl.trim().isEmpty();
    }
}
