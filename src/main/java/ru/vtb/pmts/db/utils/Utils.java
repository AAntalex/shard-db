package ru.vtb.pmts.db.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Utils {
    public static long addChanges(int index, Long changes) {
        return Optional.ofNullable(changes).orElse(0L) |
                (index > Long.SIZE ? 0L : (1L << (index - 1)));
    }

    public static boolean isChanged(int index, Long changes) {
        return Optional.ofNullable(changes)
                .map(it -> index > Long.SIZE || (it & (1L << (index - 1))) > 0L)
                .orElse(false);
    }

    public static String transform(String condition, Map<String, String> tokenMap, boolean hasAlias) {
        int startIdx = 0;
        int curIdx = condition.indexOf("${");
        StringBuilder resCondition = new StringBuilder();
        while (curIdx > -1) {
            resCondition.append(condition, startIdx, curIdx - 1);
            int endIdx = condition.indexOf("}", curIdx + 2);
            if (endIdx < 0) {
                resCondition.append(condition.substring(curIdx));
                return resCondition.toString();
            }
            startIdx = endIdx + 1;
            String token = condition.substring(curIdx + 2, endIdx);





            String alias = StringUtils.EMPTY;
            if (!hasAlias && token.contains(".")) {
                int aliasIdx = condition.indexOf('.');
                alias = token.substring(0, aliasIdx);
                token = token.substring(aliasIdx + 1);
            }
            String newToken = tokenMap.get(token);


            if (Objects.isNull(newToken)) {
                throw new IllegalArgumentException(
                        "Для поля \"" + token + "\" не определено соответствие колонки в таблице!"
                );
            }
            if (newToken.isEmpty()) {
                throw new IllegalArgumentException(
                        "Для поля \"" + token + "\" не однозначно определено соответствие колонки в таблице!"
                );
            }
            condition = condition.replace("${" + alias + token + "}", alias + newToken);
            curIdx = condition.indexOf("${");
        }
        return resCondition.toString();
    }

    public static String transform(String condition, Map<String, String> tokenMap) {
        return transform(condition, tokenMap, false);
    }
}
