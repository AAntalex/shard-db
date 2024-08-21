package ru.vtb.pmts.db.service;

import ru.vtb.pmts.db.model.DataBaseInstance;

import java.sql.Connection;

public interface LockManager {
    <T extends Connection> String getLockInfo(T conn, DataBaseInstance shard);
    void setDelay(long delay);
    void setTimeOut(long timeOut);
    long getTimeOut();
    long getDelay();
}
