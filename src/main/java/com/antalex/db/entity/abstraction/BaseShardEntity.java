package com.antalex.db.entity.abstraction;

import com.antalex.db.service.impl.SharedEntityTransaction;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageContext;
import com.antalex.db.utils.ShardUtils;

import javax.persistence.EntityTransaction;
import java.util.*;

public abstract class BaseShardEntity implements ShardInstance {
    protected Long id;
    private StorageContext storageContext;
    private List<AttributeStorage> attributeStorage = new ArrayList<>();
    private boolean hasDomain;
    private Cluster cluster;

    public BaseShardEntity () {
        if (this.getClass().isAnnotationPresent(ShardEntity.class)) {
            throw new ShardDataBaseException(
                    String.format(
                            "Запрещено использовать конструктор класса %s напрямую. " +
                                    "Следует использовать ShardEntityManager.newEntity(Class<?>)",
                            this.getClass().getName())
            );
        }
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getOrderId() {
        return Optional.ofNullable(this.id)
                .map(it -> it % (ShardUtils.MAX_SHARDS * ShardUtils.MAX_CLUSTERS))
                .orElse(null);
    }

    @Override
    public StorageContext getStorageContext() {
        return storageContext;
    }

    @Override
    public void setStorageContext(StorageContext storageContext) {
        this.storageContext = storageContext;
    }

    @Override
    public boolean isChanged() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::isChanged)
                .orElse(false);
    }

    @Override
    public Boolean isStored() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::isStored)
                .orElse(false);
    }

    @Override
    public boolean hasNewShards() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::hasNewShards)
                .orElse(false);
    }

    @Override
    public boolean isOurShard(Shard shard) {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::getShard)
                .map(Shard::getId)
                .map(shardId -> shardId.equals(shard.getId()))
                .orElse(false);
    }

    @Override
    public boolean setTransactionalContext(EntityTransaction transaction) {
        return this.storageContext != null &&
                this.storageContext.setTransactionalContext((SharedEntityTransaction) transaction);
    }

    @Override
    public List<AttributeStorage> getAttributeStorage() {
        return attributeStorage;
    }

    @Override
    public void setAttributeStorage(List<AttributeStorage> attributeStorage) {
        this.attributeStorage = attributeStorage;
    }

    @Override
    public boolean hasDomain() {
        return hasDomain;
    }

    @Override
    public void setHasDomain(boolean hasDomain) {
        this.hasDomain = hasDomain;
    }

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public boolean hasMainShard() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::hasMainShard)
                .orElse(false);
    }

    public void setShardMap(Long shardMap) {
        if (this.storageContext != null) {
            this.storageContext.setShardMap(Math.abs(shardMap));
            this.storageContext.setOriginalShardMap(this.storageContext.getShardMap());
        }
    }

    public boolean isChanged(int index) {
        return Optional.ofNullable(this.storageContext)
                .map(it -> it.isChanged(index))
                .orElse(false);
    }

    public Long getChanges() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::getChanges)
                .orElse(null);
    }

    public boolean isLazy() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::isLazy)
                .orElse(false);
    }

    protected void setChanges(int index) {
        if (this.storageContext != null) {
            this.storageContext.setChanges(index);
        }
    }
}
