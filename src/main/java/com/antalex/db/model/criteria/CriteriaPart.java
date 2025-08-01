package com.antalex.db.model.criteria;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.PredicateGroup;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaPart {
    private String from;
    private Long columns;
    private Cluster cluster;
    private Long aliasMask;
    private String outerJoinKey;
    private PredicateGroup predicateGroup;
    private List<Pair<String, String>> joinColumns = new ArrayList<>();
}
