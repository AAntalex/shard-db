package ru.vtb.pmts.db.service;

import java.util.stream.Stream;

public interface CriteriaCacheManager {
    Stream<CriteriaDomain> get(Object... keys);
}
