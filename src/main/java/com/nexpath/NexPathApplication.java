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

@SpringBootApplication
@EnableScheduling
public class NexPathApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexPathApplication.class, args);
    }

    /**
     * 🛠️ DATABASE AUTO-FIX
     * Drops the outdated Postgres check constraint that prevents new credit transaction types.
     */
    @Bean
    public CommandLineRunner databaseFixer(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Drop the constraint if it exists to allow new Enum types (SIGNUP_BONUS, etc.)
                jdbcTemplate.execute("ALTER TABLE credit_transactions DROP CONSTRAINT IF EXISTS credit_transactions_type_check");
                System.out.println("✅ Database Fix: Dropped outdated credit_transactions_type_check constraint.");
            } catch (Exception e) {
                System.err.println("❌ Database Fix Failed: " + e.getMessage());
            }
        };
    }
}
