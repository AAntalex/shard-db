package com.antalex.db.service.api;

import com.antalex.db.model.DataBaseInstance;

import java.util.concurrent.ExecutorService;

public interface TransactionalRemoteTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask(DataBaseInstance shard);
}
