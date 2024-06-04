package com.antalex.db.service;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.model.DataStorage;

import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Map;

public interface DomainEntityManager {
    <T extends Domain> T newDomain(Class<T> clazz);
    <T extends Domain, M extends ShardInstance> T map(final Class<T> clazz, M entity);
    <T extends Domain, M extends ShardInstance> M map(final Class<T> clazz, T domain);
    <T extends Domain, M extends ShardInstance> List<T> mapAllToDomains(final Class<T> clazz, List<M> entities);
    <T extends Domain, M extends ShardInstance> List<M> mapAllToEntities(final Class<T> clazz, List<T> domains);
    <T extends Domain> T find(Class<T> clazz, Long id);
    <T extends Domain> List<T> findAll(Class<T> clazz, Integer limit, String condition, Object... binds);
    <T extends Domain> List<T> skipLocked(Class<T> clazz, Integer limit, String condition, Object... binds);
    <T extends Domain> T save(T domain);
    <T extends Domain> List<T> saveAll(List<T> domains);
    <T extends Domain> T update(T domain);
    <T extends Domain> void delete(T domain);
    <T extends Domain> void deleteAll(List<T> domains);
    <T extends Domain> List<T> updateAll(List<T> domains);
    <T extends Domain> boolean lock(T domain);
    <T extends Domain> Map<String, DataStorage> getDataStorage(Class<T> clazz);
    AttributeStorage getAttributeStorage(Domain domain, DataStorage dataStorage);
    EntityTransaction getTransaction();
    String getTransactionUUID();
    void setAutonomousTransaction();
    void addParallel();

    default  <T extends Domain> List<T> findAll(Class<T> clazz, String condition, Object... binds) {
        return findAll(clazz, null, condition, binds);
    }

    default  <T extends Domain> List<T> findAll(Class<T> clazz, Integer limit) {
        return findAll(clazz, limit, null);
    }

    default  <T extends Domain> List<T> findAll(Class<T> clazz) {
        return findAll(clazz, null);
    }

    default  <T extends Domain> List<T> skipLocked(Class<T> clazz, String condition, Object... binds) {
        return skipLocked(clazz, 1, condition, binds);
    }
}
