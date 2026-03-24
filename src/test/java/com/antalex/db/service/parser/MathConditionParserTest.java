package com.antalex.db.service.parser;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.impl.parser.MathConditionParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MathConditionParserTest {
    @Test
    @DisplayName("Тест упрощения логического выражения")
    void simplifyingExpressionTest() {
        MathConditionParser parser = new MathConditionParser();

        BooleanExpression expression = parser.parse("¬(A ∨ ¬B ∨ C)");
        Assertions.assertEquals("¬A ∧ B ∧ ¬C", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("(¬A v B)&¬(A&B)");
        Assertions.assertEquals("¬A", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬(A&B)v¬(B v С)");
        Assertions.assertEquals("(¬A ∨ ¬B)", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("A&C v ¬A&C");
        Assertions.assertEquals("C", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬A v ¬B v ¬С v A v B v С");
        Assertions.assertEquals("TRUE", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬((А&В) v ¬(А&В))");
        Assertions.assertEquals("FALSE", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬А&¬(¬В v А)");
        Assertions.assertEquals("¬А ∧ В", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬A&B ∨ ¬A&¬B ");
        Assertions.assertEquals("¬A", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬A&(A&¬B)&¬B");
        Assertions.assertEquals("FALSE", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("A&B&C ∨ A&¬B&C ∨ A&B&¬C");
        Assertions.assertEquals("(A ∧ C ∨ A ∧ B)", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("(¬A ∨ (B ∨ C))&((¬A ∨ B) ∨ ¬C)");
        Assertions.assertEquals("(¬A ∨ B)", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬A ∨ ¬(A&B&¬B)");
        Assertions.assertEquals("TRUE", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬(A ∨ ¬B) ∨ ¬(A ∨ B) ∨ A&B");
        Assertions.assertEquals("(¬A ∨ B)", parser.toString(parser.simplifying(expression)));
    }
}