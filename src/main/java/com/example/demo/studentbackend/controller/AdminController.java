package com.example.demo.studentbackend.controller;

import com.example.demo.studentbackend.util.DatabaseResetService;
import com.example.demo.studentbackend.util.DiagnosticService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 🛠️ ADMIN API - Database Management
 * 
 * Endpoints để quản lý database:
 * - Reset dữ liệu (khi gặp sự cố)
 * - Kiểm tra auto-increment
 * - Kiểm tra failover (DB phụ thay thế DB chính)
 * - Kiểm tra health status
 * 
 * API Routes:
 * POST /api/admin/reset-all - Xóa tất cả dữ liệu ở cả 2 DB
 * POST /api/admin/reset-primary - Xóa dữ liệu ở DB chính
 * POST /api/admin/reset-secondary - Xóa dữ liệu ở DB phụ
 * GET /api/admin/verify-autoincrement - Kiểm tra auto-increment
 * GET /api/admin/check-failover - Kiểm tra failover
 * GET /api/admin/database-stats - Thống kê dữ liệu
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private DatabaseResetService databaseResetService;
    
    @Autowired
    private DiagnosticService diagnosticService;
    
    /**
     * 🧹 Xóa TẤT CẢ dữ liệu ở cả 2 DB (reset to clean state)
     */
    @PostMapping("/reset-all")
    public ResponseEntity<?> resetAllData() {
        logger.warn("🧹 [ADMIN API] Reset all data request received");
        
        try {
            databaseResetService.resetAllData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Đã xóa tất cả dữ liệu ở cả 2 database");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Reset all data failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 🧹 Xóa dữ liệu ở DB CHÍNH
     */
    @PostMapping("/reset-primary")
    public ResponseEntity<?> resetPrimaryOnly() {
        logger.warn("🧹 [ADMIN API] Reset primary database request received");
        
        try {
            databaseResetService.resetPrimaryOnly();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Đã xóa dữ liệu ở DB chính (Railway)");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Reset primary failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 🧹 Xóa dữ liệu ở DB PHỤ
     */
    @PostMapping("/reset-secondary")
    public ResponseEntity<?> resetSecondaryOnly() {
        logger.warn("🧹 [ADMIN API] Reset secondary database request received");
        
        try {
            databaseResetService.resetSecondaryOnly();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Đã xóa dữ liệu ở DB phụ (Neon)");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Reset secondary failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 🔍 Kiểm tra AUTO-INCREMENT trên cả 3 bảng
     * 
     * Trả về: thông tin sequence (last_value, increment_by) cho mỗi bảng
     */
    @GetMapping("/verify-autoincrement")
    public ResponseEntity<?> verifyAutoIncrement() {
        logger.info("🔍 [ADMIN API] Check auto-increment request received");
        
        try {
            databaseResetService.verifyAutoIncrement();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Đã kiểm tra auto-increment. Xem logs chi tiết.");
            response.put("tables", new String[]{"students", "grades", "users"});
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Auto-increment verification failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 📊 Kiểm tra FAILOVER:
     * - Nếu DB chính sập → chuyển sang DB phụ?
     * - Lấy dữ liệu từ DB phụ được không?
     */
    @GetMapping("/check-failover")
    public ResponseEntity<?> checkFailover() {
        logger.info("🔄 [ADMIN API] Check failover scenario request received");
        
        try {
            // Kiểm tra trạng thái cả 2 DB
            boolean primaryHealthy = diagnosticService.isPrimaryHealthy();
            boolean secondaryHealthy = diagnosticService.isSecondaryHealthy();
            
            Map<String, Object> response = new HashMap<>();
            response.put("primary_database", new HashMap<String, Object>() {{
                put("healthy", primaryHealthy);
                put("status", primaryHealthy ? "✅ OK" : "❌ DOWN");
            }});
            response.put("secondary_database", new HashMap<String, Object>() {{
                put("healthy", secondaryHealthy);
                put("status", secondaryHealthy ? "✅ OK" : "❌ DOWN");
            }});
            
            if (primaryHealthy && secondaryHealthy) {
                response.put("failover_status", "✅ Cả 2 DB khỏe. Failover sẵn sàng.");
            } else if (!primaryHealthy && secondaryHealthy) {
                response.put("failover_status", "⚠️  DB chính DOWN. Nên chuyển sang DB phụ!");
            } else if (primaryHealthy && !secondaryHealthy) {
                response.put("failover_status", "⚠️  DB phụ DOWN. Cần khôi phục!");
            } else {
                response.put("failover_status", "❌ Cả 2 DB DOWN. Tình trạng NGUY HIỂM!");
            }
            
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failover check failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 📊 Thống kê dữ liệu ở cả 2 DB
     */
    @GetMapping("/database-stats")
    public ResponseEntity<?> getDatabaseStats() {
        logger.info("📊 [ADMIN API] Get database stats request received");
        
        try {
            databaseResetService.verifyTablesEmpty();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Đã kiểm tra thống kê. Xem logs chi tiết.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Database stats check failed", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
