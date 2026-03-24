package com.antalex.db.model.dto;

import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.ShardType;
import lombok.Builder;
import lombok.Data;

import javax.persistence.FetchType;

@Data
@Builder
public class StorageDto {
    private String name;
    private String cluster;
    private ShardType shardType;
    private DataFormat dataFormat;
    private FetchType fetchType;
    private Boolean isUsed;
}
