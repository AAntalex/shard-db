package com.antalex.db.utils;

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
}
