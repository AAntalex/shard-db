package com.antalex.db.service.abstractive;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.Shard;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.api.ResultQuery;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.service.impl.ResultParallelQuery;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

@Slf4j
public abstract class AbstractTransactionalQuery implements TransactionalQuery, Runnable {
    protected QueryType queryType;
    protected String query;
    protected ResultQuery result;
    protected Integer fetchLimit;

    private int resultUpdate;
    private int[] resultUpdateBatch;
    private ExecutorService executorService;
    private Boolean parallelRun;
    private AbstractTransactionalQuery mainQuery;
    private int currentIndex;
    private boolean isButch;
    private int count;
    private Shard shard;
    private final List<TransactionalQuery> relatedQueries = new ArrayList<>();
    private String error;
    private ResultParallelQuery parallelResult;

    @Data
    private static class RunInfo {
        private Future future;
        private String error;
        private String name;
    }

    @Override
    public void addRelatedQuery(TransactionalQuery query) {
        relatedQueries.add(query);
    }

    @Override
    public void execute() {
        if (queryType != QueryType.DML) {
            log.trace("Execute Query '" + this.query + "'");
            if (relatedQueries.isEmpty()) {
                run();
            } else {
                if (Objects.nonNull(executorService) && Optional.ofNullable(parallelRun).orElse(false)) {
                    List<RunInfo> runs = new ArrayList<>();
                    runs.add(runQuery(this));
                    relatedQueries.forEach(relatedQuery ->
                            runs.add(runQuery(relatedQuery))
                    );
                    runs.forEach(this::waitRun);
                    if (Objects.nonNull(this.error)) {
                        throw new ShardDataBaseException(this.error);
                    }
                } else {
                    run();
                    relatedQueries.forEach(TransactionalQuery::execute);
                }
            }
        }
    }

    @Override
    public TransactionalQuery bind(Object o, boolean skip) {
        return skip ? this : bind(this.currentIndex + 1, o);
    }

    @Override
    public TransactionalQuery bindAll(Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            bind(i+1, objects[i]);
        }
        return this;
    }

    @Override
    public TransactionalQuery bind(int index, Object o) {
        try {
            bindOriginal(index, o);
            relatedQueries.forEach(query -> query.bind(index, o));
            this.currentIndex = index;
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        }
        return this;
    }

    @Override
    public TransactionalQuery bindShardMap(ShardInstance entity) {
        return bind(entity.getStorageContext().getShardMap() * (entity.isOurShard(this.shard) ? 1L : -1L));
    }

    @Override
    public TransactionalQuery addBatch() {
        if (queryType != QueryType.DML) {
            throw new ShardDataBaseException("Метод addBatch предназначен только для DML запросов");
        }
        this.increment();
        this.currentIndex = 0;
        this.isButch = true;
        try {
            addBatchOriginal();
            relatedQueries.forEach(TransactionalQuery::addBatch);
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        }
        return this;
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public ResultQuery getResult() {
        return getResult(0);
    }

    @Override
    public ResultQuery getResult(int keyCount) {
        if (Objects.isNull(result)) {
            execute();
        }
        if (!relatedQueries.isEmpty()) {
            if (Objects.isNull(parallelResult)) {
                parallelResult = new ResultParallelQuery(keyCount);
                parallelResult.add(result);
                relatedQueries.forEach(relatedQuery -> parallelResult.add(relatedQuery.getResult()));
            }
            return parallelResult;
        }
        return result;
    }

    @Override
    public QueryType getQueryType() {
        return queryType;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void run() {
        try {
            if (queryType == QueryType.DML) {
                if (this.isButch) {
                    this.resultUpdateBatch = executeBatch();
                } else {
                    this.increment();
                    this.resultUpdate = executeUpdate();
                }
            } else {
                this.increment();
                this.result = executeQuery();
            }
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        }
    }

    @Override
    public void setMainQuery(TransactionalQuery mainQuery) {
        this.mainQuery = (AbstractTransactionalQuery) mainQuery;
    }

    @Override
    public TransactionalQuery getMainQuery() {
        return mainQuery;
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void setParallelRun(Boolean parallelRun) {
        this.parallelRun = parallelRun;
    }

    @Override
    public void setShard(Shard shard) {
        this.shard = shard;
    }

    @Override
    public Shard getShard() {
        return shard;
    }

    @Override
    public void init() {
        this.currentIndex = 0;
        this.result = null;
        this.resultUpdateBatch = null;
        this.resultUpdate = 0;
        this.error = null;
        this.parallelResult = null;
        this.fetchLimit = null;
        this.relatedQueries.clear();
    }

    @Override
    public int getResultUpdate() {
        if (relatedQueries.isEmpty()) {
            return resultUpdate;
        } else {
            return relatedQueries.stream()
                    .map(TransactionalQuery::getResultUpdate)
                    .reduce(resultUpdate, Integer::sum);
        }
    }

    @Override
    public TransactionalQuery fetchLimit(Integer fetchLimit) {
        this.fetchLimit = fetchLimit;
        relatedQueries.forEach(relatedQuery -> relatedQuery.fetchLimit(fetchLimit));
        return this;
    }

    public int[] getResultUpdateBatch() {
        if (relatedQueries.isEmpty()) {
            return resultUpdateBatch;
        } else {
            return IntStream
                    .range(0, resultUpdateBatch.length)
                    .map(idx ->
                            relatedQueries.stream()
                                    .map(TransactionalQuery::getResultUpdateBatch)
                                    .map(arr -> arr[idx])
                                    .reduce(resultUpdateBatch[idx], Integer::sum)
                    ).toArray();
        }
    }

    private RunInfo runQuery(TransactionalQuery transactionalQuery) {
        RunInfo runInfo = new RunInfo();
        runInfo.setName(
                "\"" +
                        transactionalQuery.getQuery() +
                        "\" on shard \"" +
                        transactionalQuery.getShard().getName() +
                        "\""
        );
        runInfo.setFuture(
                this.executorService.submit(() -> {
                    try {
                        log.trace("Running " + runInfo.getName());
                        ((Runnable) transactionalQuery).run();
                    } catch (Exception err) {
                        runInfo.setError("ERROR: " + runInfo.getName() + ":\n" + err.getLocalizedMessage());
                        this.error = runInfo.getError();
                    }
                })
        );
        return runInfo;
    }

    private void waitRun(RunInfo runInfo) {
        try {
            log.trace("Waiting " + runInfo.getName() + "...");
            runInfo.getFuture().get();
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        }
    }

    private void increment() {
        this.count++;
        if (this.mainQuery != null) {
            this.mainQuery.increment();
        }
    }
}
