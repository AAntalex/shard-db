package ru.vtb.pmts.db.service.impl.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.vtb.pmts.db.model.Shard;
import ru.vtb.pmts.db.service.api.TransactionalRemoteTaskFactory;
import ru.vtb.pmts.db.service.api.TransactionalTask;
import org.springframework.stereotype.Component;
import ru.vtb.pmts.db.service.impl.TransactionalRemoteTask;

import java.util.concurrent.ExecutorService;

@Component
public class TransactionalRemoteTaskFactoryImpl implements TransactionalRemoteTaskFactory {
    private final ObjectMapper objectMapper;
    private ExecutorService executorService;

    TransactionalRemoteTaskFactoryImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TransactionalTask createTask(Shard shard) {
        return new TransactionalRemoteTask(shard, objectMapper, executorService);
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
