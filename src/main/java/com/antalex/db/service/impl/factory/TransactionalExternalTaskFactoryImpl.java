package com.antalex.db.service.impl.factory;

import com.antalex.db.model.Shard;
import com.antalex.db.service.api.TransactionalExternalTaskFactory;
import com.antalex.db.service.api.TransactionalTask;
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
