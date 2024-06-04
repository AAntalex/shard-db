package com.antalex.db.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataBaseInfo {
    private short shardId;
    private boolean mainShard;
    private short clusterId;
    private String clusterName;
    private boolean defaultCluster;
}
