package com.example.demo.studentbackend.config;

import com.example.demo.studentbackend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 🔐 JWT AUTH FILTER: JwtAuthFilter
 * 
 * Mục đích:
 *  - Intercept tất cả HTTP requests
 *  - Extract JWT token từ Authorization header
 *  - Validate token
 *  - Set authentication vào SecurityContext (cho phép user access resource)
 * 
 * Flow:
 *  1. Request đến → Filter xử lý
 *  2. Extract Authorization: "Bearer <token>"
 *  3. Validate token
 *  4. Nếu valid → Set authentication
 *  5. Nếu invalid → Bỏ qua (user trở thành anonymous)
 *  6. Request tiếp tục tới controller
 * 
 * Note:
 *  - Extend OncePerRequestFilter: chạy 1 lần per request
 *  - Không throw exception (để filter chain tiếp tục)
 *  - Controller sau đó sẽ check authentication và return 403 nếu cần
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Step 1: Extract Authorization header
            String authHeader = request.getHeader("Authorization");
            log.debug("🔍 Checking Authorization header: {}", authHeader != null ? "Present" : "Missing");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("⚠️ No Bearer token found - proceeding as anonymous");
                filterChain.doFilter(request, response);
                return;
            }

            // Step 2: Extract token từ "Bearer <token>"
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            log.debug("✅ Token extracted from header");

            // Step 3: Validate token
            if (!jwtUtil.validateToken(token)) {
                log.warn("❌ Token validation failed");
                filterChain.doFilter(request, response);
                return;
            }

            // Step 4: Extract username từ token
            String username = jwtUtil.extractUsername(token);
            log.debug("✅ Username extracted from token: {}", username);

            // Step 5: Create Authentication object
            // Note: Authorities empty list (không cần role check ở đây)
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    new ArrayList<>()  // Empty authorities
            );

            // Step 6: Set authentication vào SecurityContext
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("✅ User authenticated: {}", username);

        } catch (Exception e) {
            log.error("❌ Error processing JWT token: {}", e.getMessage());
            // Không throw - cho request tiếp tục, controller sẽ handle
        }

        // Cho request tiếp tục tới controller
        filterChain.doFilter(request, response);
    }
}
