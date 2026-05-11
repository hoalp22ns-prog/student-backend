package com.example.demo.studentbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * 🔐 SECURITY CONFIG: SecurityConfig
 * 
 * Mục đích:
 *  - Cấu hình Spring Security
 *  - Kích hoạt JWT filter
 *  - Định nghĩa endpoints public vs protected
 *  - Cấu hình CORS
 * 
 * Configuration:
 *  - SessionCreationPolicy.STATELESS: Không dùng session (JWT only)
 *  - CSRF disabled: Stateless API không cần CSRF protection
 *  - Public endpoints:
 *    ├─ /api/auth/login → Login endpoint
 *    ├─ /api/auth/register → Register endpoint
 *    └─ /api/students → Get all students (public)
 *  
 *  - Protected endpoints:
 *    ├─ /api/grades/** → Cần login
 *    └─ /api/students/{id} → PUT/DELETE cần login
 * 
 * Filter chain:
 *  1. JwtAuthFilter (trước UsernamePasswordAuthenticationFilter)
 *  2. Kiểm tra Authorization header
 *  3. Validate JWT token
 *  4. Set authentication vào SecurityContext
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    /**
     * PasswordEncoder Bean - Dùng BCrypt để hash password
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure HTTP Security
     * 
     * @param http - HttpSecurity object
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (stateless API không cần)
                .csrf(csrf -> csrf.disable())

                // Disable basic auth
                .httpBasic(basic -> basic.disable())

                // Stateless: không dùng session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Authorization rules
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints (không cần login)
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/students").permitAll()  // Get all students
                        .requestMatchers("/api/students/health/**").permitAll()  // Health checks
                        .requestMatchers("/api/students/debug/**").permitAll()  // Debug endpoints
                        .requestMatchers("/api/admin/**").permitAll()  // Admin endpoints (reset, verify, failover)

                        // Protected endpoints (cần login)
                        .requestMatchers("/api/grades/**").authenticated()  // Grades endpoints
                        .requestMatchers("POST", "/api/students").authenticated()  // Create student
                        .requestMatchers("PUT", "/api/students/**").authenticated()  // Update student
                        .requestMatchers("DELETE", "/api/students/**").authenticated()  // Delete student

                        // Tất cả request khác cần login
                        .anyRequest().authenticated()
                )

                // Add JWT filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS Configuration - Cho phép frontend gọi API
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow all origins
        config.setAllowedOrigins(Arrays.asList("*"));

        // Allow methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow headers
        config.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials (cookies, auth headers)
        config.setAllowCredentials(false);  // false vì allow all origins

        // Max age (cache preflight)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
