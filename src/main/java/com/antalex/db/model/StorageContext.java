package com.antalex.db.model;

import com.antalex.db.service.impl.SharedEntityTransaction;
import com.antalex.db.utils.ShardUtils;
import lombok.Builder;

import java.util.Objects;
import java.util.Optional;

@Builder
public class StorageContext {
    private Cluster cluster;
    private Shard shard;
    private Long shardMap;
    private Long originalShardMap;
    private Boolean stored;
    private Long changes;
    private boolean isLazy;
    private boolean temporary;
    private TransactionalContext transactionalContext;

    public boolean setTransactionalContext(SharedEntityTransaction transaction) {
        if (transaction == null) {
            return false;
        }
        if (this.transactionalContext == null) {
            this.transactionalContext = new TransactionalContext();
            this.transactionalContext.stored = Optional.ofNullable(this.stored).orElse(false);
            this.transactionalContext.changes = this.changes;
            this.transactionalContext.originalShardMap = this.originalShardMap;
            this.transactionalContext.transaction = transaction;
            this.transactionalContext.persist = false;
            return true;
        }
        if (!this.transactionalContext.persist && this.transactionalContext.transaction == transaction) {
            return false;
        }
        Optional.ofNullable(this.transactionalContext.transaction)
                .filter(SharedEntityTransaction::isCompleted)
                .ifPresent(it -> {
                    if (it.hasError()) {
                        this.transactionalContext.changes = this.changes;
                        this.transactionalContext.stored = Optional.ofNullable(this.stored).orElse(false);
                        this.transactionalContext.originalShardMap = this.originalShardMap;
                    } else {
                        this.changes = this.transactionalContext.changes;
                        this.stored = this.transactionalContext.stored;
                        this.originalShardMap = this.transactionalContext.originalShardMap;
                    }
                });
        this.transactionalContext.persist = false;
        this.transactionalContext.transaction = transaction;
        return true;
    }

    public void persist() {
        if (this.transactionalContext != null) {
            this.transactionalContext.changes = null;
            this.transactionalContext.stored = true;
            this.transactionalContext.originalShardMap = this.shardMap;
            this.transactionalContext.persist = true;
        }
    }

    private Long addChanges(int index, Long changes) {
        return Optional.ofNullable(changes).orElse(0L) |
                (index > Long.SIZE ? 0L : (1L << (index - 1)));
    }

    public void setChanges(int index) {
        this.changes = addChanges(index, this.changes);
        if (this.transactionalContext != null) {
            this.transactionalContext.changes = addChanges(index, this.transactionalContext.changes);
        }
    }

    public Boolean isChanged() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(it -> Objects.nonNull(it.changes))
                .orElse(Objects.nonNull(this.changes));
    }

    private Boolean isChanged(int index, Long changes) {
        return Optional.ofNullable(changes)
                .map(it -> index > Long.SIZE || (it & (1L << (index - 1))) > 0L)
                .orElse(false);
    }

    public Boolean isChanged(int index) {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(it -> isChanged(index, it.changes))
                .orElse(isChanged(index, this.changes));
    }

    public Boolean isStored() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(it -> it.stored)
                .orElse(this.stored);
    }

    public Long getOriginalShardMap() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(it -> it.originalShardMap)
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

    private class TransactionalContext {
        private SharedEntityTransaction transaction;
        private Long changes;
        private Long originalShardMap;
        private Boolean stored;
        private Boolean persist;
    }
}
