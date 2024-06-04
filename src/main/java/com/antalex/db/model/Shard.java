package com.antalex.db.model;

import lombok.Data;

import javax.sql.DataSource;

@Data
public class Shard {
    private Short id;
    private String name;
    private DataSource dataSource;
    private DataBaseInfo dataBaseInfo;
    private DynamicDataBaseInfo dynamicDataBaseInfo;
    private String owner;
    private Integer sequenceCacheSize;
    private Boolean external;
    private String url;
    private Integer hashCode;
    private String segment;
}
