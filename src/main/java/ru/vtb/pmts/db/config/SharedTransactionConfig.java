package ru.vtb.pmts.db.config;

import lombok.Data;

@Data
public class SharedTransactionConfig {
    private Integer activeConnectionParallelLimit;
    private Boolean parallelRun;
}
