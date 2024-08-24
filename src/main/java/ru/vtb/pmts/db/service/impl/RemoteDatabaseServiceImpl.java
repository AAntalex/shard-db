package ru.vtb.pmts.db.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.dto.QueryDto;
import ru.vtb.pmts.db.model.dto.response.ResponseButchDto;
import ru.vtb.pmts.db.model.dto.response.ResponseErrorDto;
import ru.vtb.pmts.db.model.dto.response.ResponseQueryDto;
import ru.vtb.pmts.db.model.dto.response.ResponseUpdateDto;
import ru.vtb.pmts.db.service.ShardDataBaseManager;
import ru.vtb.pmts.db.service.SharedTransactionManager;
import ru.vtb.pmts.db.service.api.RemoteDatabaseService;
import ru.vtb.pmts.db.service.api.ResultQuery;
import ru.vtb.pmts.db.service.api.TransactionalQuery;
import ru.vtb.pmts.db.service.api.TransactionalTask;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteDatabaseServiceImpl implements RemoteDatabaseService {
    private static final UUID CLIENT_UUID = UUID.randomUUID();
    private final Map<UUID, TransactionalTask> postponedTasks = new HashMap<>();
    private final ShardDataBaseManager dataBaseManager;
    private final ObjectMapper objectMapper;
    private final SharedTransactionManager sharedTransactionManager;

    @Override
    public String executeQuery(QueryDto query) {
        TransactionalTask task = getTask(query);
        try {
            ResultQuery result = task
                    .addQuery(query.query(), query.queryType())
                    .bindAll(query.binds(), getTypes(query))
                    .getResult();
            int columnCount = result.getColumnCount();
            List<List<String>> resultValues = new ArrayList<>();
            while (result.next()) {
                resultValues.add(
                        IntStream.rangeClosed(1, columnCount)
                                .mapToObj(idx -> {
                                    try {
                                        return result.getString(idx);
                                    } catch (Exception err) {
                                        throw new ShardDataBaseException(err);
                                    }
                                })
                                .toList()
                );
            }
            ResponseQueryDto responseQueryDto =
                    new ResponseQueryDto()
                            .clientUuid(CLIENT_UUID)
                            .result(resultValues);
            return objectMapper.writeValueAsString(responseQueryDto);
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        } finally {
            finish(query, task);
        }
    }

    @Override
    public String executeUpdate(QueryDto query) {
        TransactionalTask task = getTask(query);
        try {
            TransactionalQuery transactionalQuery =
                    task
                            .addQuery(query.query(), query.queryType())
                            .bindAll(query.binds(), getTypes(query));
            task.run(true);
            task.waitTask();
            if (Objects.nonNull(task.getError())) {
                throw new ShardDataBaseException(task.getError());
            }
            ResponseUpdateDto result = new ResponseUpdateDto()
                    .clientUuid(CLIENT_UUID)
                    .result(transactionalQuery.getResultUpdate());
            return objectMapper.writeValueAsString(result);
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        } finally {
            finish(query, task);
        }
    }

    @Override
    public String executeBatch(QueryDto query) {
        TransactionalTask task = getTask(query);
        try {
            List<Class<?>> types =  getTypes(query);
            TransactionalQuery transactionalQuery = task.addQuery(query.query(), query.queryType());
            query.batchBinds().forEach(binds -> transactionalQuery.bindAll(binds, types).addBatch());
            task.run(true);
            task.waitTask();
            if (Objects.nonNull(task.getError())) {
                throw new ShardDataBaseException(task.getError());
            }
            ResponseButchDto result = new ResponseButchDto()
                    .clientUuid(CLIENT_UUID)
                    .result(transactionalQuery.getResultUpdateBatch());
            return objectMapper.writeValueAsString(result);
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        } finally {
            finish(query, task);
        }
    }

    @Override
    public void commit(UUID clientUuid, UUID taskUuid, Boolean postponedCommit) throws Exception {
        TransactionalTask task = getTask(clientUuid, taskUuid, postponedCommit);
        if (Objects.nonNull(task)) {
            task.commit();
            task.finish();
            ((SharedEntityTransaction) sharedTransactionManager.getTransaction()).close();
            postponedTasks.remove(taskUuid);
        }
    }

    @Override
    public void rollback(UUID clientUuid, UUID taskUuid, Boolean postponedCommit) throws Exception {
        TransactionalTask task = getTask(clientUuid, taskUuid, postponedCommit);
        if (Objects.nonNull(task)) {
            task.rollback();
            task.finish();
            ((SharedEntityTransaction) sharedTransactionManager.getTransaction()).close();
            postponedTasks.remove(taskUuid);
        }
    }

    @Override
    public String getResponseError(String error) {
        try {
            return objectMapper.writeValueAsString(
                    new ResponseErrorDto().clientUuid(CLIENT_UUID).error(error)
            );
        } catch (JsonProcessingException err) {
            throw new ShardDataBaseException(err);
        }
    }

    private void finish(QueryDto query, TransactionalTask task) {
        try {
            if (!query.postponedCommit()) {
                if (task.needCommit()) {
                    if (Objects.nonNull(task.getError())) {
                        task.rollback();
                    } else {
                        task.commit();
                    }
                }
                task.finish();
            }
            ((SharedEntityTransaction) sharedTransactionManager.getTransaction()).close();
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        }
    }

    private TransactionalTask getTask(QueryDto query) {
        TransactionalTask task = getTask(query.clientUuid(), query.taskUuid(), query.postponedCommit());
        if (Objects.isNull(task)) {
            task = dataBaseManager.getTransactionalTask(
                    dataBaseManager.getShard(
                            dataBaseManager.getCluster(query.clusterName()), query.shardId()
                    )
            );
            if (query.postponedCommit()) {
                postponedTasks.put(query.taskUuid(), task);
            }
        }
        return task;
    }

    private List<Class<?>> getTypes(QueryDto query) {
        return query.types()
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
    }

    private TransactionalTask getTask(UUID clientUuid, UUID taskUuid, Boolean postponedCommit) {
        if (postponedCommit && Objects.nonNull(clientUuid)) {
            Assert.isTrue(clientUuid.equals(CLIENT_UUID), "Invalid client UUID");
            return postponedTasks.get(taskUuid);
        }
        return null;
    }
}
