package com.antalex.db.service.impl;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.api.BooleanExpressionParser;
import org.apache.logging.log4j.util.Chars;
import org.apache.logging.log4j.util.Strings;

import java.util.*;
import java.util.stream.Collectors;

public class SQLConditionParser implements BooleanExpressionParser {
    private final Map<String, Integer> predicates = new HashMap<>();

    @Override
    public BooleanExpression parse(String expression) {
        BooleanExpression booleanExpression = new BooleanExpression();
        predicates.clear();
        parseCondition(expression, booleanExpression);
        return booleanExpression;
    }

    @Override
    public String toString(BooleanExpression booleanExpression) {
        if (booleanExpression.expressions().isEmpty()) {
            return (booleanExpression.isNot() ? "NOT " : "") + booleanExpression.expression();
        }
        return
                (!booleanExpression.isAnd() ? "(" : "") +
                        booleanExpression
                                .expressions()
                                .stream()
                                .map(this::toString)
                                .collect(Collectors.joining(booleanExpression.isAnd() ? " AND " : " OR ")) +
                        (!booleanExpression.isAnd() ? ")" : "");
    }

    private void parseCondition(String condition, BooleanExpression expression) {
        Set<Character> escapeCharacters = Set.of(Chars.LF, Chars.CR, Chars.TAB, Chars.SPACE);
        BooleanExpression currentExpression = expression;
        boolean isNot = expression.isNot();
        String token = Strings.EMPTY;
        char[] chars = condition.toCharArray();
        char lastChar = 0;

        for (int i = 0; i < chars.length; i++) {
            char curChar = chars[i];
            if (escapeCharacters.contains(curChar)) continue;
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
                parseCondition(String.copyValueOf(chars, i + 1, endPos - i - 1), currentExpression);
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
            }
            if (curChar == '_' || curChar >= 'a' && curChar <= 'z' || curChar >= 'A' && curChar <= 'Z') {
                int endPos = getEndWord(chars, i);
                curToken = String.copyValueOf(chars, i, endPos - i + 1).toUpperCase();
                i = endPos;
            }
            if (!curToken.isEmpty()) {
                if ("OR".equals(curToken) || "AND".equals(curToken)) {
                    setBitMask(currentExpression);
                    currentExpression = new BooleanExpression();
                    concatExpression(
                            expression,
                            currentExpression,
                            isNot && "OR".equals(curToken) || !isNot && "AND".equals(curToken),
                            isNot
                    );
                    token = Strings.EMPTY;
                } else if ("NOT".equals(curToken)) {
                    currentExpression.isNot(!currentExpression.isNot());
                    token = Strings.EMPTY;
                } else {
                    currentExpression
                            .expression()
                            .append(lastChar == '.' ? "." : (token.isEmpty() ? "" :  " "))
                            .append(curToken);
                    token = curToken;
                }
                lastChar = chars[i];
                continue;
            }
            if (curChar == '.' && !token.isEmpty()) {
                System.out.println("ALIAS: " + token);
                currentExpression.aliases().add(token);
                lastChar = chars[i];
                continue;
            }
            token = Strings.EMPTY;
            currentExpression.expression().append(Character.toUpperCase(curChar));
            lastChar = chars[i];
        }
        setBitMask(currentExpression);
    }

    private void setBitMask(BooleanExpression expression) {
        String predicate = expression.expression().toString();
        if (!predicate.isBlank()) {
            expression.orMask(1L << predicates.computeIfAbsent(predicate, k -> predicates.size() + 1));
            expression.andMask(~expression.orMask());
        }
    }

    private int getEndWord(char[] chars, int offset) {
        for (int i = offset; i < chars.length; i++) {
            if (
                    i == chars.length - 1 ||
                            !(chars[i+1] >= 'a' && chars[i+1] <= 'z' ||
                                    chars[i+1] >= 'A' && chars[i+1] <= 'Z' ||
                                    chars[i+1] >= '0' && chars[i+1] <= '9' ||
                                    chars[i+1] == '_' || chars[i+1] == '$' || chars[i+1] == '#'))
            {
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
                        "Отсутсвует закрывающая кавычка %c в тексте; %s",
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
                        "Отсутсвует закрывающая скобка %c в тексте; %s",
                        endParenthesis,
                        String.copyValueOf(chars, offset, chars.length - offset)
                )
        );
    }

    private void cloneUpExpression(BooleanExpression source, boolean isAnd) {
        BooleanExpression child = new BooleanExpression();
        child
                .expression(source.expression())
                .expressions(source.expressions())
                .aliases(source.aliases())
                .isAnd(source.isAnd())
                .isNot(source.isNot());
        source.aliases(new HashSet<>());
        source.expression(new StringBuilder());
        source.expressions(new ArrayList<>());
        source.expressions().add(child);
        source.isAnd(isAnd);
        source.isNot(false);
        source.expression().append("p1");
    }

    private void concatExpression(BooleanExpression left, BooleanExpression right, boolean isAnd, boolean isNot) {
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
