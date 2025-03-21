package com.antalex.db;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.impl.MathConditionParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MathConditionParserTest {
    @Test
    @DisplayName("Тест упрощения логического выражения")
    void simplifyingExpressionTest() {
        MathConditionParser parser = new MathConditionParser();

        BooleanExpression expression = parser.parse("¬(A ∨ ¬B ∨ C)");
        System.out.println("RES1: " + parser.toString(parser.simplifying(expression)));

        expression = parser.parse("(¬A v B)&¬(A&B)");
        System.out.println("RES2: " + parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬(A&B)v¬(B v С)");
        System.out.println("RES3: " + parser.toString(parser.simplifying(expression)));

        expression = parser.parse("A&С v ¬A&С");
        System.out.println("RES4: " + parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬A v ¬B v ¬С v A v B v С");
        System.out.println("RES5: " + parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬((А&В) v ¬(А&В))");
        System.out.println("RES6: " + parser.toString(parser.simplifying(expression)));

        expression = parser.parse("¬А&¬(¬В v А)");
        System.out.println("RES7: " + parser.toString(parser.simplifying(expression)));
    }
}