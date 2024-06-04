package com.antalex.db.domain.abstraction;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class BaseDomain implements Domain {
    protected ShardInstance entity;
    protected boolean isLazy;
    private Long changes;
    private final Map<String, Boolean> lazyStore = new HashMap<>();
    private final Map<String, Boolean> changedStore = new HashMap<>();
    private final Map<String, AttributeStorage> storage = new HashMap<>();

    public BaseDomain () {
        if (this.getClass().isAnnotationPresent(DomainEntity.class)) {
            throw new ShardDataBaseException(
                    String.format(
                            "Запрещено использовать конструктор класса %s напрямую. " +
                                    "Следует использовать DomainEntityManager.newDomain(Class<?>)",
                            this.getClass().getName())
            );
        }
    }

    @Override
    public Long getId() {
        return Optional.ofNullable(entity).map(ShardInstance::getId).orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ShardInstance> T getEntity() {
        return (T) entity;
    }

    @Override
    public Map<String, AttributeStorage> getStorage() {
        return storage;
    }

    @Override
    public boolean isLazy() {
        return isLazy;
    }

    @Override
    public boolean isLazy(String storageName) {
        return Optional.ofNullable(lazyStore.get(storageName)).orElse(false);
    }

    @Override
    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    @Override
    public void setLazy(String storageName, boolean lazy) {
        lazyStore.put(storageName, lazy);
    }

    @Override
    public void setStorageChanged() {
        storage.keySet().forEach(k -> changedStore.put(k, true));
    }

    public void setChanges(int index) {
        this.changes = Utils.addChanges(index, this.changes);
    }

    public void setChanges(String storageName) {
        this.changedStore.put(storageName, true);
    }

    public Boolean isChanged(int index) {
        return Utils.isChanged(index, this.changes);
    }

    public Boolean isChanged(String storageName) {
        return Optional.ofNullable(changedStore.get(storageName)).orElse(false);
    }

    public Boolean isChanged() {
        return Objects.nonNull(this.changes);
    }

    public void dropChanges() {
        this.changes = null;
    }

    public void dropChanges(String storageName) {
        this.changedStore.put(storageName, false);
    }
}
