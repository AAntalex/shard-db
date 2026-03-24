package com.antalex.db.model;

import lombok.Data;
import com.antalex.db.service.impl.transaction.SharedEntityTransaction;
import com.antalex.db.service.impl.transaction.TransactionState;
import com.antalex.db.utils.ShardUtils;
import com.antalex.db.utils.Utils;
import lombok.Builder;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
public class StorageContext {
    private Cluster cluster;
    private DataBaseInstance shard;
    private Long shardMap;
    private Long originalShardMap;
    private Boolean stored;
    private Long changes;
    private boolean lazy;
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
            this.transactionalContext.transactionUuid = transaction.getUuid();
            this.transactionalContext.transactionState = transaction.getState();
            this.transactionalContext.persist = false;
            return true;
        }
        if (!this.transactionalContext.persist &&
                this.transactionalContext.transactionUuid.equals(transaction.getUuid())) {
            return false;
        }
        Optional.ofNullable(this.transactionalContext.transactionState)
                .filter(TransactionState::isCompleted)
                .ifPresent(it -> {
                    if (it.isHasError()) {
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
        this.transactionalContext.transactionUuid = transaction.getUuid();
        this.transactionalContext.transactionState = transaction.getState();
        return true;
    }

    public void persist(boolean delete) {
        if (this.transactionalContext != null) {
            this.transactionalContext.changes = null;
            this.transactionalContext.stored = !delete;
            this.transactionalContext.originalShardMap = this.shardMap;
            this.transactionalContext.persist = true;
        }
    }

    public void setChanges(int index) {
        this.changes = Utils.addChanges(index, this.changes);
        if (this.transactionalContext != null) {
            this.transactionalContext.changes = Utils.addChanges(index, this.transactionalContext.changes);
        }
    }

    public Long getChanges() {
        if (this.transactionalContext == null || this.transactionalContext.transactionState.isHasError()) {
            return this.changes;
        } else {
            return this.transactionalContext.changes;
        }
    }

    public Boolean isChanged() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transactionState.isHasError())
                .map(it -> Objects.nonNull(it.changes))
                .orElse(Objects.nonNull(this.changes));
    }

    public Boolean isChanged(int index) {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transactionState.isHasError())
                .map(it -> Utils.isChanged(index, it.changes))
                .orElse(Utils.isChanged(index, this.changes));
    }

    public Boolean isStored() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transactionState.isHasError())
                .map(it -> it.stored)
                .orElse(this.stored);
    }

    public Long getOriginalShardMap() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transactionState.isHasError())
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
                (ShardUtils.getShardMap(this.cluster.getMainShard().getId()) & this.shardMap) > 0L;
    }

    private static class TransactionalContext {
        private TransactionState transactionState;
        private UUID transactionUuid;
        private Long changes;
        private Long originalShardMap;
        private Boolean stored;
        private Boolean persist;
    }
}
