package com.antalex.db;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.impl.SQLConditionParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SqlConditionParserTest {
    @Test
    @DisplayName("Тест упрощения логического выражения")
    void simplifyingExpressionTest() {
        SQLConditionParser parser = new SQLConditionParser();

        BooleanExpression expression = parser.parse("not c or (a and c) or not (a or c or not b)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "(NOT C OR A)");

        expression = parser.parse("(a or not b) and not (a or b) and (not a or c)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "NOT A AND NOT B");

        expression = parser.parse("(L or M) and ( K or M) and not N and not M");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "K AND L AND NOT M AND NOT N");

        expression = parser.parse("not (A or not B or C)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "NOT A AND B AND NOT C");

        expression = parser.parse("(not A or B) and not (A and B)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "NOT A");

        expression = parser.parse("not (A and B) or not (В or С)");
        Assertions.assertEquals(
                parser.toString(parser.simplifying(expression)),
                "(NOT A OR NOT B OR NOT С AND NOT В)"
        );

        expression = parser.parse("A and C or not A and C");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "C");

        expression = parser.parse("not A or not B or not С or A or B or С");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "TRUE");

        expression = parser.parse("not ((А and В) or not (А and В))");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "FALSE");

        expression = parser.parse("not А and not (not В or А)");
        Assertions.assertEquals(parser.toString(parser.simplifying(expression)), "NOT А AND В");
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
                and "a2".C_DEST Not like 'A1.ID  =  ?%'
                or a3.C_DATE >= ? and a1.ID = a2.ID
                """
        );
        Assertions.assertEquals(
                parser.toString(parser.simplifying(expression)),
                "(A1.ID=:1 AND NOT \"a2\".C_DEST LIKE 'A1.ID  =  ?%' AND A1.ID=:3 AND A2.C_DEST LIKE 'AAA%' " +
                        "OR A1.C_COL=:2 AND NOT \"a2\".C_DEST LIKE 'A1.ID  =  ?%' AND A1.ID=:3 AND " +
                        "A2.C_DEST LIKE 'AAA%' OR A1.ID=A2.ID AND NOT A3.C_DATE<:4)"
        );

        expression = parser.parse(
                """
                    (a1.Id = :1 or a1.C_COL = :2)
                and A1.ID = :1
                and A2.C_DEST like 'AAA%'
                and "a2".C_DEST Not like 'A1.ID  =  ?%'
                or a3.C_DATE >= :3 and a1.ID = a2.ID
                """
        );
        Assertions.assertEquals(
                parser.toString(parser.simplifying(expression)),
                "(A1.ID=:1 AND NOT \"a2\".C_DEST LIKE 'A1.ID  =  ?%' AND A2.C_DEST LIKE 'AAA%' OR " +
                        "NOT A3.C_DATE<:3 AND A1.ID=A2.ID)"
        );

        Pattern pattern = Pattern.compile("\\{:\\d+\\}");
        Matcher matcher = pattern.matcher("(A1.ID= {:12} AND NOT \"a2\".C_DEST LIKE 'A1.ID  =  ?%' AND A2.C_DEST LIKE 'AAA%' OR " +
                "NOT A3.C_DATE<{:3} AND A1.ID=A2.ID)");
        while (matcher.find()) {
            System.out.println("group: " + matcher.group());
            System.out.println("idx: " + matcher.group().substring(2, matcher.group().length() - 1));
        }
        System.out.println("RES: " + matcher.replaceAll("?"));
    }
}