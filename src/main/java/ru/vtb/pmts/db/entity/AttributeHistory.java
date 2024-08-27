package ru.vtb.pmts.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class AttributeHistory extends BaseShardEntity {
    private Long entityId;
    private String attributeName;
    private OffsetDateTime time;
    private String value;
}
