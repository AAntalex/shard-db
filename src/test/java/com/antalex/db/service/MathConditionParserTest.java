package com.antalex.db.service;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.impl.MathConditionParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MathConditionParserTest {
    @Test
    @DisplayName("Тест упрощения логического выражения")
    void simplifyingExpressionTest() {
        MathConditionParser parser = new MathConditionParser();

        BooleanExpression expression = parser.parse("¬(A ∨ ¬B ∨ C)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "¬A ∧ B ∧ ¬C");

        expression = parser.parse("(¬A v B)&¬(A&B)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "¬A");

        expression = parser.parse("¬(A&B)v¬(B v С)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "(¬A ∨ ¬B)");

        expression = parser.parse("A&C v ¬A&C");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "C");

        expression = parser.parse("¬A v ¬B v ¬С v A v B v С");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "TRUE");

        expression = parser.parse("¬((А&В) v ¬(А&В))");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "FALSE");

        expression = parser.parse("¬А&¬(¬В v А)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "¬А ∧ В");

        expression = parser.parse("¬A&B ∨ ¬A&¬B ");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "¬A");

        expression = parser.parse("¬A&(A&¬B)&¬B");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "FALSE");

        expression = parser.parse("A&B&C ∨ A&¬B&C ∨ A&B&¬C");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "(A ∧ C ∨ A ∧ B)");

        expression = parser.parse("(¬A ∨ (B ∨ C))&((¬A ∨ B) ∨ ¬C)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "(¬A ∨ B)");

        expression = parser.parse("¬A ∨ ¬(A&B&¬B)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "TRUE");

        expression = parser.parse("¬(A ∨ ¬B) ∨ ¬(A ∨ B) ∨ A&B");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "(¬A ∨ B)");
    }
}