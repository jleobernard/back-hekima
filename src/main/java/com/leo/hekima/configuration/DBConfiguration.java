package com.leo.hekima.configuration;

import com.leo.hekima.model.*;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
        final var cf = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "pool")
                .option(PROTOCOL, "postgresql") // driver identifier, PROTOCOL is delegated as DRIVER by the pool.
                .option(HOST, dbHost)
                .option(PORT, dbPort)
                .option(USER, username)
                .option(PASSWORD, password)
                .option(DATABASE, db)
                .build());
        ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(cf)
                .maxIdleTime(Duration.ofMinutes(30))
                .initialSize(1)
                .maxSize(10)
                .maxCreateConnectionTime(Duration.ofSeconds(5))
                .build();
        return new ConnectionPool(configuration);
    }
    @Bean
    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new NoteFilesWriter());
        converters.add(new NoteFilesReader());
        converters.add(new NoteSubsWriter());
        converters.add(new NoteSubsReader());
        converters.add(new LanguageWriter());
        converters.add(new LanguageReader());
        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }
}
