package ru.vtb.pmts.db.service.impl;

import lombok.extern.slf4j.Slf4j;
import ru.vtb.pmts.db.model.Shard;
import ru.vtb.pmts.db.model.enums.QueryType;
import ru.vtb.pmts.db.model.enums.TaskStatus;
import ru.vtb.pmts.db.service.abstractive.AbstractTransactionalTask;
import ru.vtb.pmts.db.service.api.TransactionalQuery;
import ru.vtb.pmts.db.utils.ShardUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TransactionalExternalTask extends AbstractTransactionalTask {
    private final String url;
    private boolean needCommit;

    public TransactionalExternalTask(
            Shard shard,
            Connection connection,
            ExecutorService executorService,
            boolean parallelCommit) {
        this.connection = connection;
        this.executorService = executorService;
        this.shard = shard;
        this.parallelCommit = parallelCommit;
    }

    @Override
    public void commit() throws SQLException {
        this.connection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        this.connection.rollback();

    }

    @Override
    public Boolean needCommit() {
        return this.needCommit;
    }

    @Override
    public void finish() {
        if (this.status == TaskStatus.COMPLETION) {
            try {
                if (this.parallelCommit) {
                    this.future.get();
                }
            } catch (Exception err) {
                this.errorCompletion = err.getLocalizedMessage();
            }
            this.status = TaskStatus.FINISHED;
        }
    }

    @Override
    public TransactionalQuery createQuery(String query, QueryType queryType) {
        if (Optional.ofNullable(query).map(String::isEmpty).orElse(true)) {
            return null;
        }
        if (
                (
                        queryType == QueryType.DML ||
                                queryType == QueryType.LOCK ||
                                query.toUpperCase().contains("FOR UPDATE")
                )
                        && !this.needCommit)
        {
            this.needCommit = true;
        }
        String sql = ShardUtils.transformSQL(query, shard);
        return new TransactionalExternalQuery(sql, queryType);
    }
}
