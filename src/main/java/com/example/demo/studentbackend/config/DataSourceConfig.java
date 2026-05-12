package com.example.demo.studentbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.example.demo.studentbackend.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class DataSourceConfig {

    @Value("${app.primary.enabled:true}")
    private boolean primaryEnabled;

    @Value("${spring.datasource.url:}")
    private String primaryUrl;

    @Value("${spring.datasource.username:}")
    private String primaryUsername;

    @Value("${spring.datasource.password:}")
    private String primaryPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String primaryDriver;

    @Value("${secondary.datasource.url}")
    private String secondaryUrl;

    @Value("${secondary.datasource.username}")
    private String secondaryUsername;

    @Value("${secondary.datasource.password}")
    private String secondaryPassword;

    @Value("${secondary.datasource.driver-class-name:org.postgresql.Driver}")
    private String secondaryDriver;

    @Bean(name = "primaryDatasource")
    @Primary
    public DataSource primaryDatasource() {
        if (!primaryEnabled || isBlank(primaryUrl)) {
            return secondaryDataSource();
        }

        return DataSourceBuilder.create()
                .url(primaryUrl)
                .username(primaryUsername)
                .password(primaryPassword)
                .driverClassName(primaryDriver)
                .build();
    }

    @Bean(name = "primaryJdbc")
    @Primary
    public JdbcTemplate primaryJdbcTemplate() {
        return new JdbcTemplate(primaryDatasource());
    }

    @Bean(name = "secondaryDataSource")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create()
                .url(secondaryUrl)
                .username(secondaryUsername)
                .password(secondaryPassword)
                .driverClassName(secondaryDriver)
                .build();
    }

    @Bean(name = "secondaryJdbcTemplate")
    public JdbcTemplate secondaryJdbcTemplate() {
        return new JdbcTemplate(secondaryDataSource());
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("student-service-");
        executor.initialize();
        return executor;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
