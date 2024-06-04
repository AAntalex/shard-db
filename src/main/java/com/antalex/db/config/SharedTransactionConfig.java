package com.antalex.db.config;

import lombok.Data;

@Data
public class SharedTransactionConfig {
    private Integer activeConnectionParallelLimit;
    private Boolean parallelCommit;
}
