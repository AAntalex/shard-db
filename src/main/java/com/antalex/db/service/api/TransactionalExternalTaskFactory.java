package com.antalex.db.service.api;

import com.antalex.db.model.Shard;

import java.util.concurrent.ExecutorService;

public interface TransactionalExternalTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask(Shard shard);
}
