package ru.vtb.pmts.db.service.api;

import ru.vtb.pmts.db.model.Shard;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public interface TransactionalSQLTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask(Shard shard, Connection connection);
    void setParallelCommit(boolean parallelCommit);
}
