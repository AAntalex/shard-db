package ru.vtb.pmts.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttributeHistoryEntity extends BaseShardEntity {
    private Long entityId;
    private String attributeName;
    private OffsetDateTime time;
    private String value;
}
