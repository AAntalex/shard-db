package com.antalex.db.service.impl;

import com.antalex.db.service.abstractive.AbstractBooleanExpressionParser;

public class MathConditionParser extends AbstractBooleanExpressionParser {
    @Override
    protected String andToken() {
        return " ∧ ";
    }

    @Override
    protected String orToken() {
        return " ∨ ";
    }

    @Override
    protected String notToken() {
        return "¬";
    }

    @Override
    protected boolean isOr(String token, Character ch) {
        return ch == '∨';
    }

    @Override
    protected boolean isAnd(String token, Character ch) {
        return ch == '∧';
    }

    @Override
    protected boolean isNot(String token, Character ch) {
        return ch == '¬';
    }
}
