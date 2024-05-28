package ru.vtb.pmts.db.entity.abstraction;

import ru.vtb.pmts.db.entity.AttributeStorage;
import ru.vtb.pmts.db.model.Shard;
import ru.vtb.pmts.db.model.StorageContext;

import javax.persistence.EntityTransaction;
import java.util.List;

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
    boolean hasDomain();
    void setHasDomain(boolean hasDomain);
}
