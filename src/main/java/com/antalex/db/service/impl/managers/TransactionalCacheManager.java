package com.antalex.db.service.impl.managers;

import com.antalex.db.service.CriteriaCacheManager;

import java.util.stream.Stream;

public class TransactionalCacheManager<T> implements CriteriaCacheManager<T> {

    @Override
    public Stream<T> get(Object... keys) {
        return null;
    }
}
