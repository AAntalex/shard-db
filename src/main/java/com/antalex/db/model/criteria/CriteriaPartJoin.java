package com.antalex.db.model.criteria;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.criteria.JoinType;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaPartJoin {
    private CriteriaPart part;
    private JoinType joinType;
    private Pair<String, String> joinColumns;
}
