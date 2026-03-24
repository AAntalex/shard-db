package com.antalex.db.service;

import com.antalex.db.model.DataBaseInstance;

import java.sql.Connection;

public interface LockManager {
    <T extends Connection> String getLockInfo(T conn, DataBaseInstance shard);
    void setDelay(long delay);
    void setTimeOut(long timeOut);
    long getTimeOut();
    long getDelay();
}
