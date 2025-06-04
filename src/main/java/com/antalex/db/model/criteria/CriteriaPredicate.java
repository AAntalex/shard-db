package com.antalex.db.model.criteria;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaPredicate {
    private String value;
    private Long aliasMask;
}
