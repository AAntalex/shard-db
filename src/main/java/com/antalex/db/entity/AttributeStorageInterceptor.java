package com.antalex.db.entity;

import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.model.enums.DataFormat;
import lombok.Setter;

@Setter
public class AttributeStorageInterceptor extends AttributeStorage {
    private ShardEntityManager entityManager;

    public void init() {
    }

    @Override
    public Long getEntityId() {
        if (this.isLazy()) {
            entityManager.find(this);
        }
        return super.getEntityId();
    }
    @Override
    public String getStorageName() {
        if (this.isLazy()) {
            entityManager.find(this);
        }
        return super.getStorageName();
    }
    @Override
    public String getData() {
        if (this.isLazy()) {
            entityManager.find(this);
        }
        return super.getData();
    }
    @Override
    public DataFormat getDataFormat() {
        if (this.isLazy()) {
            entityManager.find(this);
        }
        return super.getDataFormat();
    }

    public void setEntityId(Long value, boolean change) {
        if (change) {
            if (this.isLazy()) {
                entityManager.find(this);
            }
            this.setChanges(1);
        }
        super.setEntityId(value);
    }
    @Override
    public void setEntityId(Long value) {
        setEntityId(value, true);
    }
    public void setStorageName(String value, boolean change) {
        if (change) {
            if (this.isLazy()) {
                entityManager.find(this);
            }
            this.setChanges(2);
        }
        super.setStorageName(value);
    }
    @Override
    public void setStorageName(String value) {
        setStorageName(value, true);
    }
    public void setData(String value, boolean change) {
        if (change) {
            if (this.isLazy()) {
                entityManager.find(this);
            }
            this.setChanges(3);
        }
        super.setData(value);
    }
    @Override
    public void setData(String value) {
        setData(value, true);
    }
    public void setDataFormat(DataFormat value, boolean change) {
        if (change) {
            if (this.isLazy()) {
                entityManager.find(this);
            }
            this.setChanges(4);
        }
        super.setDataFormat(value);
    }
    @Override
    public void setDataFormat(DataFormat value) {
        setDataFormat(value, true);
    }
}
