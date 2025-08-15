package com.antalex.db.service.api;

public interface QueryQueueFactory {
    QueryQueue getOrCreate(String sql, Object... binds);
}
