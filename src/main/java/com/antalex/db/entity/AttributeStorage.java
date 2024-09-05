package com.antalex.db.entity;

import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.api.DataWrapper;
import lombok.EqualsAndHashCode;
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
