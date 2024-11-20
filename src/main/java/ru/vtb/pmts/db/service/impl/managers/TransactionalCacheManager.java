package ru.vtb.pmts.db.service.impl.managers;

import ru.vtb.pmts.db.service.CriteriaCacheManager;

import java.util.stream.Stream;

public class TransactionalCacheManager<T> implements CriteriaCacheManager<T> {

    @Override
    public Stream<T> get(Object... keys) {
        return null;
    }
}
