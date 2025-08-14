package com.antalex.db.service.api;

import com.antalex.db.service.impl.results.ResultRemoteQuery;

import java.util.concurrent.TimeUnit;

public interface QueryQueue {
    void start();
    void finish();
    ResultRemoteQuery get(long timeout, TimeUnit unit);
    ResultRemoteQuery get();
}
