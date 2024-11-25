package com.antalex.db.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class TransactionInfo {
    private UUID uuid;
    private OffsetDateTime executeTime;
    private Integer chunks;
    private Long elapsedTime;
    private Long allElapsedTime;
    private Boolean failed;
    private String error;
    private DataBaseInstance shard;
    private List<QueryInfo> queries;
}
