package com.antalex.db.model.criteria;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.criteria.JoinType;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaElementJoin2 {
    private CriteriaElement2 element;
    private String on;
    private JoinType joinType;
    private Pair<Integer, Integer> joinColumns;
    private boolean linked;
}
