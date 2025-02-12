package com.antalex.db;

import com.antalex.db.model.BooleanExpression;
import com.antalex.db.service.impl.SQLConditionParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
class SqlConditionParserTest extends BaseIntegrationTest {
    @Test
    @DisplayName("Тест получения по id")
    void parseTest() {
        String condition = """
                (a1.Id = ? or a1.C_COL = ?)
                and
                  (
                    upper (a1.C_DEST) = ('ASD')
                    and (
                        "a2".C_DEST like 'A1.ID = ?  AND (A2.C_DEST like \t''AAA%'' or a3.C_DATE >= :date)%'
                      or a2.C_DEST like 'Aaa%'
                      or a3.C_DATE >= ?
                    )
                    AND NOT (
                          b1."c_col" =1
                      and Not b2.C_COL2 = 2
                      or b3.C_COL3= 3
                    )
                    AND NOT (
                        c1."c_col" = 1
                     or c2.C_COL2 = 2
                     AND Not c3.C_COL3= 3
                    )
                    AND (
                          (
                               D1.C_1 = 1
                            OR C2.C_2 = 2
                          )
                      AND (D3.C_3 = 3 and D4.C_4 = 4)
                    )
                    OR Not (1=1)
                  )""";

        SQLConditionParser parser = new SQLConditionParser();
        BooleanExpression expression = parser.parse(condition);

        System.out.println("RES: " + parser.toString(expression));
    }
}
