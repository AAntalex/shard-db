package ru.vtb.pmts.db.model.dto;

import ru.vtb.pmts.db.model.enums.DataFormat;
import ru.vtb.pmts.db.model.enums.ShardType;
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
