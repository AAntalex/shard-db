package ru.vtb.pmts.db.service.impl;

import ru.vtb.pmts.db.domain.abstraction.Domain;
import ru.vtb.pmts.db.entity.AttributeHistoryEntity;
import ru.vtb.pmts.db.entity.abstraction.ShardInstance;
import ru.vtb.pmts.db.model.DataStorage;
import ru.vtb.pmts.db.model.dto.AttributeHistory;
import ru.vtb.pmts.db.service.DomainEntityManager;
import ru.vtb.pmts.db.service.DomainEntityMapper;
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