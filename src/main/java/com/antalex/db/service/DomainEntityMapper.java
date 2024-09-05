package com.antalex.db.service;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.AttributeHistoryEntity;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.dto.AttributeHistory;
import com.antalex.db.model.DataStorage;

import java.util.List;
import java.util.Map;

public interface DomainEntityMapper<T extends Domain, M extends ShardInstance> {
    T newDomain(M entity);
    T map(M entity);
    M map(T domain);
    Map<String, DataStorage> getDataStorage();
    void setDomainManager(DomainEntityManager domainManager);
    Map<String, String> getFieldMap();
    List<AttributeHistory> mapAttributeHistory(List<AttributeHistoryEntity> attributeHistoryEntities);
}
