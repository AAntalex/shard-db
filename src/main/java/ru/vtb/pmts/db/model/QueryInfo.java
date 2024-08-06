package ru.vtb.pmts.db.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class QueryInfo {
    private Integer order;
    private Integer rows;
    private String sql;
    private Long elapsedTime;
}
