package ru.vtb.pmts.db.model;

import lombok.Data;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;

@Data
public class DataBaseInstance {
    private Short id;
    private String name;
    private String clusterName;
    private DataSource dataSource;
    private WebClient webClient;
    private DataBaseInfo dataBaseInfo;
    private DynamicDataBaseInfo dynamicDataBaseInfo;
    private String owner;
    private Integer sequenceCacheSize;
    private Boolean remote;
    private String url;
    private Integer hashCode;
    private String segment;
}
