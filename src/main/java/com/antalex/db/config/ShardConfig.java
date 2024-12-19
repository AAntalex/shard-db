package com.antalex.db.config;

import lombok.Data;

@Data
public class ShardConfig {
    private Short id;
    private Boolean main;
    private DataSourceConfig dataSource;
    private RemoteConfig remote;
    private HikariSettings hikari;
    private Integer activeConnectionParallelLimit;
    private Integer percentActiveConnectionParallelLimit;
    private Integer sequenceCacheSize;
    private String segment;
    private Boolean accessible;
}
