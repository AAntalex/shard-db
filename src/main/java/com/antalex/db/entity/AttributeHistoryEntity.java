package com.antalex.db.entity;

import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttributeHistoryEntity extends BaseShardEntity {
    private Long entityId;
    private String attributeName;
    private OffsetDateTime time;
    private String value;
}
