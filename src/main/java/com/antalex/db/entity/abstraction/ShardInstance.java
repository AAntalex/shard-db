package com.antalex.db.entity.abstraction;

import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageContext;

import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Map;

public interface ShardInstance {
    Long getId();
    Long getOrderId();
    StorageContext getStorageContext();
    void setId(Long id);
    void setStorageContext(StorageContext storageContext);
    boolean isChanged();
    Boolean isStored();
    boolean hasNewShards();
    boolean isOurShard(Shard shard);
    boolean setTransactionalContext(EntityTransaction transaction);
    List<AttributeStorage> getAttributeStorage();
    void setAttributeStorage(List<AttributeStorage> attributeStorage);
}
