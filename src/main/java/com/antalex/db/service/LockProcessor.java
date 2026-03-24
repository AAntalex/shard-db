package com.antalex.db.service;

import com.antalex.db.model.DataBaseInstance;

import java.sql.Connection;

public interface LockProcessor<T extends Connection> {
    String getLockInfo(T targetConnection, DataBaseInstance instance);
}
