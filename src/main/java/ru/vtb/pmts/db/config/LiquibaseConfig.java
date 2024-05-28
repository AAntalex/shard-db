package ru.vtb.pmts.db.config;

import lombok.Data;

@Data
public class LiquibaseConfig {
    private String changeLogSrc;
    private String changeLogName;
    private Boolean enabled;
}
