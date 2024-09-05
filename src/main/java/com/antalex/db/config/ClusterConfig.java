package com.antalex.db.config;

import lombok.Data;

import java.util.List;

@Data
public class ClusterConfig {
    private Short id;
    private String name;
    private Boolean defaultCluster;
    private List<ShardConfig> shards;
    private HikariSettings hikari;
    private SharedTransactionConfig transactionConfig;
    private Integer sequenceCacheSize;
}
