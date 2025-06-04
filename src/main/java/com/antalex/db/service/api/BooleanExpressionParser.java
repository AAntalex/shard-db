package com.antalex.db.service.api;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.model.PredicateGroup;

import java.util.List;

public interface BooleanExpressionParser {
    BooleanExpression parse(String expression);
    String toString(BooleanExpression booleanExpression);
    BooleanExpression simplifying(BooleanExpression booleanExpression);
    List<PredicateGroup> getPredicateGroupsWithSimplifying(BooleanExpression booleanExpression);
    BooleanExpression assemblyExpression(List<PredicateGroup> predicateGroups);
}
