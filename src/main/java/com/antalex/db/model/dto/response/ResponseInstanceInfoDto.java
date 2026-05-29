package com.antalex.db.model.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

@Data
@Accessors(chain = true, fluent = true)
public class ResponseInstanceInfoDto {
    private String segment;
    private Boolean accessible;
    private Boolean available;
    private Short shardId;
    private Boolean mainShard;
    private OffsetDateTime lastTime;
    private String unavailableReason;
    private Integer idleConnections;
    private Integer activeConnections;
}
