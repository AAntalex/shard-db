package com.antalex.db.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true, fluent = true)
public class BooleanExpression {
    private StringBuilder expression = new StringBuilder();
    private List<BooleanExpression> expressions = new ArrayList<>();
    private Set<String> aliases = new HashSet<>();
    private boolean isAnd;
    private boolean isNot;
    private Long orMask;
    private Long andMask;
}