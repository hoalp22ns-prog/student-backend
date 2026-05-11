package com.example.demo.studentbackend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 🧹 DATABASE RESET SERVICE
 * 
 * Giúp xóa tất cả dữ liệu trong các bảng (KHÔNG XÓA BẢN hoặc cấu trúc)
 * Dùng khi gặp sự cố: xung khắc dữ liệu, lỗi đồng bộ, v.v.
 * 
 * Công dụng:
 * - resetAllData() : Xóa tất cả dữ liệu ở cả 2 DB
 * - resetPrimaryOnly() : Xóa dữ liệu ở DB chính (Railway)
 * - resetSecondaryOnly() : Xóa dữ liệu ở DB phụ (Neon)
 * - verifyTablesEmpty() : Kiểm tra xem dữ liệu đã xóa chưa
 */
@Service
public class DatabaseResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseResetService.class);
    
    @Autowired
    private DataSource primaryDatasource;
    
    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;
    
    /**
     * 🧹 Xóa TẤT CẢ dữ liệu ở cả 2 database
     * Giữ bảng + cấu trúc + auto-increment
     */
    public void resetAllData() {
        logger.warn("🧹 [RESET] Bắt đầu xóa tất cả dữ liệu ở cả 2 database...");
        
        try {
            resetPrimaryOnly();
            resetSecondaryOnly();
            
            logger.warn("✅ [RESET] Đã xóa tất cả dữ liệu ở cả 2 database thành công!");
        } catch (Exception e) {
            logger.error("❌ [RESET] Lỗi khi xóa tất cả dữ liệu", e);
            throw new RuntimeException("Failed to reset all databases", e);
        }
    }
    
    /**
     * 🧹 Xóa dữ liệu ở Database CHÍNH (Railway)
     */
    public void resetPrimaryOnly() {
        logger.warn("🧹 [RESET PRIMARY] Bắt đầu xóa dữ liệu ở DB chính (Railway)...");
        
        try (Connection conn = primaryDatasource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Xóa dữ liệu (KHÔNG xóa bảng) - thứ tự để tránh FK constraint
            logger.info("   Deleting from grades...");
            stmt.execute("DELETE FROM grades");
            logger.info("   ✅ Grades deleted");
            
            logger.info("   Deleting from users...");
            stmt.execute("DELETE FROM users");
            logger.info("   ✅ Users deleted");
            
            logger.info("   Deleting from students...");
            stmt.execute("DELETE FROM students");
            logger.info("   ✅ Students deleted");
            
            // Reset auto-increment sequences
            stmt.execute("ALTER SEQUENCE students_id_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE grades_id_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE users_id_seq RESTART WITH 1");
            
            logger.warn("✅ [RESET PRIMARY] Đã xóa dữ liệu ở DB chính + reset sequences!");
            
        } catch (Exception e) {
            logger.error("❌ [RESET PRIMARY] Lỗi khi xóa dữ liệu ở DB chính", e);
            throw new RuntimeException("Failed to reset primary database", e);
        }
    }
    
    /**
     * 🧹 Xóa dữ liệu ở Database PHỤ (Neon)
     */
    public void resetSecondaryOnly() {
        logger.warn("🧹 [RESET SECONDARY] Bắt đầu xóa dữ liệu ở DB phụ (Neon)...");
        
        try (Connection conn = secondaryDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Xóa dữ liệu (KHÔNG xóa bảng) - thứ tự để tránh FK constraint
            logger.info("   Deleting from grades...");
            stmt.execute("DELETE FROM grades");
            logger.info("   ✅ Grades deleted");
            
            logger.info("   Deleting from users...");
            stmt.execute("DELETE FROM users");
            logger.info("   ✅ Users deleted");
            
            logger.info("   Deleting from students...");
            stmt.execute("DELETE FROM students");
            logger.info("   ✅ Students deleted");
            
            // Reset auto-increment sequences
            stmt.execute("ALTER SEQUENCE students_id_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE grades_id_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE users_id_seq RESTART WITH 1");
            
            logger.warn("✅ [RESET SECONDARY] Đã xóa dữ liệu ở DB phụ + reset sequences!");
            
        } catch (Exception e) {
            logger.error("❌ [RESET SECONDARY] Lỗi khi xóa dữ liệu ở DB phụ", e);
            throw new RuntimeException("Failed to reset secondary database", e);
        }
    }
    
    /**
     * ✅ Kiểm tra xem các bảng đã trống chưa (sau khi reset)
     */
    public void verifyTablesEmpty() {
        logger.info("✅ Kiểm tra dữ liệu ở cả 2 database...");
        
        verifyTableCountPrimary();
        verifyTableCountSecondary();
    }
    
    private void verifyTableCountPrimary() {
        try (Connection conn = primaryDatasource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            long studentCount = getTableCount(stmt, "SELECT COUNT(*) FROM students");
            long gradeCount = getTableCount(stmt, "SELECT COUNT(*) FROM grades");
            long userCount = getTableCount(stmt, "SELECT COUNT(*) FROM users");
            
            logger.info("📊 [PRIMARY] Students: {}, Grades: {}, Users: {}", 
                    studentCount, gradeCount, userCount);
            
        } catch (Exception e) {
            logger.error("❌ Error verifying primary database count", e);
        }
    }
    
    private void verifyTableCountSecondary() {
        try (Connection conn = secondaryDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            long studentCount = getTableCount(stmt, "SELECT COUNT(*) FROM students");
            long gradeCount = getTableCount(stmt, "SELECT COUNT(*) FROM grades");
            long userCount = getTableCount(stmt, "SELECT COUNT(*) FROM users");
            
            logger.info("📊 [SECONDARY] Students: {}, Grades: {}, Users: {}", 
                    studentCount, gradeCount, userCount);
            
        } catch (Exception e) {
            logger.error("❌ Error verifying secondary database count", e);
        }
    }
    
    private long getTableCount(Statement stmt, String query) throws Exception {
        try (ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }
    
    /**
     * 🔍 Kiểm tra auto-increment trên cả 3 bảng
     */
    public void verifyAutoIncrement() {
        logger.info("🔍 Kiểm tra auto-increment ở cả 2 database...");
        
        logger.info("--- PRIMARY DATABASE ---");
        checkAutoIncrementPrimary();
        
        logger.info("--- SECONDARY DATABASE ---");
        checkAutoIncrementSecondary();
    }
    
    private void checkAutoIncrementPrimary() {
        try (Connection conn = primaryDatasource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            logger.info("✅ Students: {}", getAutoIncrementInfo(stmt, "students_id_seq"));
            logger.info("✅ Grades: {}", getAutoIncrementInfo(stmt, "grades_id_seq"));
            logger.info("✅ Users: {}", getAutoIncrementInfo(stmt, "users_id_seq"));
            
        } catch (Exception e) {
            logger.error("❌ Error checking primary auto-increment", e);
        }
    }
    
    private void checkAutoIncrementSecondary() {
        try (Connection conn = secondaryDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            logger.info("✅ Students: {}", getAutoIncrementInfo(stmt, "students_id_seq"));
            logger.info("✅ Grades: {}", getAutoIncrementInfo(stmt, "grades_id_seq"));
            logger.info("✅ Users: {}", getAutoIncrementInfo(stmt, "users_id_seq"));
            
        } catch (Exception e) {
            logger.error("❌ Error checking secondary auto-increment", e);
        }
    }
    
    private String getAutoIncrementInfo(Statement stmt, String sequenceName) throws Exception {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT last_value, increment_by FROM " + sequenceName)) {
            if (rs.next()) {
                return "last_value=" + rs.getLong(1) + ", increment_by=" + rs.getLong(2);
            }
        }
        return "SEQUENCE NOT FOUND";
    }
}
