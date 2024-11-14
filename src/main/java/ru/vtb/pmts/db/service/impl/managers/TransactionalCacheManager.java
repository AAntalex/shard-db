package ru.vtb.pmts.db.service.impl.managers;

import ru.vtb.pmts.db.service.CriteriaCacheManager;
import ru.vtb.pmts.db.service.CriteriaDomain;

import java.util.stream.Stream;

public class TransactionalCacheManager implements CriteriaCacheManager {

    @Override
    public Stream<CriteriaDomain> get(Object... keys) {
        return null;
    }
}
