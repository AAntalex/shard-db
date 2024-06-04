package com.antalex.db.service.impl;

import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.Shard;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.TaskStatus;
import com.antalex.db.service.abstractive.AbstractTransactionalTask;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.utils.ShardUtils;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TransactionalSQLTask extends AbstractTransactionalTask {
    private final Connection connection;

    public TransactionalSQLTask(
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
        try {
            return !this.connection.isClosed() && !this.connection.getAutoCommit();
        } catch (SQLException err) {
            throw new ShardDataBaseException(err);
        }
    }

    @Override
    public void finish() {
        if (this.status == TaskStatus.COMPLETION) {
            try {
                if (this.parallelCommit) {
                    this.future.get();
                }
                if (!this.connection.isClosed()) {
                    this.connection.close();
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
        try {
            if (
                    (
                            queryType == QueryType.DML ||
                                    queryType == QueryType.LOCK ||
                                    query.toUpperCase().contains("FOR UPDATE")
                    )
                            && connection.getAutoCommit())
            {
                connection.setAutoCommit(false);
            }
            String sql = ShardUtils.transformSQL(query, shard);
            return new TransactionalSQLQuery(sql, queryType, connection.prepareStatement(sql));
        } catch (SQLException err) {
            throw new ShardDataBaseException(err);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
