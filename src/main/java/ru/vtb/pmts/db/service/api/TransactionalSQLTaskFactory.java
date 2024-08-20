package ru.vtb.pmts.db.service.api;

import ru.vtb.pmts.db.model.DataBaseInstance;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public interface TransactionalSQLTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask(DataBaseInstance shard, Connection connection);
}
