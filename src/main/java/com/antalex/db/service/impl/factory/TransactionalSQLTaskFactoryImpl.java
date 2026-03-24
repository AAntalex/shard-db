package com.antalex.db.service.impl.factory;

import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.service.LockManager;
import com.antalex.db.service.api.TransactionalTask;
import com.antalex.db.service.api.TransactionalSQLTaskFactory;
import com.antalex.db.service.impl.transaction.TransactionalSQLTask;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

@Component
public class TransactionalSQLTaskFactoryImpl implements TransactionalSQLTaskFactory {
    private final LockManager lockManager;
    private ExecutorService executorService;

    TransactionalSQLTaskFactoryImpl (LockManager lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public TransactionalTask createTask(DataBaseInstance shard, Connection connection) {
        return new TransactionalSQLTask(shard, connection, executorService, lockManager);
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
