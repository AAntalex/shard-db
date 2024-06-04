package com.antalex.db.service.api;

import com.antalex.db.model.Shard;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public interface TransactionalSQLTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask(Shard shard, Connection connection);
    void setParallelCommit(boolean parallelCommit);
}
