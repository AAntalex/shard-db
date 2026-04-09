package com.antalex.db.config.model;

import lombok.Data;

@Data
public class ChecksConfig {
    private Boolean checkShardID;
    private Boolean checkMainShard;
    private Boolean checkClusterID;
    private Boolean checkClusterName;
    private Boolean checkClusterDefault;
}
