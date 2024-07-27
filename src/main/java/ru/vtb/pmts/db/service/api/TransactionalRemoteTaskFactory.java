package ru.vtb.pmts.db.service.api;

import ru.vtb.pmts.db.model.Shard;

import java.util.concurrent.ExecutorService;

public interface TransactionalRemoteTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask(Shard shard);
}
