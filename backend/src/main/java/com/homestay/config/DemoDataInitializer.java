package com.homestay.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, password FROM sys_user WHERE password NOT LIKE '$2%'");
        for (Map<String, Object> user : users) {
            Long id = ((Number) user.get("id")).longValue();
            String rawPassword = String.valueOf(user.get("password"));
            jdbcTemplate.update("UPDATE sys_user SET password = ? WHERE id = ?",
                    passwordEncoder.encode(rawPassword), id);
        }
    }
}
