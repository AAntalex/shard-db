package com.antalex.db.service.impl;

import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.Shard;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.service.api.TransactionalTask;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityTransaction;
import java.sql.Timestamp;
import java.util.*;

public class SharedEntityTransaction implements EntityTransaction {
    private static final String SAVE_TRANSACTION_QUERY = "INSERT INTO $$$.APP_TRANSACTION " +
            "(UUID,EXECUTE_TIME,FAILED,ERROR,ELAPSED_TIME) VALUES (?,?,?,?,?)";
    private static final String SAVE_DML_QUERY = "INSERT INTO $$$.APP_DML_QUERY " +
            "(TRN_UUID,QUERY_ORDER,SQL_TEXT,ROWS_PROCESSED) VALUES (?,?,?,?)";
    private static final String SQL_ERROR_TEXT = "Ошибки при выполнении запроса: ";
    private static final String SQL_ERROR_COMMIT_TEXT = "Ошибки при подтверждении транзакции: ";
    private static final String SQL_ERROR_ROLLBACK_TEXT = "Ошибки при откате транзакции: ";
    private static final String SQL_ERROR_PREFIX = "\n\t\t";
    private static final String TASK_PREFIX = "\n\t";

    @Setter
    @Getter
    private SharedEntityTransaction parentTransaction;
    private boolean active;
    @Getter
    private boolean completed;
    private boolean hasError;
    private String error;
    private String errorCommit;
    @Getter
    private UUID uuid;
    @Getter
    private final Boolean parallelRun;
    private Long duration;
    @Setter
    private boolean isShort;

    private final List<TransactionalTask> tasks = new ArrayList<>();
    private final Map<Integer, TransactionalTask> currentTasks = new HashMap<>();
    private final Map<Integer, Bucket> buckets = new HashMap<>();

    public SharedEntityTransaction(Boolean parallelRun) {
        this.parallelRun = parallelRun;
    }

    @Override
    public void begin() {
        if (!this.active) {
            this.uuid = UUID.randomUUID();
            this.active = true;
        }
    }

    @Override
    public void rollback() {
        if (this.completed) {
            return;
        }
        this.tasks.forEach(task -> task.completion(true, true));
        this.tasks.forEach(TransactionalTask::finish);
        tasks.clear();
        currentTasks.clear();
        buckets.clear();
        this.completed = true;
    }

    @Override
    public void commit() {
        if (this.completed) {
            return;
        }
        this.duration = System.currentTimeMillis();
        this.tasks.forEach(task -> task.setName("TRN: " + this.uuid + task.getName()));
        this.tasks.forEach(task -> task.run(parallelRun && this.tasks.size() > 1));
        this.tasks.forEach(task -> {
            task.waitTask();
            this.error = processTask(task, task.getError(), this.error, SQL_ERROR_TEXT);
        });
        this.duration = System.currentTimeMillis() - this.duration;
        if (!isShort) {
            prepareSaveTransaction();
        }
        this.tasks.forEach(task -> task.completion(this.hasError, false));
        this.tasks.forEach(TransactionalTask::finish);
        this.tasks.forEach(task ->
                this.errorCommit =
                        processTask(
                                task,
                                task.getErrorCompletion(),
                                this.errorCommit,
                                this.hasError ? SQL_ERROR_ROLLBACK_TEXT : SQL_ERROR_COMMIT_TEXT
                        )
        );
        this.completed = true;
        if (this.hasError) {
            throw new ShardDataBaseException(
                    Optional.ofNullable(this.error)
                            .map(it -> it.concat(StringUtils.LF))
                            .orElse(StringUtils.EMPTY)
                            .concat(
                                    Optional.ofNullable(this.errorCommit)
                                            .orElse(StringUtils.EMPTY)
                            )
            );
        }
    }

