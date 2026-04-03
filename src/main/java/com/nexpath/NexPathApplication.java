package com.nexpath;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class NexPathApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexPathApplication.class, args);
    }

    /**
     *  DATABASE AUTO-FIX
     * Drops the outdated Postgres check constraint that prevents new credit transaction types.
     */
    @Bean
    public CommandLineRunner databaseFixer(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Drop constraints for opportunity_posts and credit_transactions to prevent save failures due to Enum changes
                jdbcTemplate.execute("ALTER TABLE opportunity_posts DROP CONSTRAINT IF EXISTS opportunity_posts_role_type_check");
                jdbcTemplate.execute("ALTER TABLE opportunity_posts DROP CONSTRAINT IF EXISTS opportunity_posts_status_check");
                jdbcTemplate.execute("ALTER TABLE opportunity_posts DROP CONSTRAINT IF EXISTS role_type_check");
                jdbcTemplate.execute("ALTER TABLE opportunity_posts DROP CONSTRAINT IF EXISTS status_check");
                jdbcTemplate.execute("ALTER TABLE credit_transactions DROP CONSTRAINT IF EXISTS credit_transactions_type_check");
                jdbcTemplate.execute("ALTER TABLE credit_transactions DROP CONSTRAINT IF EXISTS type_check");
                System.out.println(" Database Fix: Dropped outdated Enum constraints.");
            } catch (Exception e) {
                System.err.println("❌ Database Fix Failed: " + e.getMessage());
            }
        };
    }
}
