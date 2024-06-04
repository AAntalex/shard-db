package com.antalex.db.config;

import lombok.Data;

@Data
public class HikariSettings {
    private Integer minimumIdle;
    private Integer maximumPoolSize;
    private Long connectionTimeout;
    private Long idleTimeout;
    private Long keepAliveTime;
    private Long maxLifetime;
    private String poolName;
}
