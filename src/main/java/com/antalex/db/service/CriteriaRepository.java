package com.antalex.db.service;

import java.util.stream.Stream;

public interface CriteriaRepository<T> {
    Stream<T> get(Object... binds);
}