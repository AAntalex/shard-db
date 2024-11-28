package com.antalex.db.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = ShardDataBaseConfig.CONFIG_NAME)
@Data
public class ShardDataBaseConfig {
    public static final String CONFIG_NAME = "shard-db-config";

    private String segment;
    private Integer processorTimeOut;
    private List<ClusterConfig> clusters;
    private LiquibaseConfig liquibase;
    private HikariSettings hikari;
    private ThreadPoolConfig threadPool;
    private LockProcessorConfig lockProcessor;
    private Integer activeConnectionParallelLimit;
    private Boolean parallelRun;
    private ChecksConfig checks;
    private Integer sequenceCacheSize;
    private Integer sqlInClauseLimit;
}

