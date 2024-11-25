package com.antalex.db.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class QueryInfo {
    private Integer order;
    private Integer rows;
    private String sql;
    private Long elapsedTime;
}
