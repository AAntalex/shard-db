package com.antalex.db.service.api;

import com.antalex.db.model.BooleanExpression;

public interface BooleanExpressionParser {
    BooleanExpression parse(String expression);
    String toString(BooleanExpression booleanExpression);
    BooleanExpression simplifying(BooleanExpression booleanExpression);
}
