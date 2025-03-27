package com.antalex.db.service.api;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.model.enums.QueryType;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface TransactionalQuery {
    TransactionalQuery bind(Object o, boolean skip);
    TransactionalQuery bind(int index, Object o);
    TransactionalQuery bind(int index, String o, Class<?> clazz);
    TransactionalQuery bindAll(Object... objects);
    TransactionalQuery bindAll(List<String> binds, List<Class<?>> types);
    TransactionalQuery bindShardMap(ShardInstance entity);
    TransactionalQuery addBatch();
    TransactionalQuery fetchLimit(Integer fetchLimit);
    void addBatchOriginal() throws Exception;
    void addRelatedQuery(TransactionalQuery query);
    void addQueryPart(TransactionalQuery query);
    String getQuery();
    ResultQuery getResult();
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
    DataBaseInstance getShard();
    void init();
    int getResultUpdate();
    int[] getResultUpdateBatch();
    long getDuration();
    void setBindIndexes(List<Integer> bindIndexes);

    default TransactionalQuery bind(Object o) {
        return bind(o, false);
    }
}
