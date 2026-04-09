package com.antalex.db.config.model;

import lombok.Data;

@Data
public class LockProcessorConfig {
    private Long delay;
    private Long timeOut;
}
