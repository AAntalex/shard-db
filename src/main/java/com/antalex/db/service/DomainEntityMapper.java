package com.antalex.db.service;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.DataStorage;

import java.util.Map;

public interface DomainEntityMapper<T extends Domain, M extends ShardInstance> {
    T newDomain(M entity);
    T map(M entity);
    M map(T domain);
    Map<String, DataStorage> getDataStorage();
    void setDomainManager(DomainEntityManager domainManager);
}
