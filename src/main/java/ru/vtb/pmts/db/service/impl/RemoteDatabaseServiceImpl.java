package ru.vtb.pmts.db.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.dto.QueryDto;
import ru.vtb.pmts.db.model.dto.RemoteButchResultDto;
import ru.vtb.pmts.db.service.ShardDataBaseManager;
import ru.vtb.pmts.db.service.api.RemoteDatabaseService;
import ru.vtb.pmts.db.service.api.TransactionalQuery;
import ru.vtb.pmts.db.service.api.TransactionalTask;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteDatabaseServiceImpl implements RemoteDatabaseService {
    private static final UUID CLIENT_UUID = UUID.randomUUID();
    private final Map<UUID, TransactionalTask> postponedTasks = new HashMap<>();
    private final ShardDataBaseManager dataBaseManager;

    @Override
    public RemoteButchResultDto executeBatch(QueryDto query) {
        TransactionalTask task;
        if (query.postponedCommit() && Objects.nonNull(query.clientUuid())) {
            Assert.isTrue(query.clientUuid().equals(CLIENT_UUID), "Invalid client UUID");
            task = postponedTasks.get(query.taskUuid());
        } else {
            task = dataBaseManager.getTransactionalTask(
                    dataBaseManager.getShard(
                            dataBaseManager.getCluster(query.clusterName()), query.shardId()
                    )
            );
            if (query.postponedCommit()) {
                postponedTasks.put(query.taskUuid(), task);
            }
        }
        List<Class<?>> types =  query.types()
                .stream()
                .map(className -> {
                    if (Objects.isNull(className)) {
                        return null;
                    }
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException err) {
                        throw new ShardDataBaseException(err);
                    }
                })
                .collect(Collectors.toList());

        TransactionalQuery transactionalQuery = task.addQuery(query.query(), query.queryType());
        query.binds().forEach(binds -> transactionalQuery.bindAll(binds, types).addBatch());
        try {
            RemoteButchResultDto result = new RemoteButchResultDto()
                    .clientUuid(CLIENT_UUID)
                    .result(transactionalQuery.executeBatch());
            if (!query.postponedCommit()) {
                task.commit();
            }
            return result;
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        }
    }
}
