package ru.vtb.pmts.db.service;

import ru.vtb.pmts.db.entity.abstraction.ShardInstance;
import ru.vtb.pmts.db.model.Cluster;
import ru.vtb.pmts.db.model.DataStorage;
import ru.vtb.pmts.db.model.StorageContext;
import ru.vtb.pmts.db.model.enums.ShardType;
import ru.vtb.pmts.db.service.api.ResultQuery;

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
