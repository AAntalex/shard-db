package com.antalex.db.model.criteria;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.PredicateGroup;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaElement {
    private String tableName;
    private String tableAlias;
    private Long columns;
    private CriteriaElementJoin join;
    private Cluster cluster;
    private ShardType shardType;
    private Integer index;
}
