package com.antalex.db.service.abstractive;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.model.PredicateGroup;
import com.antalex.db.service.api.BooleanExpressionParser;
import org.apache.commons.lang3.NotImplementedException;

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
        return simplifying(booleanExpression);
    }

    private BooleanExpression simplifying(BooleanExpression booleanExpression) {
        absorption(getPredicateGroup(booleanExpression));
    }

    private List<PredicateGroup> getPredicateGroup(BooleanExpression booleanExpression) {
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
            return Collections.singletonList(predicateGroup);
        }
        List<PredicateGroup> groupList = new ArrayList<>();
        for (BooleanExpression child : booleanExpression.expressions()) {
            List<PredicateGroup> childGroupList = getPredicateGroup(child);
            String childValue = childGroupList.size() == 1 ? childGroupList.get(0).getValue() : null;
            if (childValue == null) {
                if (groupList.isEmpty() || !booleanExpression.isAnd()) {
                    groupList.addAll(childGroupList);
                } else {
                    List<PredicateGroup> predicateGroups = new ArrayList<>();
                    for (PredicateGroup group : groupList) {
                        for (PredicateGroup childGroup : childGroupList) {
                            if (!excludedAnd(group, childGroup)) {
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
                        return Collections.singletonList(new PredicateGroup(FALSE));
                    } else {
                        groupList = predicateGroups;
                    }
                }
            } else if (TRUE.equals(childValue) && !booleanExpression.isAnd() ||
                    FALSE.equals(childValue) && booleanExpression.isAnd()) {
                return Collections.singletonList(new PredicateGroup(childValue));
            }
        }
        return groupList;
    }

    private boolean excludedAnd(PredicateGroup left, PredicateGroup right) {
        Long intersection = left.getPredicateMask() & right.getPredicateMask();
        return intersection != 0 &&
                (left.getPredicateMask() & intersection) != (right.getPredicateMask() & intersection);
    }

    private boolean excludedOr(PredicateGroup left, PredicateGroup right) {
        return left.getPredicateMask().equals(right.getPredicateMask()) &&
                Long.bitCount(left.getPredicateMask()) == 1 &&
                !left.getSignMask().equals(right.getSignMask());
    }

    private void absorption(PredicateGroup left, PredicateGroup right) {
        long intersection = left.getPredicateMask() & right.getPredicateMask();
        if (intersection == left.getPredicateMask() && left.getSignMask() == (intersection & right.getSignMask())) {
            right.setValue(FALSE);
        }
        if (intersection == right.getPredicateMask() && right.getSignMask() == (intersection & left.getSignMask())) {
            left.setValue(FALSE);
        }
    }

    private void reduction(PredicateGroup left, PredicateGroup right) {
        long intersection = left.getPredicateMask() & right.getPredicateMask();
        if (intersection != 0) {
            long signDifference = (intersection & left.getSignMask()) ^ (intersection & right.getSignMask());
            if (Long.bitCount(signDifference) == 1) {
                if (intersection == left.getPredicateMask()) {
                    excludeFromGroup(right, signDifference);
                }
                if (intersection == right.getPredicateMask()) {
                    excludeFromGroup(left, signDifference);
                }
            }
        }
    }

    private void excludeFromGroup(PredicateGroup predicateGroup, Long value) {
        predicateGroup.setPredicateMask(predicateGroup.getPredicateMask() & ~value);
        predicateGroup.setSignMask(predicateGroup.getSignMask() & ~value);
    }

    private List<PredicateGroup> absorption(List<PredicateGroup> predicateGroups) {
        for (int i = 0; i < predicateGroups.size(); i++) {
            PredicateGroup currentPredicateGroup = predicateGroups.get(i);
            if (currentPredicateGroup.getValue() == null) {
                for (int j = i + 1; j < predicateGroups.size(); j++) {
                    PredicateGroup predicateGroup = predicateGroups.get(j);
                    if (predicateGroup.getValue() == null) {
                        if (excludedOr(currentPredicateGroup, predicateGroup)) {
                            return Collections.singletonList(new PredicateGroup(TRUE));
                        }
                        absorption(currentPredicateGroup, predicateGroup);
                        if (FALSE.equals(currentPredicateGroup.getValue())) {
                            break;
                        }
                    } else if (TRUE.equals(predicateGroup.getValue())) {
                        return Collections.singletonList(new PredicateGroup(TRUE));
                    }
                }
            } else if (TRUE.equals(currentPredicateGroup.getValue())) {
                return Collections.singletonList(new PredicateGroup(TRUE));
            }
        }
        return predicateGroups
                .stream()
                .filter(it -> Objects.isNull(it.getValue()))
                .toList();
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
