package com.antalex.db.service.impl;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.AttributeHistoryEntity;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.dto.AttributeHistory;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.DomainEntityMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DomainEntityMapperImpl<T extends Domain, M extends ShardInstance> implements DomainEntityMapper<T, M> {
    @Override
    public T newDomain(M entity) {
        return null;
    }

    @Override
    public M map(T domain) {
        return null;
    }

    @Override
    public Cluster getCluster() {
        return null;
    }

    @Override
    public T map(M entity) {
        return null;
    }

    @Override
    public void setDomainManager(DomainEntityManager domainManager) {

    }

    @Override
    public Map<String, DataStorage> getDataStorage() {
        return null;
    }

    @Override
    public Map<String, String> getFieldMap() {
        return null;
    }

    @Override
    public List<AttributeHistory> mapAttributeHistory(List<AttributeHistoryEntity> attributeHistoryEntities) {
        return List.of();
    }
}