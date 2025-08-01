package com.antalex.db.service.parser;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.impl.parser.SQLConditionParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqlConditionParserTest {
    @Test
    @DisplayName("Тест упрощения логического SQL-выражения")
    void simplifyingExpressionTest() {
        SQLConditionParser parser = new SQLConditionParser();

        BooleanExpression expression = parser.parse("not c or (a and c) or not (a or c or not b)");
        Assertions.assertEquals("(NOT C OR A)", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("(a or not b) and not (a or b) and (not a or c)");
        Assertions.assertEquals("NOT A AND NOT B", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("(L or M) and ( K or M) and not N and not M");
        Assertions.assertEquals("K AND L AND NOT M AND NOT N", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("not (A or not B or C)");
        Assertions.assertEquals("NOT A AND B AND NOT C", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("(not A or B) and not (A and B)");
        Assertions.assertEquals("NOT A", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("not (A and B) or not (В or С)");
        Assertions.assertEquals(
                "(NOT A OR NOT B OR NOT С AND NOT В)",
                parser.toString(parser.simplifying(expression))
        );

        expression = parser.parse("A and C or not A and C");
        Assertions.assertEquals("C", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("not A or not B or not С or A or B or С");
        Assertions.assertEquals("TRUE", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("not ((А and В) or not (А and В))");
        Assertions.assertEquals("FALSE", parser.toString(parser.simplifying(expression)));

        expression = parser.parse("not А and not (not В or А)");
        Assertions.assertEquals("NOT А AND В", parser.toString(parser.simplifying(expression)));
    }

    @Test
    @DisplayName("Тест разбора условия SQL-выражения")
    void parseSqlConditionTest() {
        SQLConditionParser parser = new SQLConditionParser();
        BooleanExpression expression = parser.parse(
                """
                    (a1.Id = ? or a1.C_COL = ?)
                and A1.ID = ?
                and A2.C_DEST like 'AAA%'
                and "a2".C_DEST Not like 'A1.ID  =  ?%'
                or a3.C_DATE >= ? and a1.ID = a2.ID
                """
        );
        Assertions.assertEquals(
                "(A1.ID={:3} AND A1.ID={:1} AND NOT \"a2\".C_DEST LIKE 'A1.ID  =  ?%' AND " +
                        "A2.C_DEST LIKE 'AAA%' OR A1.ID={:3} AND A1.C_COL={:2} AND " +
                        "NOT \"a2\".C_DEST LIKE 'A1.ID  =  ?%' AND A2.C_DEST LIKE 'AAA%' " +
                        "OR NOT A3.C_DATE<{:4} AND A1.ID=A2.ID)",
                parser.toString(parser.simplifying(expression))
        );

        expression = parser.parse(
                """
                    (a1.Id = {:1} or a1.C_COL = {:2})
                and A1.ID = {:1}
                and A2.C_DEST like 'AAA%'
                and "a2".C_DEST Not like 'A1.ID  =  ?%'
                or a3.C_DATE >= {:3} and a1.ID = a2.ID
                """
        );
        Assertions.assertEquals(
                "(A1.ID={:1} AND NOT \"a2\".C_DEST LIKE 'A1.ID  =  ?%' AND A2.C_DEST LIKE 'AAA%' OR " +
                        "NOT A3.C_DATE<{:3} AND A1.ID=A2.ID)",
                parser.toString(parser.simplifying(expression))
        );
    }
}