package ru.vtb.pmts.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;
import ru.vtb.pmts.db.model.enums.DataFormat;
import ru.vtb.pmts.db.model.enums.ShardType;
import ru.vtb.pmts.db.service.api.DataWrapper;

@EqualsAndHashCode(callSuper = true)
@Data
public class AttributeHistory extends BaseShardEntity {
    private Long entityId;
    private String attributeName;
    private String data;
    private DataFormat dataFormat;
    private ShardType shardType;
    private DataWrapper dataWrapper;
}
