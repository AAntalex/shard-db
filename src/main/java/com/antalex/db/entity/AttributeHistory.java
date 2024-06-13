package com.antalex.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.antalex.db.entity.abstraction.BaseShardEntity;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class AttributeHistory extends BaseShardEntity {
    private Long entityId;
    private String attributeName;
    private LocalDateTime time;
    private String value;
}
