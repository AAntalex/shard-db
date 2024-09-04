package ru.vtb.pmts.db.domain.abstraction;

import ru.vtb.pmts.db.entity.AttributeStorage;
import ru.vtb.pmts.db.entity.abstraction.ShardInstance;
import ru.vtb.pmts.db.model.dto.AttributeHistory;

import java.util.List;
import java.util.Map;

public interface Domain {
    Long getId();
    <T extends ShardInstance> T getEntity();
    Map<String, AttributeStorage> getStorage();
    boolean isLazy();
    boolean isLazy(String storageName);
    void setLazy(boolean lazy);
    void setLazy(String storageName, boolean lazy);
    void setStorageChanged();
    List<AttributeHistory> getAttributeHistory();
}
