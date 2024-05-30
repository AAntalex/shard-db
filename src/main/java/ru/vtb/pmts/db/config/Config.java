package ru.vtb.pmts.db.config;

import liquibase.Liquibase;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.vtb.pmts.db.service.ShardDataBaseManager;

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
