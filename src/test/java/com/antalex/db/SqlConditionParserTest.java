package com.antalex.db;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.impl.MathConditionParser;
import com.antalex.db.service.impl.SQLConditionParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqlConditionParserTest {
    @Test
    @DisplayName("Тест упрощения логического выражения")
    void simplifyingExpressionTest() {
        SQLConditionParser parser = new SQLConditionParser();

        BooleanExpression expression = parser.parse("not c or (a and c) or not (a or c or not b)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "(NOT C OR A)");

        expression = parser.parse("(a or not b) and not (a or b) and (not a or c)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "NOT A AND NOT B");

        System.out.println("RES: " + parser.toString(parser.simplifying(expression)));

        expression = parser.parse("(L or M) and ( K or M) and not N and not M");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "K AND L AND NOT M AND NOT N");

        MathConditionParser mathParser = new MathConditionParser();

        expression = mathParser.parse("¬(A ∨ ¬B ∨ C)");
        System.out.println("RES1: " + mathParser.toString(mathParser.simplifying(expression)));

        expression = mathParser.parse("(¬A v B)&¬(A&B)");
        System.out.println("RES2: " + mathParser.toString(mathParser.simplifying(expression)));

        expression = mathParser.parse("¬(A&B)v¬(В v С)");
        System.out.println("RES3: " + mathParser.toString(mathParser.simplifying(expression)));

        expression = mathParser.parse("A&С v ¬A&С");
        System.out.println("RES4: " + mathParser.toString(mathParser.simplifying(expression)));

        expression = mathParser.parse("¬A v ¬B v ¬С v A v B v С");
        System.out.println("RES5: " + mathParser.toString(mathParser.simplifying(expression)));

        expression = mathParser.parse("¬((А&В) v ¬(А&В))");
        System.out.println("RES6: " + mathParser.toString(mathParser.simplifying(expression)));

        expression = mathParser.parse("¬А&¬(¬В v А)");
        System.out.println("RES7: " + mathParser.toString(mathParser.simplifying(expression)));
    }

    @Test
    @DisplayName("Тест разбора усоорвия SQL-выражения")
    void parseSqlConditionTest() {
        SQLConditionParser parser = new SQLConditionParser();
        BooleanExpression expression = parser.parse(
                """
                    (a1.Id = ? or a1.C_COL = ?)
                and A1.ID = ?
                and A2.C_DEST like 'AAA%'
                and "a2".C_DEST Not like 'A1.ID = ?%'
                or a3.C_DATE >= ?
                """
        );
        Assertions.assertEquals(
                parser.toString(parser.simplifying(expression)),
                "(NOT \"a2\".C_DEST LIKE 'A1.ID = ?%' AND A1.ID=? AND A2.C_DEST LIKE 'AAA%' OR NOT A3.C_DATE<?)");
    }
}