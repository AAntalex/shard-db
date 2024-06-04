package com.antalex.db.domain.abstraction;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.entity.AttributeStorage;

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
}
