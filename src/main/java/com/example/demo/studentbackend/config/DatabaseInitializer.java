package com.example.demo.studentbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource primaryDatasource;

    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

    @Value("${app.primary.enabled:true}")
    private boolean primaryEnabled;

    @Value("${spring.datasource.url:}")
    private String primaryUrl;

    @Override
    public void run(String... args) {
        if (primaryEnabled && !isBlank(primaryUrl)) {
            logger.info("Initializing primary database schema...");
            initializeDatabase(primaryDatasource, true);
        } else {
            logger.warn("Primary database is disabled or missing. Skipping primary initialization.");
        }

        logger.info("Initializing secondary database schema...");
        initializeDatabase(secondaryDataSource, false);
    }

    private void initializeDatabase(DataSource dataSource, boolean withForeignKey) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE,
                    phone VARCHAR(20),
                    age INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGSERIAL PRIMARY KEY,
                    username VARCHAR(255) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            if (withForeignKey) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS grades (
                        id BIGSERIAL PRIMARY KEY,
                        student_id BIGINT NOT NULL,
                        math DOUBLE PRECISION,
                        literature DOUBLE PRECISION,
                        english DOUBLE PRECISION,
                        total DOUBLE PRECISION,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_student_grade FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
                    )
                """);
            } else {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS grades (
                        id BIGSERIAL PRIMARY KEY,
                        student_id BIGINT NOT NULL,
                        math DOUBLE PRECISION,
                        literature DOUBLE PRECISION,
                        english DOUBLE PRECISION,
                        total DOUBLE PRECISION,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            }

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_grades_student_id ON grades(student_id)");
            logger.info("Database schema initialized.");

        } catch (Exception e) {
            logger.warn("Database initialization failed: {}", e.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
