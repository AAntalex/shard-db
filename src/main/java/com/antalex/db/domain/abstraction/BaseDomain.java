package com.antalex.db.domain.abstraction;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.utils.Utils;

import java.util.*;

public abstract class BaseDomain implements Domain {
    protected ShardInstance entity;
    protected boolean isLazy;
    private Long changes;
    private final Map<String, Boolean> lazyStore = new HashMap<>();
    private final Map<String, Boolean> changedStore = new HashMap<>();
    private final Map<String, Map<String, ControlledObject>> controlledObjects = new HashMap<>();
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
        if (lazy) {
            changedStore.remove(storageName);
            controlledObjects.remove(storageName);
        }
    }

    @Override
    public void setStorageChanged() {
        storage.keySet().forEach(k -> changedStore.put(k, true));
    }

    public void objectToControl(String storageName, String attribute, Object o, boolean replace) {
        if (o == null) {
            return;
        }
        Map<String, ControlledObject> controlledObjectMap = controlledObjects.get(storageName);
        if (Objects.isNull(controlledObjectMap)) {
            controlledObjectMap = new HashMap<>();
            controlledObjects.put(storageName, controlledObjectMap);
        }
        if (replace || !controlledObjectMap.containsKey(attribute)) {
            controlledObjectMap.put(attribute, new ControlledObject(o.hashCode(), o));
        }
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
        return Optional.ofNullable(changedStore.get(storageName)).orElse(false) ||
                Optional.ofNullable(controlledObjects.get(storageName))
                        .map(Map::values)
                        .map(Collection::stream)
                        .map(it -> it.anyMatch(o -> o.getHashCode() != o.getObject().hashCode()))
                        .orElse(false);
    }

    public Boolean isChanged() {
        return Objects.nonNull(this.changes);
    }

    public void dropChanges() {
        this.changes = null;
    }

    public void dropChanges(String storageName) {
        this.changedStore.put(storageName, false);
        Map<String, ControlledObject> controlledObjectMap = controlledObjects.get(storageName);
        if (Objects.nonNull(controlledObjectMap)) {
            controlledObjectMap
                    .values()
                    .forEach(o -> o.setHashCode(o.getObject().hashCode()));
        }
    }

    @Data
    @AllArgsConstructor
    private static class ControlledObject {
        private int hashCode;
        private Object object;
    }
}
