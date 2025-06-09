package com.antalex.db.model.criteria;

import com.antalex.db.model.Cluster;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaPart {
    private String from;
    private Long columns;
    private Cluster cluster;
    private Long aliasMask;
}
