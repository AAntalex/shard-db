package com.antalex.db.service.api;

import com.antalex.db.model.DataBaseInstance;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public interface TransactionalSQLTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask(DataBaseInstance shard, Connection connection);
}
