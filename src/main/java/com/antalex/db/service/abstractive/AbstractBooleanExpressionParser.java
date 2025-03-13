package com.antalex.db.service.abstractive;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.model.PredicateGroup;
import com.antalex.db.service.api.BooleanExpressionParser;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class AbstractBooleanExpressionParser implements BooleanExpressionParser {
    private static final long NEGATIVE_ZERO = ~0L;
    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";
    private final Map<String, Integer> predicates = new LinkedHashMap<>();
    protected final List<String> predicateList = new ArrayList<>();

    @Override
    public BooleanExpression parse(String expression) {
        BooleanExpression booleanExpression = new BooleanExpression();
        predicates.clear();
        predicateList.clear();
        parseCondition(expression, booleanExpression, false);
        resolve(booleanExpression);
        return booleanExpression;
    }

    private void resolve(BooleanExpression booleanExpression) {
        if (booleanExpression.predicateGroups().isEmpty()) {
            if (booleanExpression.expressions().isEmpty()) {
                String predicate = booleanExpression.expression().toString();
                PredicateGroup predicateGroup = new PredicateGroup();
                if (TRUE.equals(predicate) || FALSE.equals(predicate)) {
                    predicateGroup.setValue(predicate);
                } else {
                    Integer index = predicates.get(predicate);
                    if (index == null) {
                        throw new IllegalArgumentException("Отсутствует предикат " + predicate);
                    }
                    predicateGroup.setPredicateMask(1L << (index - 1));
                    predicateGroup.setSignMask(booleanExpression.isNot() ? predicateGroup.getPredicateMask() : 0L);
                }
                booleanExpression.predicateGroups().add(predicateGroup);
            } else {
                for (BooleanExpression child : booleanExpression.expressions()) {
                    resolve(child);
                    String childValue = child.predicateGroups().size() == 1 ?
                            child.predicateGroups().get(0).getValue() :
                            null;
                    if (childValue == null) {
                        if (booleanExpression.predicateGroups().isEmpty() || !booleanExpression.isAnd()) {
                            booleanExpression.predicateGroups().addAll(child.predicateGroups());
                        } else {
                            List<PredicateGroup> predicateGroups = new ArrayList<>();
                            for (PredicateGroup group : booleanExpression.predicateGroups()) {
                                for (PredicateGroup childGroup : child.predicateGroups()) {
                                    Long intersection = group.getPredicateMask() & childGroup.getPredicateMask();
                                    if (
                                            intersection == 0 ||
                                                    (group.getPredicateMask() & intersection) ==
                                                            (childGroup.getPredicateMask() & intersection))
                                    {
                                        predicateGroups.add(
                                                new PredicateGroup(
                                                        group.getPredicateMask() |
                                                                childGroup.getPredicateMask(),
                                                        group.getSignMask() | childGroup.getSignMask()
                                                )
                                        );
                                    }
                                }
                            }
                            if (predicateGroups.isEmpty()) {
                                setGroupValue(booleanExpression, FALSE);
                            } else {
                                booleanExpression.predicateGroups(predicateGroups);
                            }
                        }
                    } else if (TRUE.equals(childValue) && !booleanExpression.isAnd() ||
                                FALSE.equals(childValue) && booleanExpression.isAnd()) {
                            setGroupValue(booleanExpression, childValue);
                            return;
                    }
                }
            }
        }
    }

    private void setGroupValue(BooleanExpression booleanExpression, String value) {
        booleanExpression.predicateGroups(Collections.singletonList(new PredicateGroup(value)));
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

    private String normalize(BooleanExpression expression, String predicate, String operand, String oppositeOperand)
    {
        if (predicate.contains(oppositeOperand)) {
            expression.isNot(!expression.isNot());
            predicate = predicate.replace(oppositeOperand, operand);
            expression.expression(new StringBuilder(predicate));
        }
        return predicate;
    }

    private String normalizePredicate(BooleanExpression expression, String predicate) {
        predicate = normalize(expression, predicate, "=", "<>");
        predicate = normalize(expression, predicate, "=", "!=");
        predicate = normalize(expression, predicate, ">", "<=");
        predicate = normalize(expression, predicate, "<", ">=");
        return predicate;
    }

    protected boolean addPredicate(BooleanExpression expression) {
        if (expression.expressions().isEmpty()) {
            String predicate = expression.expression().toString();
            if (!predicate.isBlank() && !"TRUE".equals(predicate) && !"FALSE".equals(predicate)) {
                predicate = normalizePredicate(expression, predicate);
                Integer index = predicates.get(predicate);
                if (index == null) {
                    predicateList.add(predicate);
                    index = predicateList.size();
                    predicates.put(predicate, index);
                }
                return true;
            }
        }
        return false;
    }

    protected void cloneUpExpression(BooleanExpression source, boolean isAnd) {
        BooleanExpression child = new BooleanExpression();
        child
                .expression(source.expression())
                .expressions(source.expressions())
                .isAnd(source.isAnd())
                .isNot(source.isNot());
        source
                .expression(new StringBuilder("p1"))
                .expressions(new ArrayList<>())
                .isAnd(isAnd)
                .isNot(false)
                .expressions()
                .add(child);
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
