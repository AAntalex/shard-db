package com.antalex.db.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
public class PredicateGroup {
    private String value;
    private Long predicateMask;
    private Long signMask;

    public PredicateGroup() {}
    public PredicateGroup(String value) {
        this.value = value;
    }
    public PredicateGroup(Long predicateMask, Long signMask) {
        this.predicateMask = predicateMask;
        this.signMask = signMask;
    }
}