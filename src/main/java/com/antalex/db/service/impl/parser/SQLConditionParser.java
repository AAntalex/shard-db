package com.antalex.db.service.impl.parser;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.abstractive.AbstractBooleanExpressionParser;

import java.util.*;

public class SQLConditionParser extends AbstractBooleanExpressionParser {
    private final List<Set<String>> aliases = new ArrayList<>();
    private Set<String> currentAliases = new HashSet<>();
    private int bindIndex = 0;

    @Override
    public BooleanExpression parse(String expression) {
        aliases.clear();
        currentAliases.clear();
        bindIndex = 0;
        return super.parse(expression);
    }

    @Override
    protected String andToken() {
        return " AND ";
    }

    @Override
    protected String orToken() {
        return " OR ";
    }

    @Override
    protected String notToken() {
        return "NOT ";
    }

    @Override
    protected boolean isOr(String token, Character ch) {
        return "OR".equals(token);
    }

    @Override
    protected boolean isAnd(String token, Character ch) {
        return "AND".equals(token);
    }

    @Override
    protected boolean isNot(String token, Character ch) {
        return "NOT".equals(token);
    }

    @Override
    protected void processAlias(String token) {
        currentAliases.add(token);
    }

    @Override
    protected String processChar(Character currentCharacter) {
        if (currentCharacter == '?') {
            return "{:" + ++bindIndex + "}";
        }
        return super.processChar(currentCharacter);
    }

    @Override
    protected boolean tokenIsStarted(Character ch) {
        return ch == '_' || super.tokenIsStarted(ch);
    }

    @Override
    protected boolean isTokenCharacter(Character ch) {
        return ch == '_' || ch == '$' || ch == '#' || super.isTokenCharacter(ch);
    }


    @Override
    protected boolean addPredicate(BooleanExpression expression) {
        if (super.addPredicate(expression) && aliases.size() < predicates.size()) {
            aliases.add(currentAliases);
            this.currentAliases = new HashSet<>();
            return true;
        }
        currentAliases.clear();
        return false;
    }

    public Map<String, Integer> getPredicates() {
        return predicates;
    }

    public void setPredicates(Map<String, Integer> predicates) {
        this.predicates = predicates;
    }

    public List<Set<String>> getAliases() {
        return aliases;
    }
}
