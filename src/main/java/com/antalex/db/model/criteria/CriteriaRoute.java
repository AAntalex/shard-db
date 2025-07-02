package com.antalex.db.model.criteria;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaRoute {
    private CriteriaPart mainPart;
    private Map<Integer, CriteriaPartRelation> relations = new HashMap<>();
}
