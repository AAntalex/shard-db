package ru.vtb.pmts.db.config;

import lombok.Data;

@Data
public class LockProcessorConfig {
    private Long delay;
    private Long timeOut;
}
