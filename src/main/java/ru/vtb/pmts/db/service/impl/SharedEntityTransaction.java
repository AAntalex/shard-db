package ru.vtb.pmts.db.service.impl;

import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.QueryInfo;
import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.model.TransactionInfo;
import ru.vtb.pmts.db.service.api.TransactionalQuery;
import ru.vtb.pmts.db.service.api.TransactionalTask;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityTransaction;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.IntStream;

public class SharedEntityTransaction implements EntityTransaction {
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

    private final List<TransactionalTask> tasks = new ArrayList<>();
    private final Map<Integer, TransactionalTask> currentTasks = new HashMap<>();
    private final Map<Integer, Bucket> buckets = new HashMap<>();
    private final List<TransactionInfo> transactionInfoList;

    public SharedEntityTransaction(Boolean parallelRun, List<TransactionInfo> transactionInfoList) {
        this.parallelRun = parallelRun;
        this.transactionInfoList = transactionInfoList;
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
        commit(true);
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

    public void commit(boolean enableTransactionStat) {
        if (this.completed) {
            return;
        }
        this.duration = System.currentTimeMillis();
        this.tasks.forEach(task -> task.setName("TRN: " + this.uuid + " " + task.getName()));
        this.tasks.forEach(task -> task.run(parallelRun && this.tasks.size() > 1));
        this.tasks.forEach(task -> {
            task.waitTask();
            this.error = processTask(task, task.getError(), this.error, SQL_ERROR_TEXT);
        });
        this.tasks.forEach(task -> task.completion(this.hasError, false));
        this.tasks.forEach(TransactionalTask::finish);
        this.duration = System.currentTimeMillis() - this.duration;
        if (enableTransactionStat) {
            prepareTransactionInfo();
        }
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

    public boolean hasError() {
        return this.hasError;
    }

    public TransactionalTask getCurrentTask(DataBaseInstance shard, boolean limitParallel) {
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

    public void addTask(DataBaseInstance shard, TransactionalTask task) {
        currentTasks.put(shard.getHashCode(), task);
        tasks.add(task);

        Optional.ofNullable(buckets.get(shard.getHashCode()))
                .orElseGet(() -> {
                    Bucket bucket = new Bucket();
                    buckets.put(shard.getHashCode(), bucket);
                    return bucket;
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

    public void close() {
        this.completed = true;
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

    private void prepareTransactionInfo() {
        List<TransactionInfo> localTransactionInfo = this.buckets.values()
                .stream()
                .filter(it ->
                        !Optional
                                .ofNullable(it.mainTask())
                                .map(TransactionalTask::getDmlQueries)
                                .map(List::isEmpty)
                                .orElse(true)
                )
                .map(bucket ->
                        new TransactionInfo()
                                .uuid(this.uuid)
                                .executeTime(OffsetDateTime.now())
                                .failed(this.hasError)
                                .shard(
                                        Optional
                                                .ofNullable(bucket.mainTask())
                                                .map(TransactionalTask::getShard)
                                                .orElse(null))
                                .error(
                                        this.error != null && this.error.length() > 2000 ?
                                                this.error.substring(2000) :
                                                this.error)
                                .elapsedTime(bucket.getElapsedTime())
                                .chunks(bucket.chunkSize())
                                .allElapsedTime(this.duration)
                                .queries(
                                        Optional
                                                .ofNullable(bucket.mainTask())
                                                .map(TransactionalTask::getDmlQueries)
                                                .map(queries -> IntStream
                                                        .range(0, queries.size())
                                                        .mapToObj(idx -> {
                                                            TransactionalQuery query = queries.get(idx);
                                                            String sqlText = query.getQuery();
                                                            return new QueryInfo()
                                                                    .order(idx+1)
                                                                    .rows(query.getCount())
                                                                    .elapsedTime(query.getDuration())
                                                                    .sql(
                                                                            sqlText.length() > 2000 ?
                                                                                    sqlText.substring(2000) :
                                                                                    sqlText);
                                                        })
                                                        .toList()
                                                )
                                                .orElse(new ArrayList<>())
                                )
                )
                .toList();
        synchronized (this.transactionInfoList) {
            this.transactionInfoList.addAll(localTransactionInfo);
        }
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

        long getElapsedTime() {
            return chunks
                    .stream()
                    .map(TransactionalTask::getDuration)
                    .reduce(0L, Long::max);
        }
    }
}
