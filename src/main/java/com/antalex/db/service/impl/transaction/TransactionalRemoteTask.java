package com.antalex.db.service.impl.transaction;

import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.model.RemoteTaskContainer;
import com.antalex.db.model.enums.TaskStatus;
import com.antalex.db.service.abstractive.AbstractTransactionalTask;
import com.antalex.db.utils.ShardUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.api.TransactionalQuery;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TransactionalRemoteTask extends AbstractTransactionalTask {
    private final RemoteTaskContainer taskContainer;
    private final ObjectMapper objectMapper;

    public TransactionalRemoteTask(
            DataBaseInstance shard,
            ObjectMapper objectMapper,
            ExecutorService executorService) {
        this.executorService = executorService;
        this.shard = shard;
        this.objectMapper = objectMapper;
        this.taskUuid = UUID.randomUUID();
        this.taskContainer =
                new RemoteTaskContainer()
                        .shard(shard)
                        .taskUuid(this.taskUuid);
    }

    @Override
    public void run(Boolean parallelRun) {
        this.taskContainer.postponedCommit(parallelRun && this.taskContainer.postponedCommit());
        super.run(parallelRun);
    }

    @Override
    public void commit() throws ShardDataBaseException {
        commitOrRollbackRequest(true);
    }

    @Override
    public void rollback() throws ShardDataBaseException {
        commitOrRollbackRequest(false);
    }

    @Override
    public Boolean needCommit() {
        return this.taskContainer.postponedCommit() && this.parallelRun;
    }


    @Override
    public void finish() {
        try {
            if (this.parallelRun) {
                this.future.get();
            }
        } catch (Exception err) {
            this.errorCompletion = err.getLocalizedMessage();
        }
        this.status = TaskStatus.FINISHED;
    }

    @Override
    protected TransactionalQuery createQuery(String query, QueryType queryType) {
        if (
                (
                        queryType == QueryType.DML ||
                                queryType == QueryType.LOCK ||
                                query.toUpperCase().contains("FOR UPDATE")
                )
                        && !taskContainer.postponedCommit())
        {
            taskContainer.postponedCommit(true);
        }
        String sql = ShardUtils.transformSQL(query, shard);
        return new TransactionalRemoteQuery(sql, queryType, taskContainer, objectMapper, shard);
    }

    private void commitOrRollbackRequest(Boolean commit) {
        taskContainer
                .shard()
                .getWebClient()
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/db/request/" + (commit ? "commit" : "rollback"))
                        .queryParam("clientUuid", taskContainer.clientUuid())
                        .queryParam("taskUuid", taskContainer.taskUuid())
                        .queryParam("postponedCommit", taskContainer.postponedCommit())
                        .build()
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class).map(ShardDataBaseException::new))
                .bodyToMono(String.class)
                .block();
    }
}
