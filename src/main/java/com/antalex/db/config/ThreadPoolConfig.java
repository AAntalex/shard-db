package com.antalex.db.config;

import lombok.Data;

@Data
public class ThreadPoolConfig {
    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Integer keepAliveTime;
}
