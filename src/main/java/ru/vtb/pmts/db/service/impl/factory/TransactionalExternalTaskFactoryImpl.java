package ru.vtb.pmts.db.service.impl.factory;

import ru.vtb.pmts.db.model.Shard;
import ru.vtb.pmts.db.service.api.TransactionalExternalTaskFactory;
import ru.vtb.pmts.db.service.api.TransactionalTask;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class TransactionalExternalTaskFactoryImpl implements TransactionalExternalTaskFactory {
    private ExecutorService executorService;

    @Override
    public TransactionalTask createTask(Shard shard) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        System.out.println("AAA setExecutorService");
        this.executorService = executorService;
    }
}
