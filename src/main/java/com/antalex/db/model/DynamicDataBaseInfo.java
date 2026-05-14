package com.antalex.db.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DynamicDataBaseInfo {
    private String segment;
    private Boolean accessible;
    private Boolean available;
    private OffsetDateTime lastTime;
    private String unavailableReason;
    private Integer idleConnections;
    private Integer activeConnections;
}
