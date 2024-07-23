package ru.vtb.pmts.db.service.impl.factory;

import ru.vtb.pmts.db.model.Shard;
import ru.vtb.pmts.db.service.api.TransactionalExternalTaskFactory;
import ru.vtb.pmts.db.service.api.TransactionalTask;
import org.springframework.stereotype.Component;
import ru.vtb.pmts.db.service.impl.TransactionalExternalTask;

import java.util.concurrent.ExecutorService;

@Component
public class TransactionalExternalTaskFactoryImpl implements TransactionalExternalTaskFactory {
    private ExecutorService executorService;

    @Override
    public TransactionalTask createTask(Shard shard) {
        return new TransactionalExternalTask(shard, executorService);
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
