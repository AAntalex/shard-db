package com.antalex.db.service.impl;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.abstractive.AbstractBooleanExpressionParser;
import org.apache.logging.log4j.util.Chars;
import org.apache.logging.log4j.util.Strings;

import java.util.*;
import java.util.stream.Collectors;

public class SQLConditionParser extends AbstractBooleanExpressionParser {
    private static final Set<Character> ESCAPE_CHARACTERS = Set.of(Chars.LF, Chars.CR, Chars.TAB, Chars.SPACE);

    private final List<Set<String>> aliases = new ArrayList<>();
    private Set<String> currentAliases = new HashSet<>();

    @Override
    public BooleanExpression parse(String expression) {
        aliases.clear();
        currentAliases.clear();
        return super.parse(expression);
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

    @Override
    protected void parseCondition(String condition, BooleanExpression expression, boolean recurse) {
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
            }
            if (curChar == '_' || curChar >= 'a' && curChar <= 'z' || curChar >= 'A' && curChar <= 'Z') {
                int endPos = getEndWord(chars, i);
                curToken = String.copyValueOf(chars, i, endPos - i + 1).toUpperCase();
                i = endPos;
            }
            if (!curToken.isEmpty()) {
                if ("OR".equals(curToken) || "AND".equals(curToken)) {
                    addPredicate(currentExpression);
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
                currentAliases.add(token);
                lastChar = chars[i];
                continue;
            }
            token = Strings.EMPTY;
            currentExpression.expression().append(Character.toUpperCase(curChar));
            lastChar = chars[i];
        }
        if (!recurse || currentExpression != expression) {
            addPredicate(currentExpression);
        }
    }

    @Override
    protected boolean addPredicate(BooleanExpression expression) {
        if (super.addPredicate(expression) && aliases.size() < predicateList.size()) {
            aliases.add(currentAliases);
            this.currentAliases = new HashSet<>();
            return true;
        }
        return false;
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
}
