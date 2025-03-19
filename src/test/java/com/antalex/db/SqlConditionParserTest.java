package com.antalex.db;

import com.antalex.db.model.BooleanExpression;
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
        Assertions.assertEquals(parser.toString(expression), "(NOT C OR A)");
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
                parser.toString(expression),
                "(NOT \"a2\".C_DEST LIKE 'A1.ID = ?%' AND A1.ID=? AND A2.C_DEST LIKE 'AAA%' OR NOT A3.C_DATE<?)");
    }
}