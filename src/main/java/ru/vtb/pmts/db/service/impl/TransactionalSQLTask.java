package ru.vtb.pmts.db.service.impl;

import com.zaxxer.hikari.pool.ProxyConnection;
import lombok.Getter;
import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.model.enums.QueryType;
import ru.vtb.pmts.db.model.enums.TaskStatus;
import ru.vtb.pmts.db.service.LockManager;
import ru.vtb.pmts.db.service.abstractive.AbstractTransactionalTask;
import ru.vtb.pmts.db.service.api.TransactionalQuery;
import ru.vtb.pmts.db.utils.ShardUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TransactionalSQLTask extends AbstractTransactionalTask {
    @Getter
    private final Connection connection;
    private final LockManager lockManager;

    public TransactionalSQLTask(
            DataBaseInstance shard,
            Connection connection,
            ExecutorService executorService,
            LockManager lockManager) {
        this.connection = connection;
        this.executorService = executorService;
        this.shard = shard;
        this.lockManager = lockManager;
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
        try {
            if (this.parallelRun) {
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

    @Override
    public void waitTask() {
        if (this.status == TaskStatus.RUNNING) {
            try {
                log.trace("Waiting {}...", this.name);
                long waitTime = System.currentTimeMillis();
                while (true) try {
                    this.future.get(1, TimeUnit.SECONDS);
                    break;
                } catch (TimeoutException ignored) {
                    log.trace("Waiting after {} sec.", (System.currentTimeMillis() - waitTime) / 1000);
                    log.trace("Waiting for {}...", lockManager.getLockInfo(connection, shard));
                }
            } catch (Exception err) {
                throw new ShardDataBaseException(err);
            } finally {
                this.status = TaskStatus.DONE;
            }
        }
    }

    private Object getDelegateConnection(ProxyConnection connection) throws Exception {
        Field field = ProxyConnection.class.getDeclaredField("delegate");
        field.setAccessible(true);
        return field.get(connection);
    }
}
