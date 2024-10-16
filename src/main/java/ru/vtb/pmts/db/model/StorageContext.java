package ru.vtb.pmts.db.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.vtb.pmts.db.service.impl.transaction.SharedEntityTransaction;
import ru.vtb.pmts.db.utils.ShardUtils;
import ru.vtb.pmts.db.utils.Utils;
import lombok.Builder;

import java.util.Objects;
import java.util.Optional;

@Data
@Builder
@EqualsAndHashCode(exclude = {"transactionalContext"})
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
        if (this.transactionalContext == null || this.transactionalContext.transaction.hasError()) {
            return this.changes;
        } else {
            return this.transactionalContext.changes;
        }
    }

    public Boolean isChanged() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(it -> Objects.nonNull(it.changes))
                .orElse(Objects.nonNull(this.changes));
    }

    public Boolean isChanged(int index) {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(it -> Utils.isChanged(index, it.changes))
                .orElse(Utils.isChanged(index, this.changes));
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
                (ShardUtils.getShardMap(this.cluster.getMainShard().getId()) & this.shardMap) > 0L;
    }

    private static class TransactionalContext {
        private SharedEntityTransaction transaction;
        private Long changes;
        private Long originalShardMap;
        private Boolean stored;
        private Boolean persist;
    }
}
