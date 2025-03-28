package com.antalex.db.service.impl.transaction;

import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.model.enums.TaskStatus;
import com.antalex.db.service.LockManager;
import com.antalex.db.service.abstractive.AbstractTransactionalTask;
import com.antalex.db.utils.ShardUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.api.TransactionalQuery;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
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
        this.taskUuid = UUID.randomUUID();
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
            throw new ShardDataBaseException(err, this.shard);
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
    protected TransactionalQuery createQuery(String query, QueryType queryType) {
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
            return new TransactionalSQLQuery(sql, queryType, connection.prepareStatement(sql), shard);
        } catch (SQLException err) {
            throw new ShardDataBaseException(err, this.shard);
        }
    }

    @Override
    public void waitTask() {
        if (this.status == TaskStatus.RUNNING) {
            try {
                log.trace("Waiting {}...", this.name);
                long waitTime = System.currentTimeMillis();
                long lockTime = waitTime;
                String lockInfo = StringUtils.EMPTY;
                while (true) try {
                    this.future.get(lockManager.getDelay(), TimeUnit.SECONDS);
                    break;
                } catch (TimeoutException ignored) {
                    String currentLockInfo = lockManager.getLockInfo(connection, shard);
                    log.trace(
                            "Waiting after {} sec. LockInfo: {}, LockTime = {}",
                            (System.currentTimeMillis() - waitTime) / 1000,
                            currentLockInfo,
                            (System.currentTimeMillis() - lockTime) / 1000
                    );
                    if (currentLockInfo == null || !currentLockInfo.equals(lockInfo)) {
                        lockInfo = currentLockInfo == null ? StringUtils.EMPTY : currentLockInfo;
                        lockTime = System.currentTimeMillis();
                    }
                    if (currentLockInfo != null
                            && (System.currentTimeMillis() - lockTime) / 1000 >= lockManager.getTimeOut()
                    ) {
                        for (TransactionalQuery query : this.queries.values()) {
                            ((TransactionalSQLQuery) query).cancel();
                        }
                        this.future.get();
                        this.error = "Истекло время ожидания блокирующей пассивной сессии: " + currentLockInfo;
                    }
                }
            } catch (Exception err) {
                throw new ShardDataBaseException(err, this.shard);
            } finally {
                this.status = TaskStatus.DONE;
            }
        }
    }
}
