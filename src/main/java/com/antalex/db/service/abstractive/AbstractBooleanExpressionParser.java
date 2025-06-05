package com.antalex.db.service.abstractive;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.model.PredicateGroup;
import com.antalex.db.service.api.BooleanExpressionParser;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.util.Chars;
import org.apache.logging.log4j.util.Strings;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AbstractBooleanExpressionParser implements BooleanExpressionParser {
    private static final Set<Character> ESCAPE_CHARACTERS = Set.of(Chars.LF, Chars.CR, Chars.TAB, Chars.SPACE);
    private static final Set<Character> BOOLEAN_OPERATOR_CHARACTERS = Set.of('>', '<', '=', '!');
    private static final Map<String, String> OPPOSITE_OPERATIONS = ImmutableMap.<String, String>builder()
            .put("!=", "=")
            .put("<>", "=")
            .put(">=", "<")
            .put("<=", ">")
            .build();
    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";
    protected Map<String, Integer> predicates = new HashMap<>();

    @Override
    public BooleanExpression parse(String expression) {
        BooleanExpression booleanExpression = new BooleanExpression();
        predicates.clear();
        parseCondition(expression, booleanExpression, false);
        return booleanExpression;
    }

    @Override
    public BooleanExpression simplifying(BooleanExpression booleanExpression) {
        return assemblyExpression(getPredicateGroupsWithSimplifying(booleanExpression));
    }

    @Override
    public List<PredicateGroup> getPredicateGroupsWithSimplifying(BooleanExpression booleanExpression) {
        return absorption(
                reduction(
                        absorption(
                                getPredicateGroup(booleanExpression)
                        )
                )
        );
    }

    @Override
    public BooleanExpression assemblyExpression(List<PredicateGroup> predicateGroups) {
        return assemblyExpression(
                predicateGroups
                        .stream()
                        .map(this::getPredicateExpressions)
                        .map(booleanExpressions -> assemblyExpression(booleanExpressions, true))
                        .toList(),
                false
        );
    }

    @Override
    public String toString(BooleanExpression booleanExpression) {
        if (booleanExpression.expressions().isEmpty()) {
            return (booleanExpression.isNot() ? notToken() : "") + booleanExpression.expression();
        }
        return
                (!booleanExpression.isAnd() ? "(" : "") +
                        booleanExpression
                                .expressions()
                                .stream()
                                .map(this::toString)
                                .collect(Collectors.joining(booleanExpression.isAnd() ? andToken() : orToken())) +
                        (!booleanExpression.isAnd() ? ")" : "");
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
                predicateGroup.setPredicateMask(1L << index);
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
                (left.getSignMask() & intersection) != (right.getSignMask() & intersection);
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

    private boolean reduction(PredicateGroup left, PredicateGroup right) {
        long intersection = left.getPredicateMask() & right.getPredicateMask();
        if (intersection != 0) {
            long signDifference = (intersection & left.getSignMask()) ^ (intersection & right.getSignMask());
            if (Long.bitCount(signDifference) == 1) {
                if (intersection == left.getPredicateMask()) {
                    excludeFromGroup(right, signDifference);
                    return false;
                }
                if (intersection == right.getPredicateMask()) {
                    excludeFromGroup(left, signDifference);
                    return true;
                }
            }
        }
        return false;
    }

    private void excludeFromGroup(PredicateGroup predicateGroup, Long value) {
        predicateGroup.setPredicateMask(predicateGroup.getPredicateMask() & ~value);
        predicateGroup.setSignMask(predicateGroup.getSignMask() & ~value);
    }

    private List<PredicateGroup> reduction(List<PredicateGroup> predicateGroups) {
        IntStream.range(0, predicateGroups.size())
                .forEach(i -> {
                    PredicateGroup left = predicateGroups.get(i);
                    while (IntStream.range(i+1, predicateGroups.size())
                            .mapToObj(predicateGroups::get)
                            .map(right -> reduction(left, right))
                            .reduce(false, (a, b) -> a || b));
                });
        return predicateGroups;
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
        List<PredicateGroup> result = predicateGroups
                .stream()
                .filter(it -> Objects.isNull(it.getValue()))
                .toList();
        if (result.isEmpty()) {
            return Collections.singletonList(new PredicateGroup(FALSE));
        }
        return result;
    }

    private List<BooleanExpression> getPredicateExpressions(PredicateGroup predicateGroup) {
        if (Objects.nonNull(predicateGroup.getValue())) {
            return Collections.singletonList(
                    new BooleanExpression()
                            .expression(new StringBuilder(predicateGroup.getValue()))
            );
        } else {
            return predicates.entrySet()
                    .stream()
                    .filter(it -> (predicateGroup.getPredicateMask() & (1L << it.getValue())) > 0)
                    .map(it ->
                            new BooleanExpression()
                                    .expression(new StringBuilder(it.getKey()))
                                    .isNot((predicateGroup.getSignMask() & (1L << it.getValue())) > 0)
                    )
                    .toList();
        }
    }

    private BooleanExpression assemblyExpression(List<BooleanExpression> booleanExpressions, boolean isAnd) {
        return booleanExpressions.size() == 1 ?
                booleanExpressions.get(0) :
                new BooleanExpression()
                        .isAnd(isAnd)
                        .expressions(booleanExpressions);
    }

    private void parseCondition(String condition, BooleanExpression expression, boolean recurse) {
        BooleanExpression currentExpression = expression;
        boolean isNot = expression.isNot();
        String token = Strings.EMPTY;
        char[] chars = condition.toCharArray();
        char lastChar = 0;

        for (int i = 0; i < chars.length; i++) {
            char curChar = chars[i];
            if (ESCAPE_CHARACTERS.contains(curChar)) continue;
            if (curChar == Chars.QUOTE) {
                int endPos = getEndString(chars, i);
                String currentString = String.copyValueOf(chars, i, endPos - i + 1);
                currentExpression
                        .expression()
                        .append(token.isEmpty() ? "" :  " ")
                        .append(currentString);
                i = endPos;
                lastChar = chars[i];
                continue;
            }
            if (curChar == '(') {
                int endPos = getEndParenthesis(chars, i);
                boolean needParenthesis = false;
                if (!currentExpression.expression().isEmpty()) {
                    currentExpression.expression().append("(");
                    needParenthesis = true;
                }
                parseCondition(
                        String.copyValueOf(chars, i + 1, endPos - i - 1),
                        currentExpression,
                        true
                );
                if (expression == currentExpression && !currentExpression.expressions().isEmpty()) {
                    cloneUpExpression(expression, true);
                }
                if (needParenthesis) {
                    currentExpression.expression().append(")");
                }
                i = endPos;
                lastChar = chars[i];
                continue;
            }
            String curToken = Strings.EMPTY;
            if (curChar == Chars.DQUOTE) {
                int endPos = getEndString(chars, i);
                curToken = String.copyValueOf(chars, i, endPos - i + 1);
                i = endPos;
            } else if (tokenIsStarted(curChar)) {
                int endPos = getEndWord(chars, i);
                curToken = String.copyValueOf(chars, i, endPos - i + 1).toUpperCase();
                i = endPos;
            }
            if (isAnd(curToken, curChar) || isOr(curToken, curChar)) {
                addPredicate(currentExpression);
                currentExpression = new BooleanExpression();
                concatExpression(
                        expression,
                        currentExpression,
                        isNot && isOr(curToken, curChar) || !isNot && isAnd(curToken, curChar),
                        isNot
                );
                token = Strings.EMPTY;
                lastChar = chars[i];
                continue;
            } else if (isNot(curToken, curChar)) {
                currentExpression.isNot(!currentExpression.isNot());
                lastChar = chars[i];
                continue;
            } else if (!curToken.isEmpty()) {
                currentExpression
                        .expression()
                        .append(lastChar == '.' ? "." : (token.isEmpty() ? "" :  " "))
                        .append(curToken);
                token = curToken;
                lastChar = chars[i];
                continue;
            } else if (curChar == '.' && !token.isEmpty()) {
                processAlias(token);
                lastChar = chars[i];
                continue;
            } else if (
                    BOOLEAN_OPERATOR_CHARACTERS.contains(curChar)
                            && i < chars.length-1
                            && BOOLEAN_OPERATOR_CHARACTERS.contains(chars[i+1]))
            {
                ++i;
                lastChar = chars[i];
                String operator = String.valueOf(curChar) + lastChar;
                if (OPPOSITE_OPERATIONS.containsKey(operator)) {
                    operator = OPPOSITE_OPERATIONS.get(operator);
                    currentExpression.isNot(!currentExpression.isNot());
                }
                currentExpression.expression().append(operator);
                continue;
            }
            token = Strings.EMPTY;
            currentExpression.expression().append(processChar(curChar));
            lastChar = chars[i];
        }
        if (!recurse || currentExpression != expression) {
            addPredicate(currentExpression);
        }
    }

    private int getEndWord(char[] chars, int offset) {
        for (int i = offset; i < chars.length; i++) {
            if (i == chars.length - 1 || !isTokenCharacter(chars[i+1])) {
                return i;
            }
        }
        return chars.length;
    }

    private int getEndString(char[] chars, int offset) {
        char quote = chars[offset];
        if (quote != Chars.QUOTE && quote != Chars.DQUOTE) {
            return -1;
        }
        int quotesCount = 0;
        for (int i = offset+1; i < chars.length; i++) {
            if (chars[i] == quote) {
                if (++quotesCount % 2 == 1 && (i == chars.length - 1 || chars[i + 1] != quote)) return i;
            } else {
                quotesCount = 0;
            }
        }
        throw new RuntimeException(
                String.format(
                        "Отсутствует закрывающая кавычка %c в тексте; %s",
                        quote,
                        String.copyValueOf(chars, offset, chars.length - offset)
                )
        );
    }

    private int getEndParenthesis(char[] chars, int offset) {
        char parenthesis = chars[offset];
        char endParenthesis;
        if (parenthesis == '(') {
            endParenthesis = ')';
        } else {
            return -1;
        }
        for (int i = offset+1; i < chars.length; i++) {
            if (chars[i] == endParenthesis) return i;
            if (chars[i] == parenthesis) {
                i = getEndParenthesis(chars, i);
                continue;
            }
            if (chars[i] == Chars.QUOTE) {
                i = getEndString(chars, i);
            }
        }
        throw new RuntimeException(
                String.format(
                        "Отсутствует закрывающая скобка %c в тексте; %s",
                        endParenthesis,
                        String.copyValueOf(chars, offset, chars.length - offset)
                )
        );
    }

    protected String andToken() {
        return " & ";
    }

    protected String orToken() {
        return " | ";
    }

    protected String notToken() {
        return "~";
    }

    protected boolean isOr(String token, Character ch) {
        return ch == '|';
    }

    protected boolean isAnd(String token, Character ch) {
        return ch == '&';
    }

    protected boolean isNot(String token, Character ch) {
        return ch == '~';
    }

    protected void processAlias(String token) {
    }

    protected String processChar(Character currentCharacter) {
        return String.valueOf(Character.toUpperCase(currentCharacter));
    }

    protected boolean tokenIsStarted(Character ch) {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
    }

    protected boolean isTokenCharacter(Character ch) {
        return ch >= 'a' && ch <= 'z' ||
                ch >= 'A' && ch <= 'Z' ||
                ch >= '0' && ch <= '9';
    }

    protected boolean addPredicate(BooleanExpression expression) {
        if (expression.expressions().isEmpty()) {
            String predicate = expression.expression().toString();
            if (!predicate.isBlank() && !"TRUE".equals(predicate) && !"FALSE".equals(predicate)) {
                predicates.computeIfAbsent(predicate, k -> predicates.size());
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
                    .append(isAnd ? andToken() : orToken())
                    .append("p")
                    .append(left.expressions().size());
        }
    }
}
