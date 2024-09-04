package ru.vtb.pmts.db.service.impl.factory;

import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.service.LockManager;
import ru.vtb.pmts.db.service.api.TransactionalSQLTaskFactory;
import ru.vtb.pmts.db.service.api.TransactionalTask;
import ru.vtb.pmts.db.service.impl.transaction.TransactionalSQLTask;
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
