package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.api.ResultQuery;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.StorageContext;

import java.util.List;
import java.util.Map;

public interface ShardEntityRepository<T extends ShardInstance> {
    ShardType getShardType(T entity);
    Cluster getCluster(T entity);
    ShardType getShardType();
    Cluster getCluster();
    void generateDependentId(T entity);
    void setDependentStorage(T entity);
    T newEntity();
    T getEntity(Long id, StorageContext storageContext);
    void persist(T entity, boolean delete, boolean onlyChanged);
    void lock(T entity);
    T find(T entity, Map<String, DataStorage> storageMap);
    List<T> findAll(Map<String, DataStorage> storageMap, Integer limit, String condition, Object... binds);
    List<T> findAll(ShardInstance parent, Map<String, DataStorage> storageMap, String condition, Object... binds);
    List<T> skipLocked(Integer limit, String condition, Object... binds);
    T extractValues(T entity, ResultQuery result, int index);
    void setEntityManager(ShardEntityManager entityManager);
}
