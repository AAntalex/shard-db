package ru.vtb.pmts.db.service.api;

import ru.vtb.pmts.db.entity.abstraction.ShardInstance;
import ru.vtb.pmts.db.model.Shard;
import ru.vtb.pmts.db.model.enums.QueryType;

import java.sql.SQLException;
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
