package ru.vtb.pmts.db.entity.abstraction;

import ru.vtb.pmts.db.entity.AttributeHistoryEntity;
import ru.vtb.pmts.db.entity.AttributeStorage;
import ru.vtb.pmts.db.model.Cluster;
import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.model.StorageContext;

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
    boolean hasDomain();
    void setHasDomain(boolean hasDomain);
    Cluster getCluster();
    void setCluster(Cluster cluster);
}