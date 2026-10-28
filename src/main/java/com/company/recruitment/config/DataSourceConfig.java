package com.company.recruitment.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String defaultUrl,
            @Value("${spring.datasource.username:}") String defaultUsername,
            @Value("${spring.datasource.password:}") String defaultPassword,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName) {

        String databaseUrlEnv = System.getenv("DATABASE_URL");
        if (databaseUrlEnv != null && !databaseUrlEnv.isBlank()
                && (databaseUrlEnv.startsWith("postgres://") || databaseUrlEnv.startsWith("postgresql://"))) {
            try {
                log.info("Configuring DataSource from environment DATABASE_URL");
                URI uri = new URI(databaseUrlEnv);
                String userInfo = uri.getUserInfo();
                String username = defaultUsername;
                String password = defaultPassword;
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                }

                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + path + "?sslmode=prefer";

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("org.postgresql.Driver");
                config.setMaximumPoolSize(10);
                return new HikariDataSource(config);
            } catch (URISyntaxException e) {
                log.error("Failed to parse DATABASE_URL environment variable, falling back to default datasource properties", e);
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(defaultUrl);
        config.setUsername(defaultUsername);
        config.setPassword(defaultPassword);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }
}
