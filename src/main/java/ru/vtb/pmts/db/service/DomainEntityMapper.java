package ru.vtb.pmts.db.service;

import ru.vtb.pmts.db.domain.abstraction.Domain;
import ru.vtb.pmts.db.entity.AttributeHistoryEntity;
import ru.vtb.pmts.db.entity.abstraction.ShardInstance;
import ru.vtb.pmts.db.model.DataStorage;
import ru.vtb.pmts.db.model.dto.AttributeHistory;

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
