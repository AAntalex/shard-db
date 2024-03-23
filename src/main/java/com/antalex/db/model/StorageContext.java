package com.antalex.db.model;

import com.antalex.db.service.impl.SharedEntityTransaction;
import com.antalex.db.utils.ShardUtils;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Builder
public class StorageContext {
    private Cluster cluster;
    private Shard shard;
    private Long shardMap;
    private Long originalShardMap;
    private Boolean stored;
    private Boolean changed;
    private boolean isLazy;
    private boolean temporary;
    private TransactionalContext transactionalContext;

    public boolean setTransactionalContext(SharedEntityTransaction transaction) {
        if (transaction == null) {
            return false;
        }
        if (this.transactionalContext == null) {
            this.transactionalContext = new TransactionalContext();
            this.transactionalContext.setStored(Optional.ofNullable(this.stored).orElse(false));
            this.transactionalContext.setChanged(Optional.ofNullable(this.changed).orElse(false));
            this.transactionalContext.setOriginalShardMap(this.originalShardMap);
            this.transactionalContext.setTransaction(transaction);
            this.transactionalContext.setPersist(false);
            return true;
        }
        if (!this.transactionalContext.getPersist() && this.transactionalContext.getTransaction() == transaction) {
            return false;
        }
        Optional.ofNullable(this.transactionalContext.getTransaction())
                .filter(SharedEntityTransaction::isCompleted)
                .ifPresent(it -> {
                    if (it.hasError()) {
                        this.transactionalContext.setChanged(Optional.ofNullable(this.changed).orElse(false));
                        this.transactionalContext.setStored(Optional.ofNullable(this.stored).orElse(false));
                        this.transactionalContext.setOriginalShardMap(this.originalShardMap);
                    } else {
                        this.changed = this.transactionalContext.getChanged();
                        this.stored = this.transactionalContext.getStored();
                        this.originalShardMap = this.transactionalContext.getOriginalShardMap();
                    }
                });
        this.transactionalContext.setPersist(false);
        this.transactionalContext.setTransaction(transaction);
        return true;
    }

    public void persist() {
        if (this.transactionalContext != null) {
            this.transactionalContext.setChanged(false);
            this.transactionalContext.setStored(true);
            this.transactionalContext.setOriginalShardMap(this.shardMap);
            this.transactionalContext.setPersist(true);
        }
    }

    public void setChanged() {
        this.changed = true;
        if (this.transactionalContext != null) {
            this.transactionalContext.setChanged(true);
        }
    }

    public Boolean isChanged() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(TransactionalContext::getChanged)
                .orElse(this.changed);
    }

    public Boolean isStored() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(TransactionalContext::getStored)
                .orElse(this.stored);
    }

    public Long getOriginalShardMap() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(TransactionalContext::getOriginalShardMap)
                .orElse(this.originalShardMap);
    }

    public boolean hasNewShards() {
        return Optional.ofNullable(getOriginalShardMap())
                .map(it -> !it.equals(this.shardMap))
                .orElse(false);
    }

    public boolean hasMainShard() {
        return this.shardMap.equals(0L) ||
                Long.compare(ShardUtils.getShardMap(this.cluster.getMainShard().getId()) & this.shardMap, 0L) > 0;
    }

    public Shard getShard() {
        return shard;
    }

    public void setShard(Shard shard) {
        this.shard = shard;
    }

    public Long getShardMap() {
        return shardMap;
    }

    public void setShardMap(Long shardMap) {
        this.shardMap = shardMap;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    public boolean isLazy() {
        return isLazy;
    }

    @Data
    private class TransactionalContext {
        private SharedEntityTransaction transaction;
        private Boolean changed;
        private Long originalShardMap;
        private Boolean stored;
        private Boolean persist;
    }
}