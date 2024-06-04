package com.antalex.db.service.impl.factory;

import com.antalex.db.model.Shard;
import com.antalex.db.service.api.TransactionalSQLTaskFactory;
import com.antalex.db.service.api.TransactionalTask;
import com.antalex.db.service.impl.TransactionalSQLTask;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

@Component
public class TransactionalSQLTaskFactoryImpl implements TransactionalSQLTaskFactory {
    private ExecutorService executorService;
    private boolean parallelCommit;

    @Override
    public TransactionalTask createTask(Shard shard, Connection connection) {
        return new TransactionalSQLTask(shard, connection, executorService, parallelCommit);
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void setParallelCommit(boolean parallelCommit) {
        this.parallelCommit = parallelCommit;
    }
}
