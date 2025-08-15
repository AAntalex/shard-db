package com.antalex.db.service.api;

import java.util.concurrent.TimeUnit;

public interface QueryQueue {
    void start();
    void finish();
    ResultQuery get(long timeout, TimeUnit unit);
    ResultQuery get();
}
