package com.antalex.db.model.criteria;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaElement2 {
    private String tableName;
    private String tableAlias;
    private Long columns;
    private List<CriteriaElementJoin2> joins = new ArrayList<>();
    private Cluster cluster;
    private ShardType shardType;
}
