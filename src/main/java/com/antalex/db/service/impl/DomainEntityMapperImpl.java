package com.antalex.db.service.impl;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.DataStorage;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.DomainEntityMapper;
import org.springframework.stereotype.Component;

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
}
