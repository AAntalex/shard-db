package ru.vtb.pmts.db.config;

import lombok.Data;

@Data
public class DataSourceConfig {
    private String driver;
    private String className;
    private String url;
    private String username;
    private String password;
    private String owner;
}
