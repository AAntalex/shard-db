package com.antalex.db.model;

import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.ShardType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataStorage {
    private String name;
    private Cluster cluster;
    private ShardType shardType;
    private DataFormat dataFormat;
}
