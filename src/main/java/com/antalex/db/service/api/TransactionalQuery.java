package com.antalex.db.service.api;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Shard;
import com.antalex.db.model.enums.QueryType;

import java.util.concurrent.ExecutorService;

public interface TransactionalQuery {
    TransactionalQuery bind(Object o, boolean skip);
    TransactionalQuery bindAll(Object... objects);
    TransactionalQuery bind(int index, Object o);
    void bindOriginal(int idx, Object o) throws Exception;
    TransactionalQuery bindShardMap(ShardInstance entity);
    TransactionalQuery addBatch();
    TransactionalQuery fetchLimit(Integer fetchLimit);
    void addBatchOriginal() throws Exception;
    void addRelatedQuery(TransactionalQuery query);
    String getQuery();
    ResultQuery getResult();
    ResultQuery getResult(int keyCount);
    QueryType getQueryType();
    int getCount();
    void execute();
    int[] executeBatch() throws Exception;
    int executeUpdate() throws Exception;
    ResultQuery executeQuery() throws Exception;
    void setMainQuery(TransactionalQuery mainQuery);
    TransactionalQuery getMainQuery();
    void setExecutorService(ExecutorService executorService);
    void setParallelRun(Boolean parallelRun);
    void setShard(Shard shard);
    Shard getShard();
    void init();
    int getResultUpdate();
    int[] getResultUpdateBatch();

    default TransactionalQuery bind(Object o) {
        return bind(o, false);
    }
}
