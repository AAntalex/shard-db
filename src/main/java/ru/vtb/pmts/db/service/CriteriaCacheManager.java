package ru.vtb.pmts.db.service;

import java.util.stream.Stream;

public interface CriteriaCacheManager<T> {
    Stream<T> get(Object... keys);
}
