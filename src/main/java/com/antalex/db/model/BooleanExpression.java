package com.antalex.db.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class BooleanExpression {
    private StringBuilder expression = new StringBuilder();
    private List<BooleanExpression> expressions = new ArrayList<>();
    private List<PredicateGroup> predicateGroups = new ArrayList<>();;
    private boolean isAnd;
    private boolean isNot;
}