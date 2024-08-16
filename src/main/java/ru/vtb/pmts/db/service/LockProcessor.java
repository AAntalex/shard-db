package ru.vtb.pmts.db.service;

import java.sql.Connection;

public interface LockProcessor<T extends Connection> {
    String getLockInfo(T conn);
}
