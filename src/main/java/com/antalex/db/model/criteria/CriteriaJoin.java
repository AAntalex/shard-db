package com.antalex.db.model.criteria;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.criteria.JoinType;
import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaJoin {
    private CriteriaElement element;
    private String on;
    private JoinType joinType;
    private Pair<String, String> joinColumns;
    private boolean linked;
}
