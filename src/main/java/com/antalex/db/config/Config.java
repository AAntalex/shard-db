package com.antalex.db.config;

import com.antalex.db.service.ShardDataBaseManager;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class Config {
    private final ShardDataBaseManager dataBaseManager;

    public Config(ShardDataBaseManager dataBaseManager) {
        this.dataBaseManager = dataBaseManager;
    }

    @Bean
    public DataSource dataSource() {
        return dataBaseManager.getDataSource();
    }

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setShouldRun(false);
        return liquibase;
    }
}
