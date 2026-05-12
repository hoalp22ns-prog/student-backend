package com.example.demo.studentbackend.service;

import com.example.demo.studentbackend.model.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DatabaseCompareSyncService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCompareSyncService.class);

    private final JdbcTemplate primaryJdbc;
    private final JdbcTemplate secondaryJdbc;

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    @Value("${app.primary.enabled:true}")
    private boolean primaryEnabled;

    @Value("${spring.datasource.url:}")
    private String primaryUrl;

    @Value("${app.secondary.enabled:true}")
    private boolean secondaryEnabled;

    public DatabaseCompareSyncService(
            @Qualifier("primaryDatasource") DataSource primaryDataSource,
            @Qualifier("secondaryDataSource") DataSource secondaryDataSource
    ) {
        this.primaryJdbc = new JdbcTemplate(primaryDataSource);
        this.secondaryJdbc = new JdbcTemplate(secondaryDataSource);
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 8000)
    public void compareAndSyncStudents() {
        if (!secondaryEnabled) {
            return;
        }

        if (!isRealPrimaryConfigured()) {
            log.debug("Primary DB is disabled or missing. Skip compare sync.");
            return;
        }

        if (!syncRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            if (!isDbHealthy(primaryJdbc) || !isDbHealthy(secondaryJdbc)) {
                log.warn("Skip compare sync because one database is not healthy.");
                return;
            }

            ensureStudentTable(primaryJdbc);
            ensureStudentTable(secondaryJdbc);

            long primaryCount = countStudents(primaryJdbc);
            long secondaryCount = countStudents(secondaryJdbc);

            log.info("Compare students count: primary={}, secondary={}", primaryCount, secondaryCount);

            if (primaryCount == secondaryCount) {
                return;
            }

            if (secondaryCount > primaryCount) {
                log.warn("Secondary has more students. Sync secondary -> primary.");
                syncStudents(secondaryJdbc, primaryJdbc);
            } else {
                log.warn("Primary has more students. Sync primary -> secondary.");
                syncStudents(primaryJdbc, secondaryJdbc);
            }

            fixStudentSequence(primaryJdbc);
            fixStudentSequence(secondaryJdbc);

        } catch (Exception e) {
            log.error("Compare sync failed: {}", e.getMessage(), e);
        } finally {
            syncRunning.set(false);
        }
    }

    private void syncStudents(JdbcTemplate source, JdbcTemplate target) {
        List<Student> students = source.query(
                "SELECT id, name, email, phone, age FROM students ORDER BY id",
                (rs, rowNum) -> new Student(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getInt("age")
                )
        );

        for (Student student : students) {
            target.update("""
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

        log.warn("Synced {} students.", students.size());
    }
    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void compareAndSyncUsers() {
        if (!secondaryEnabled || !isRealPrimaryConfigured()) return;
        if (!syncRunning.compareAndSet(false, true)) return;

        try {
            if (!isDbHealthy(primaryJdbc) || !isDbHealthy(secondaryJdbc)) return;

            long primaryCount = countTable(primaryJdbc, "users");
            long secondaryCount = countTable(secondaryJdbc, "users");

            log.info("Compare users count: primary={}, secondary={}", primaryCount, secondaryCount);

            if (primaryCount == secondaryCount) return;

            if (secondaryCount > primaryCount) {
                syncUsers(secondaryJdbc, primaryJdbc);
            } else {
                syncUsers(primaryJdbc, secondaryJdbc);
            }
        } finally {
            syncRunning.set(false);
        }
    }

    private void syncUsers(JdbcTemplate source, JdbcTemplate target) {
        var users = source.queryForList("SELECT id, username, password, role FROM users ORDER BY id");

        for (var user : users) {
            target.update("""
                INSERT INTO users (id, username, password, role)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (username)
                DO UPDATE SET
                    password = EXCLUDED.password,
                    role = EXCLUDED.role
                """,
                user.get("id"),
                user.get("username"),
                user.get("password"),
                user.get("role")
            );
        }

        fixSequence(target, "users_id_seq", "users");
    }
@Scheduled(fixedDelay = 10000, initialDelay = 12000)
public void compareAndSyncGrades() {
    if (!secondaryEnabled || !isRealPrimaryConfigured()) return;
    if (!syncRunning.compareAndSet(false, true)) return;

    try {
        if (!isDbHealthy(primaryJdbc) || !isDbHealthy(secondaryJdbc)) return;

        long primaryCount = countTable(primaryJdbc, "grades");
        long secondaryCount = countTable(secondaryJdbc, "grades");

        log.info("Compare grades count: primary={}, secondary={}", primaryCount, secondaryCount);

        if (primaryCount == secondaryCount) return;

        if (secondaryCount > primaryCount) {
            syncGrades(secondaryJdbc, primaryJdbc);
        } else {
            syncGrades(primaryJdbc, secondaryJdbc);
        }
    } finally {
        syncRunning.set(false);
    }
}

private void syncGrades(JdbcTemplate source, JdbcTemplate target) {
    var grades = source.queryForList("""
        SELECT id, student_id, math, literature, english, total
        FROM grades
        ORDER BY id
    """);

    for (var grade : grades) {
        target.update("""
            INSERT INTO grades (id, student_id, math, literature, english, total)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id)
            DO UPDATE SET
                student_id = EXCLUDED.student_id,
                math = EXCLUDED.math,
                literature = EXCLUDED.literature,
                english = EXCLUDED.english,
                total = EXCLUDED.total
            """,
            grade.get("id"),
            grade.get("student_id"),
            grade.get("math"),
            grade.get("literature"),
            grade.get("english"),
            grade.get("total")
        );
    }

    fixSequence(target, "grades_id_seq", "grades");
}
private long countTable(JdbcTemplate jdbc, String tableName) {
    Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    return count == null ? 0 : count;
}

private void fixSequence(JdbcTemplate jdbc, String sequenceName, String tableName) {
    try {
        jdbc.execute(String.format("""
            SELECT setval(
                '%s',
                COALESCE((SELECT MAX(id) FROM %s), 0) + 1,
                false
            )
        """, sequenceName, tableName));
    } catch (Exception e) {
        log.warn("Could not fix sequence {}: {}", sequenceName, e.getMessage());
    }
}

    private boolean isDbHealthy(JdbcTemplate jdbc) {
        try {
            Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private long countStudents(JdbcTemplate jdbc) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM students", Long.class);
        return count == null ? 0 : count;
    }

    private void ensureStudentTable(JdbcTemplate jdbc) {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS students (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255),
                email VARCHAR(255) UNIQUE,
                phone VARCHAR(255),
                age INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    private void fixStudentSequence(JdbcTemplate jdbc) {
        try {
            jdbc.execute("""
                SELECT setval(
                    'students_id_seq',
                    COALESCE((SELECT MAX(id) FROM students), 0) + 1,
                    false
                )
            """);
        } catch (Exception e) {
            log.warn("Could not fix students sequence: {}", e.getMessage());
        }
    }

    private boolean isRealPrimaryConfigured() {
        return primaryEnabled && primaryUrl != null && !primaryUrl.trim().isEmpty();
    }
}
