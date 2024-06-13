package com.antalex.db.utils;

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

    public static String transformCondition(String condition, Map<String, String> tokenMap) {
        if (Objects.isNull(condition)) {
            return null;
        }
        int startIdx = 0;
        int curIdx = condition.indexOf("${");

        StringBuilder resCondition;
        for(resCondition = new StringBuilder(); curIdx > -1; curIdx = condition.indexOf("${", startIdx)) {
            if (curIdx > 0) {
                resCondition.append(condition, startIdx, curIdx);
            }
            int endIdx = condition.indexOf("}", curIdx + 2);
            if (endIdx < 0) {
                resCondition.append(condition.substring(curIdx));
                return resCondition.toString();
            }
            startIdx = endIdx + 1;
            resCondition.append(getNewToken(condition.substring(curIdx + 2, endIdx), tokenMap, condition));
        }
        return resCondition.append(condition.substring(startIdx)).toString();
    }

    private static String getNewToken(String token, Map<String, String> tokenMap, String condition) {
        String alias = StringUtils.EMPTY;
        String newToken = null;
        if (token.contains(".")) {
            int aliasIdx = token.indexOf('.');
            alias = token.substring(0, aliasIdx + 1);
            token = token.substring(aliasIdx + 1);
            newToken = tokenMap.get(alias + token);
        }
        newToken = Optional.ofNullable(newToken).orElse(tokenMap.get(token));
        if (Objects.isNull(newToken)) {
            throw new IllegalArgumentException(
                    "Ошибка при разборе условия запроса \"" + condition +
                            "\": Для поля \"" + token + "\" не определено соответствие колонки в таблице!"
            );
        }
        if (newToken.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ошибка при разборе условия запроса \"" + condition +
                            "\": Для поля \"" + token + "\" не однозначно определено соответствие колонки в таблице!"
            );
        }
        return alias + newToken;
    }
}
