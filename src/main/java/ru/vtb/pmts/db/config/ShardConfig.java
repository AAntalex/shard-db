package ru.vtb.pmts.db.config;

import lombok.Data;

@Data
public class ShardConfig {
    private Short id;
    private Boolean main;
    private DataSourceConfig dataSource;
    private RemoteConfig remote;
    private HikariSettings hikari;
    private SharedTransactionConfig transactionConfig;
    private Integer sequenceCacheSize;
    private String segment;
    private Boolean accessible;
}
