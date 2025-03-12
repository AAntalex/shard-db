package com.antalex.db.service.abstractive;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.api.BooleanExpressionParser;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

public class AbstractBooleanExpressionParser implements BooleanExpressionParser {
    private static final long NEGATIVE_ZERO = ~0L;
    private final Map<String, Integer> predicates = new LinkedHashMap<>();
    private final List<String> predicateList = new ArrayList<>();

    @Override
    public BooleanExpression parse(String expression) {
        BooleanExpression booleanExpression = new BooleanExpression();
        predicates.clear();
        predicateList.clear();
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
                            if (
                                    (booleanExpression.orMask() > 0 && booleanExpression.andMask() < 0 ||
                                            booleanExpression.orMask() < 0 && booleanExpression.andMask() > 0
                                    ) && booleanExpression.orMask() != ~booleanExpression.andMask()
                            ) {
                                booleanExpression.orMask(0L);
                                booleanExpression.andMask(NEGATIVE_ZERO);
                            }
                        }
                    });
        }
    }

    private BooleanExpression simplifying(BooleanExpression booleanExpression, BooleanExpression parentExpression) {
        Long orMask = booleanExpression.orMask();
        Long andMask = booleanExpression.andMask();
        boolean uncertainty = orMask == 0L && andMask == NEGATIVE_ZERO;
        if (!uncertainty &&
                Optional.ofNullable(parentExpression)
                        .map(BooleanExpression::orMask)
                        .isPresent()
        ) {
            long positiveOrMask = getPositive(orMask);
            long positiveAndMask = getPositive(andMask);
            long positiveParentOrMask = getPositive(parentExpression.orMask());
            boolean negativeAndMask = andMask < 0;
            orMask = (positiveOrMask & getPositive(parentExpression.andMask())) != 0
                    || (positiveOrMask & positiveParentOrMask) != 0
                    ? 0L
                    : orMask;
            andMask = (positiveAndMask & positiveParentOrMask) != 0
                    ? 0L
                    : positiveAndMask & getNegative(parentExpression.andMask());
            if (orMask == 0L && andMask == 0L) {
                return null;
            }
            if (negativeAndMask) {
                andMask = ~andMask;
            }
        }
        BooleanExpression result =
                new BooleanExpression()
                        .andMask(andMask)
                        .orMask(orMask)
                        .isAnd(booleanExpression.isAnd());

        result.expressions()
                .addAll(
                        booleanExpression.expressions()
                                .stream()
                                .map(it -> this.simplifying(it, uncertainty ? parentExpression : booleanExpression))
                                .filter(Objects::nonNull)
                                .toList()
                );



        return result;
    }

    private boolean resolve(BooleanExpression booleanExpression) {
        boolean result = true;
        if (booleanExpression.andMask() == 0 && booleanExpression.orMask() == 0) {
            booleanExpression.expression().append("FALSE");
        } else if (
                booleanExpression.andMask() == NEGATIVE_ZERO &&
                        booleanExpression.orMask() == NEGATIVE_ZERO)
        {
            booleanExpression.expression().append("TRUE");
        } else {


        }


        return result;
    }

    private List<String> getPredicates(long bitMask) {
        List<String> result = new ArrayList<>();
        bitMask = getPositive(bitMask)
        for (int i = 0; i < predicateList.size(); i++) {
            if ((bitMask & (1L << i)) > 0) {
                result.add(predicateList.get(i));
            }
        }
        return result;
    }

    private long getPositive(long bitMask) {
        return bitMask < 0 ? ~bitMask : bitMask;
    }

    private long getNegative(long bitMask) {
        return bitMask > 0 ? ~bitMask : bitMask;
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
            if (!predicates.containsKey(predicate)) {
                predicateList.add(predicate);
                predicates.put(predicate, predicateList.size());
            }
            expression.orMask(1L << predicates.get(predicate) - 1);
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
