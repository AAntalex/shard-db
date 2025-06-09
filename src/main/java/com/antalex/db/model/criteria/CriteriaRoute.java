package com.antalex.db.model.criteria;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaRoute {
    private List<CriteriaPart> parts;
    private Map<Integer, CriteriaPartRelation> relations;
}