    @Override
    public void setRollbackOnly() {

    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    public boolean hasError() {
        return this.hasError;
    }

    public TransactionalTask getCurrentTask(Shard shard, boolean limitParallel) {
        return Optional.ofNullable(currentTasks.get(shard.getHashCode()))
                .orElse(
                        Optional.ofNullable(buckets.get(shard.getHashCode()))
                                .filter(it -> limitParallel)
                                .map(Bucket::getTask)
                                .map(task -> {
                                    currentTasks.put(shard.getHashCode(), task);
                                    return task;
                                })
                                .orElse(null)
                );
    }

    public void addTask(Shard shard, TransactionalTask task) {
        currentTasks.put(shard.getHashCode(), task);
        tasks.add(task);

        Optional.ofNullable(buckets.get(shard.getHashCode()))
                .orElseGet(() -> {
                    Bucket chunk = new Bucket();
                    buckets.put(shard.getHashCode(), chunk);
                    return chunk;
                })
                .addTask(task);

        int chunkSize = buckets.get(shard.getHashCode()).chunkSize();
        task.setName(
                "(shard: " + shard.getName() +
                        (chunkSize > 1 ? ", chunk: " + chunkSize : ")")
        );
    }

    public void addParallel() {
        currentTasks.clear();
    }

    private String processTask(
            TransactionalTask task,
            String errorTask,
            String errorText,
            String errorPrefix)
    {
        if (Objects.nonNull(errorTask)) {
            this.hasError = true;
            return Optional.ofNullable(errorText)
                    .orElse(errorPrefix)
                    .concat(TASK_PREFIX)
                    .concat(task.getName())
                    .concat(":" + SQL_ERROR_PREFIX)
                    .concat(errorTask.replace(StringUtils.LF, SQL_ERROR_PREFIX));
        }
        return errorText;
    }

    private void prepareSaveTransaction() {
        this.buckets.values()
                .stream()
                .map(Bucket::mainTask)
                .filter(Objects::nonNull)
                .filter(it -> !it.getDmlQueries().isEmpty())
                .forEach(task -> {
                    TransactionalQuery saveTransactionQuery = task.createQuery(SAVE_TRANSACTION_QUERY, QueryType.DML)
                            .bind(this.uuid.toString())
                            .bind(new Timestamp(System.currentTimeMillis()))
                            .bind(this.hasError)
                            .bind(this.error != null && this.error.length() > 2000 ? this.error.substring(2000) : this.error)
                            .bind(this.duration);

                    TransactionalQuery saveDMLQuery = task.createQuery(SAVE_DML_QUERY, QueryType.DML);
                    int idx = 0;
                    for (TransactionalQuery query : task.getDmlQueries()) {
                        String sqlText = query.getQuery();
                        saveDMLQuery
                                .bind(this.uuid.toString())
                                .bind(++idx)
                                .bind(sqlText.length() > 2000 ? sqlText.substring(2000) : sqlText)
                                .bind(query.getCount())
                                .addBatch();
                    }
                    if (this.hasError) {
                        task.addStepAfterRollback((Runnable) saveTransactionQuery, SAVE_TRANSACTION_QUERY);
                        task.addStepAfterRollback((Runnable) saveDMLQuery, SAVE_DML_QUERY);
                        task.addStepAfterRollback(() -> {
                            try {
                                task.commit();
                            } catch (Exception err) {
                                throw new ShardDataBaseException(err);
                            }
                        });
                    } else {
                        task.addStepBeforeCommit((Runnable) saveTransactionQuery, SAVE_TRANSACTION_QUERY);
                        task.addStepBeforeCommit((Runnable) saveDMLQuery, SAVE_DML_QUERY);
                    }
                });
    }

    private static class Bucket {
        private final List<TransactionalTask> chunks = new ArrayList<>();
        private int currentIndex;

        int chunkSize() {
            return chunks.size();
        }

        void addTask(TransactionalTask task) {
            if (!chunks.isEmpty()) {
                task.setMainTask(chunks.get(0));
            }
            chunks.add(task);
        }

        TransactionalTask getTask() {
            if (chunks.isEmpty()) {
                return null;
            }
            if (currentIndex >= chunks.size()) {
                currentIndex = 0;
            }
            return chunks.get(currentIndex++);
        }

        TransactionalTask mainTask() {
            if (chunks.isEmpty()) {
                return null;
            }
            return chunks.get(0);
        }
    }
}
