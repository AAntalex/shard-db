package ru.vtb.pmts.db.service;

import ru.vtb.pmts.db.model.DataBaseInstance;

import java.sql.Connection;

public interface LockProcessor<T extends Connection> {
    String getLockInfo(T targetConnection, DataBaseInstance instance);
}
