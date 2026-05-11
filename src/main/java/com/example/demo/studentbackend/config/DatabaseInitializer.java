package com.example.demo.studentbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🗄️ DATABASE INITIALIZER - EXECUTE ON APP STARTUP
 * 
 * Tạo bảng NẾU CHƯA TỒN TẠI (không drop dữ liệu cũ)
 * Strategy: CREATE TABLE IF NOT EXISTS (preserve existing data)
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    @Autowired
    private DataSource primaryDatasource;
    
    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("🗄️ Initializing database schema (PRIMARY)...");
        initializePrimaryDatabase();
        
        logger.info("🗄️ Initializing database schema (SECONDARY)...");
        initializeSecondaryDatabase();
    }

    /**
     * Initialize Primary Database (Railway)
     * Strategy: CREATE IF NOT EXISTS (preserve existing data)
     */
    private void initializePrimaryDatabase() {
        try (Connection conn = primaryDatasource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // ✅ Create STUDENTS table IF NOT EXISTS (no drop!)
            if (!tableExists(conn, "students")) {
                String createStudentsTable = "CREATE TABLE students (" +
                        "id BIGSERIAL PRIMARY KEY, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255) UNIQUE, " +
                        "phone VARCHAR(20), " +
                        "age INT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
                stmt.execute(createStudentsTable);
                logger.info("✅ Students table created successfully (auto-increment: id BIGSERIAL)");
            } else {
                logger.info("ℹ️ Students table already exists (preserving data + auto-increment)");
            }
            
            // ✅ Create USERS table IF NOT EXISTS
            if (!tableExists(conn, "users")) {
                String createUsersTable = "CREATE TABLE users (" +
                        "id SERIAL PRIMARY KEY, " +
                        "username VARCHAR(255) NOT NULL UNIQUE, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER', " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
                stmt.execute(createUsersTable);
                logger.info("✅ Users table created (auto-increment: id SERIAL)");
            } else {
                logger.info("ℹ️ Users table already exists (preserving data + auto-increment)");
            }
            
            // ✅ Create GRADES table IF NOT EXISTS
            if (!tableExists(conn, "grades")) {
                String createGradesTable = "CREATE TABLE grades (" +
                        "id SERIAL PRIMARY KEY, " +
                        "student_id BIGINT NOT NULL, " +
                        "math DOUBLE PRECISION, " +
                        "literature DOUBLE PRECISION, " +
                        "english DOUBLE PRECISION, " +
                        "total DOUBLE PRECISION, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "CONSTRAINT fk_student_grade FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE" +
                        ")";
                stmt.execute(createGradesTable);
                logger.info("✅ Grades table created (auto-increment: id SERIAL) with FK to students");
                
                // Create index for grades lookup
                String createIndex = "CREATE INDEX IF NOT EXISTS idx_grades_student_id ON grades(student_id)";
                stmt.execute(createIndex);
                logger.info("✅ Index created on grades.student_id");
            } else {
                logger.info("ℹ️ Grades table already exists (preserving data + auto-increment + index)");
            }
            
            logger.info("✅ Primary database schema initialization completed!");
            
        } catch (Exception e) {
            logger.error("❌ Error initializing primary database schema", e);
            throw new RuntimeException("Failed to initialize primary database schema", e);
        }
    }

    /**
     * Initialize Secondary Database (Neon)
     * Strategy: CREATE IF NOT EXISTS + sync from primary
     */
    private void initializeSecondaryDatabase() {
        try (Connection conn = secondaryDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // ✅ Create STUDENTS table IF NOT EXISTS
            if (!tableExists(conn, "students")) {
                String createStudentsTable = "CREATE TABLE students (" +
                        "id BIGSERIAL PRIMARY KEY, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255) UNIQUE, " +
                        "phone VARCHAR(20), " +
                        "age INT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
                stmt.execute(createStudentsTable);
                logger.info("✅ Secondary Students table created (auto-increment: id BIGSERIAL)");
            } else {
                logger.info("ℹ️ Secondary Students table already exists (preserving data + auto-increment)");
            }
            
            // ✅ Create USERS table IF NOT EXISTS (for secondary backup)
            if (!tableExists(conn, "users")) {
                String createUsersTable = "CREATE TABLE users (" +
                        "id SERIAL PRIMARY KEY, " +
                        "username VARCHAR(255) NOT NULL UNIQUE, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER', " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
                stmt.execute(createUsersTable);
                logger.info("✅ Secondary Users table created (auto-increment: id SERIAL)");
            } else {
                logger.info("ℹ️ Secondary Users table already exists (preserving data + auto-increment)");
            }
            
            // ✅ Create GRADES table IF NOT EXISTS
            if (!tableExists(conn, "grades")) {
                String createGradesTable = "CREATE TABLE grades (" +
                        "id SERIAL PRIMARY KEY, " +
                        "student_id BIGINT NOT NULL, " +
                        "math DOUBLE PRECISION, " +
                        "literature DOUBLE PRECISION, " +
                        "english DOUBLE PRECISION, " +
                        "total DOUBLE PRECISION, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
                stmt.execute(createGradesTable);
                logger.info("✅ Secondary Grades table created (auto-increment: id SERIAL)");
            } else {
                logger.info("ℹ️ Secondary Grades table already exists (preserving data + auto-increment)");
            }
            
            logger.info("✅ Secondary database schema initialization completed!");
            
        } catch (Exception e) {
            logger.warn("⚠️ Secondary database initialization warning (may not be critical): {}", e.getMessage());
            // Don't throw - secondary DB failure shouldn't crash the app
        }
    }
    
    private boolean tableExists(Connection conn, String tableName) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM " + tableName + " LIMIT 1");
            return true;
        } catch (Exception e) {
            // Table doesn't exist
            return false;
        }
    }
}
