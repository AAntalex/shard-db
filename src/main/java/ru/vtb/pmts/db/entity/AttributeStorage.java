package ru.vtb.pmts.db.entity;

import lombok.EqualsAndHashCode;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;
import ru.vtb.pmts.db.model.Cluster;
import ru.vtb.pmts.db.model.enums.DataFormat;
import ru.vtb.pmts.db.model.enums.ShardType;
import ru.vtb.pmts.db.service.api.DataWrapper;
import lombok.Data;

@EqualsAndHashCode(callSuper = true)
@Data
public class AttributeStorage extends BaseShardEntity {
    private Long entityId;
    private String storageName;
    private String data;
    private DataFormat dataFormat;
    private ShardType shardType;
    private DataWrapper dataWrapper;
}
