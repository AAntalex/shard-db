package com.antalex.db.service;

import java.util.stream.Stream;

public interface CriteriaCacheManager<T> {
    Stream<T> get(Object... keys);
}
