package com.antalex.db.service.impl.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.service.api.TransactionalRemoteTaskFactory;
import com.antalex.db.service.api.TransactionalTask;
import org.springframework.stereotype.Component;
import com.antalex.db.service.impl.transaction.TransactionalRemoteTask;

import java.util.concurrent.ExecutorService;

@Component
public class TransactionalRemoteTaskFactoryImpl implements TransactionalRemoteTaskFactory {
    private final ObjectMapper objectMapper;
    private ExecutorService executorService;

    TransactionalRemoteTaskFactoryImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TransactionalTask createTask(DataBaseInstance shard) {
        return new TransactionalRemoteTask(shard, objectMapper, executorService);
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
