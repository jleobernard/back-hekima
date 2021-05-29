package com.leo.hekima.configuration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.leo.hekima.repository")
public class DBConfiguration extends AbstractR2dbcConfiguration {
    @Value("${db.host}") private String dbHost;
    @Value("${db.port}") private int dbPort;
    @Value("${db.dbname}") private String db;
    @Value("${db.username}") private String username;
    @Value("${db.password}") private String password;

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "pool")
                .option(PROTOCOL, "postgresql") // driver identifier, PROTOCOL is delegated as DRIVER by the pool.
                .option(HOST, dbHost)
                .option(PORT, dbPort)
                .option(USER, username)
                .option(PASSWORD, password)
                .option(DATABASE, db)
                .build());
    }
}
