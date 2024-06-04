package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.api.ResultQuery;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;

import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Map;

public interface ShardEntityManager {
    <T extends ShardInstance> ShardType getShardType(T entity);
    <T extends ShardInstance> Cluster getCluster(T entity);
    <T extends ShardInstance> ShardType getShardType(Class<T> clazz);
    <T extends ShardInstance> Cluster getCluster(Class<T> clazz);
    <T extends ShardInstance> T save(T entity);
    <T extends ShardInstance> Iterable<T> saveAll(Iterable<T> entities);
    <T extends ShardInstance> T update(T entity);
    <T extends ShardInstance> Iterable<T> updateAll(Iterable<T> entities);
    <T extends ShardInstance> void delete(T entity);
    <T extends ShardInstance> void deleteAll(Iterable<T> entities);
    <T extends ShardInstance> void setDependentStorage(T entity);
    <T extends ShardInstance> void generateId(T entity, boolean force);
    <T extends ShardInstance> void generateId(T entity);
    <T extends ShardInstance> void persist(T entity, boolean onlyChanged);
    <T extends ShardInstance> void persistAll(Iterable<T> entities, boolean delete, boolean onlyChanged);
    <T extends ShardInstance> void generateAllId(Iterable<T> entities);
    <T extends ShardInstance> void generateDependentId(T entity);
    <T extends ShardInstance> void setStorage(T entity, ShardInstance parent, boolean force);
    <T extends ShardInstance> void setStorage(T entity, ShardInstance parent);
    <T extends ShardInstance> void setAllStorage(Iterable<T> entities, ShardInstance parent);
    <T extends ShardInstance> T newEntity(Class<T> clazz);
    <T extends ShardInstance> T getEntity(Class<T> clazz, Long id);
    <T extends ShardInstance> TransactionalQuery createQuery(
            T entity,
            String query,
            QueryType queryType,
            QueryStrategy queryStrategy);
    <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(
            T entity,
            String query,
            QueryType queryType,
            QueryStrategy queryStrategy
    );
    Iterable<TransactionalQuery> createQueries(
            Cluster cluster,
            String query,
            QueryType queryType
    );
    <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(
            Class<T> clazz,
            String query,
            QueryType queryType
    );
    <T extends ShardInstance> TransactionalQuery createQuery(Class<T> clazz, String query, QueryType queryType);
    TransactionalQuery createQuery(Cluster cluster, String query, QueryType queryType);
    <T extends ShardInstance> boolean lock(T entity);
    <T extends ShardInstance> T find(Class<T> clazz, Long id, Map<String, DataStorage> storageMap);
    <T extends ShardInstance> T find(T entity, Map<String, DataStorage> storageMap);
    AttributeStorage findAttributeStorage(ShardInstance parent, DataStorage storage);
    <T extends ShardInstance> List<T> findAll(
            Class<T> clazz,
            Map<String, DataStorage> storageMap,
            Integer limit,
            String condition,
            Object... binds
    );
    <T extends ShardInstance> List<T> findAll(
            Class<T> clazz,
            ShardInstance parent,
            Map<String, DataStorage> storageMap,
            String condition,
            Object... binds
    );
    <T extends ShardInstance> List<T> skipLocked(
            Class<T> clazz,
            Integer limit,
            String condition,
            Object... binds);
    <T extends ShardInstance> T extractValues(T entity, ResultQuery result, int index);
    <T extends ShardInstance> T extractValues(Class<T> clazz, ResultQuery result, int index);
    List<AttributeStorage> extractAttributeStorage(
            Map<String, DataStorage> storageMap,
            ResultQuery result,
            Cluster cluster,
            int index
    );
    EntityTransaction getTransaction();
    String getTransactionUUID();
    void setAutonomousTransaction();
    void addParallel();
    void flush();

    default <T extends ShardInstance> TransactionalQuery createQuery(T entity, String query, QueryType queryType) {
        return createQuery(entity, query, queryType, QueryStrategy.ALL_SHARDS);
    }

    default <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(
            T entity,
            String query,
            QueryType queryType)
    {
        return createQueries(entity, query, queryType, QueryStrategy.ALL_SHARDS);
    }

    default <T extends ShardInstance> T find(Class<T> clazz, Long id) {
        return find(clazz, id, null);
    }

    default <T extends ShardInstance> T find(T entity) {
        return find(entity, null);
    }

    default <T extends ShardInstance> List<T> findAll(Class<T> clazz, Integer limit, String condition, Object... binds) {
        return findAll(clazz, null, limit, condition, binds);
    }

    default <T extends ShardInstance> List<T> findAll(Class<T> clazz, String condition, Object... binds) {
        return findAll(clazz, (Map<String, DataStorage>) null, null, condition, binds);
    }

    default  <T extends ShardInstance> List<T> findAll(Class<T> clazz, Integer limit) {
        return findAll(clazz, (Map<String, DataStorage>) null, limit, null);
    }

    default  <T extends ShardInstance> List<T> findAll(Class<T> clazz) {
        return findAll(clazz, (Map<String, DataStorage>) null, null, null);
    }

    default <T extends ShardInstance> List<T> skipLocked(
            Class<T> clazz,
            String condition,
            Object... binds)
    {
        return skipLocked(clazz, 1, condition, binds);
    }

    default <T extends ShardInstance> List<T> findAll(
            Class<T> clazz,
            ShardInstance parent,
            String condition,
            Object... binds)
    {
        return findAll(clazz, parent, null, condition, binds);
    }
}
