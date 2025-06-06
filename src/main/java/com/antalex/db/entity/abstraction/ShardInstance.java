package com.antalex.db.entity.abstraction;

import com.antalex.db.model.StorageContext;
import com.antalex.db.entity.AttributeHistoryEntity;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataBaseInstance;

import javax.persistence.EntityTransaction;
import java.util.List;

public interface ShardInstance {
    Long getId();
    Long getOrderedId();
    StorageContext getStorageContext();
    void setId(Long id);
    void setStorageContext(StorageContext storageContext);
    boolean isChanged();
    boolean isLazy();
    Boolean isStored();
    boolean hasNewShards();
    boolean isOurShard(DataBaseInstance shard);
    boolean setTransactionalContext(EntityTransaction transaction);
    List<AttributeStorage> getAttributeStorage();
    void setAttributeStorage(List<AttributeStorage> attributeStorage);
    List<AttributeHistoryEntity> getAttributeHistory();
    void setAttributeHistory(List<AttributeHistoryEntity> attributeHistory);
    Cluster getCluster();
    void setCluster(Cluster cluster);
}