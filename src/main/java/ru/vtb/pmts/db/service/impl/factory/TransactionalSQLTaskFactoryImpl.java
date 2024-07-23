package ru.vtb.pmts.db.service.impl.factory;

import ru.vtb.pmts.db.model.Shard;
import ru.vtb.pmts.db.service.api.TransactionalSQLTaskFactory;
import ru.vtb.pmts.db.service.api.TransactionalTask;
import ru.vtb.pmts.db.service.impl.TransactionalSQLTask;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

@Component
public class TransactionalSQLTaskFactoryImpl implements TransactionalSQLTaskFactory {
    private ExecutorService executorService;

    @Override
    public TransactionalTask createTask(Shard shard, Connection connection) {
        return new TransactionalSQLTask(shard, connection, executorService);
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
