package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;

public interface ShardEntityManager {
    <T extends ShardInstance> ShardType getShardType(T entity);
    <T extends ShardInstance> Cluster getCluster(T entity);
    <T extends ShardInstance> T save(T entity);
    <T extends ShardInstance> Iterable save(Iterable<T> entities);
    <T extends ShardInstance> void setDependentStorage(T entity);
    <T extends ShardInstance> void generateId(T entity);
    <T extends ShardInstance> void generateId(Iterable<T> entities);
    <T extends ShardInstance> void generateDependentId(T entity);
    <T extends ShardInstance> void setStorage(T entity, StorageAttributes storage);
    <T extends ShardInstance> void setStorage(Iterable<T> entities, StorageAttributes storage);
}
