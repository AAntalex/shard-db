package com.antalex.db.config.model;

import lombok.Data;

@Data
public class ThreadPoolConfig {
    private String nameFormat;
    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Integer keepAliveTime;
}
