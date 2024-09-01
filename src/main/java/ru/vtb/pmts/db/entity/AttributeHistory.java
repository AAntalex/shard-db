package ru.vtb.pmts.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;
import ru.vtb.pmts.db.model.Cluster;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true, fluent = true)
public class AttributeHistory extends BaseShardEntity {
    private Long entityId;
    private String attributeName;
    private OffsetDateTime time;
    private Object value;

    AttributeHistory cluster(Cluster cluster) {
        this.setCluster(cluster);
        return this;
    }
}
