package com.antalex.db.service.abstractive;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.api.BooleanExpressionParser;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

public class AbstractBooleanExpressionParser implements BooleanExpressionParser {
    protected final Map<String, Integer> predicates = new LinkedHashMap<>();

    @Override
    public BooleanExpression parse(String expression) {
        BooleanExpression booleanExpression = new BooleanExpression();
        predicates.clear();
        parseCondition(expression, booleanExpression, false);
        calcBitMask(booleanExpression);
        return booleanExpression;
    }

    private void calcBitMask(BooleanExpression booleanExpression) {
        if (booleanExpression.orMask() == null) {
            booleanExpression
                    .expressions()
                    .forEach(child -> {
                        calcBitMask(child);
                        long orMask = child.isNot() ? ~child.orMask() : child.orMask();
                        long andMask = child.isNot() ? ~child.andMask() : child.andMask();
                        if (booleanExpression.orMask() == null) {
                            booleanExpression.orMask(orMask);
                            booleanExpression.andMask(andMask);
                        } else {
                            if (booleanExpression.isAnd()) {
                                booleanExpression.orMask(booleanExpression.orMask() & orMask);
                                booleanExpression.andMask(booleanExpression.andMask() & andMask);
                            } else {
                                booleanExpression.orMask(booleanExpression.orMask() | orMask);
                                booleanExpression.andMask(booleanExpression.andMask() | andMask);
                            }
                        }
                    });
        }
    }

    private String interpret(BooleanExpression booleanExpression) {
        String operand = "AND";
        if (booleanExpression.orMask() < 0 || booleanExpression.orMask() > 0 & booleanExpression.andMask() < 0) {
            operand = "OR";
        }
    }

    @Override
    public String toString(BooleanExpression booleanExpression) {
        throw new NotImplementedException();
    }

    protected void parseCondition(String condition, BooleanExpression expression, boolean recurse) {
        throw new NotImplementedException();
    }

    protected int addPredicate(BooleanExpression expression) {
        if (expression.expressions().isEmpty()) {
            String predicate = expression.expression().toString();
            expression.orMask(1L << predicates.computeIfAbsent(predicate, k -> predicates.size() + 1) - 1);
            expression.andMask(~expression.orMask());
            return predicates.get(predicate);
        }
        return 0;
    }

    protected void cloneUpExpression(BooleanExpression source, boolean isAnd) {
        BooleanExpression child = new BooleanExpression();
        child
                .expression(source.expression())
                .expressions(source.expressions())
                .isAnd(source.isAnd())
                .isNot(source.isNot())
                .orMask(source.orMask())
                .andMask(source.andMask());
        source
                .expression(new StringBuilder())
                .expressions(new ArrayList<>())
                .isAnd(isAnd)
                .isNot(false)
                .orMask(null)
                .andMask(null);
        source.expressions().add(child);
        source.expression().append("p1");
    }

    protected void concatExpression(BooleanExpression left, BooleanExpression right, boolean isAnd, boolean isNot) {
        right.isNot(isNot && !right.isNot() || !isNot && right.isNot());
        if (left.expressions().isEmpty() ||
                !isNot && left.isAnd() && !isAnd ||
                isNot && !left.isAnd() && isAnd)
        {
            cloneUpExpression(left, isAnd);
        }
        if (!isNot && !left.isAnd() && isAnd || isNot && left.isAnd() && !isAnd) {
            concatExpression(left.expressions().get(left.expressions().size() - 1), right, isAnd, false);
        }
        if (left.isAnd() == isAnd) {
            left.expressions().add(right);
            left.expression()
                    .append(isAnd ? " AND " : " OR ")
                    .append("p")
                    .append(left.expressions().size());
        }
    }
}
