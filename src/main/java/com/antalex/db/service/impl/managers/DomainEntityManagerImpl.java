package com.antalex.db.service.impl.managers;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.AttributeHistoryEntity;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.dto.AttributeHistory;
import com.antalex.db.service.SharedTransactionManager;
import com.antalex.db.service.api.DataWrapper;
import com.antalex.db.service.api.DataWrapperFactory;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.DomainEntityMapper;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.utils.Utils;

import javax.persistence.EntityTransaction;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Primary
@SuppressWarnings("unchecked")
public class DomainEntityManagerImpl implements DomainEntityManager {
    private static final Map<Class<?>, Mapper> MAPPERS = new HashMap<>();

    private final ThreadLocal<Mapper> currentMapper = new ThreadLocal<>();
    private final ThreadLocal<Class<?>> currentSourceClass = new ThreadLocal<>();

    private final ShardEntityManager entityManager;
    private final DataWrapperFactory dataWrapperFactory;
    private final SharedTransactionManager sharedTransactionManager;

    DomainEntityManagerImpl(
            ShardEntityManager entityManager,
            DataWrapperFactory dataWrapperFactory,
            SharedTransactionManager sharedTransactionManager)
    {
        this.entityManager = entityManager;
        this.dataWrapperFactory = dataWrapperFactory;
        this.sharedTransactionManager = sharedTransactionManager;
    }

    @Autowired
    public void setDomainMappers(
            List<DomainEntityMapper<? extends Domain, ? extends ShardInstance>> domainEntityMappers)
    {
        for (DomainEntityMapper<?, ?> domainEntityMapper : domainEntityMappers) {
            Class<?>[] classes = GenericTypeResolver
                    .resolveTypeArguments(domainEntityMapper.getClass(), DomainEntityMapper.class);
            if (Objects.nonNull(classes) && classes.length > 0 && !MAPPERS.containsKey(classes[0])) {
                domainEntityMapper.setDomainManager(this);
                MAPPERS.put(classes[0], new Mapper(domainEntityMapper, classes[1]));
            }
        }
    }

    @Override
    public <T extends Domain> T find(Class<T> clazz, Long id) {
        Mapper mapper = getMapper(clazz);
        return map(
                clazz,
                entityManager.find(
                        mapper.entityClass,
                        id,
                        mapper.domainEntityMapper.getDataStorage()
                )
        );
    }

    @Override
    public <T extends Domain> T find(Class<T> clazz, String condition, Object... binds) {
        Mapper mapper = getMapper(clazz);
        return sharedTransactionManager.runInTransaction(() -> map(
                clazz,
                entityManager.find(
                        mapper.entityClass,
                        mapper.domainEntityMapper.getDataStorage(),
                        Utils.transformCondition(condition, mapper.domainEntityMapper.getFieldMap()),
                        binds
                )
        ));
    }

    @Override
    public <T extends Domain> List<T> findAllLimit(Class<T> clazz, Integer limit, String condition, Object... binds) {
        Mapper mapper = getMapper(clazz);
        return sharedTransactionManager.runInTransaction(() -> mapAllToDomains(
                clazz,
                entityManager.findAllLimit(
                        mapper.entityClass,
                        mapper.domainEntityMapper.getDataStorage(),
                        limit,
                        Utils.transformCondition(condition, mapper.domainEntityMapper.getFieldMap()),
                        binds
                )
        ));
    }

    @Override
    public <T extends Domain> List<T> skipLocked(Class<T> clazz, Integer limit, String condition, Object... binds) {
        Mapper mapper = getMapper(clazz);
        return mapAllToDomains(
                clazz,
                entityManager.skipLocked(
                        mapper.entityClass,
                        limit,
                        Utils.transformCondition(condition, mapper.domainEntityMapper.getFieldMap()),
                        binds
                )
        );
    }

    @Override
    public <T extends Domain> Map<String, String> getFieldMap(Class<T> clazz) {
        return getMapper(clazz).domainEntityMapper.getFieldMap();
    }

    @Override
    public <T extends Domain> T newDomain(Class<T> clazz) {
        Mapper mapper = getMapper(clazz);
        return (T) mapper.domainEntityMapper.newDomain(entityManager.newEntity(mapper.entityClass));
    }

    @Override
    public <T extends Domain, M extends ShardInstance> T map(final Class<T> clazz, M entity) {
        return (T) getMapper(clazz).domainEntityMapper.map(entity);
    }

    @Override
    public <T extends Domain, M extends ShardInstance> M map(final Class<T> clazz, T domain) {
        return (M) getMapper(clazz).domainEntityMapper.map(domain);
    }

    @Override
    public <T extends Domain, M extends ShardInstance> List<M> mapAllToEntities(
            Class<T> clazz,
            List<T> domains)
    {
        Mapper mapper = getMapper(clazz);
        return (List) domains.stream()
                .map(mapper.domainEntityMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public <T extends Domain, M extends ShardInstance> List<T> mapAllToDomains(Class<T> clazz, List<M> entities) {
        Mapper mapper = getMapper(clazz);
        return (List) entities.stream()
                .map(mapper.domainEntityMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public <T extends Domain> T save(T domain) {
        if (domain == null) {
            return null;
        }
        sharedTransactionManager.runInTransaction(() ->
                entityManager.save(getMapper(domain.getClass()).domainEntityMapper.map(domain)));
        return domain;
    }

    @Override
    public <T extends Domain> T update(T domain) {
        if (domain == null) {
            return null;
        }
        sharedTransactionManager.runInTransaction(() ->
                entityManager.update(getMapper(domain.getClass()).domainEntityMapper.map(domain)));
        return domain;
    }

    @Override
    public <T extends Domain> List<T> updateAll(List<T> domains) {
        return saveAll(domains, true);
    }

    @Override
    public <T extends Domain> void delete(T domain) {
        if (domain == null) {
            return;
        }
        ShardInstance entity = getMapper(domain.getClass()).domainEntityMapper.map(domain);
        sharedTransactionManager.runInTransaction(() -> {
            entity.setAttributeStorage(
                    entityManager.findAll(AttributeStorage.class, "C_ENTITY_ID=?", domain.getId())
            );
            entity.setAttributeHistory(
                    entityManager.findAll(AttributeHistoryEntity.class, "C_ENTITY_ID=?", domain.getId())
            );
            domain.getStorage().putAll(
                    entity.getAttributeStorage()
                            .stream()
                            .collect(Collectors.toMap(AttributeStorage::getStorageName, it -> it))
            );
            entityManager.delete(entity);
        });
        domain.setStorageChanged();
    }

    @Override
    public <T extends Domain> void deleteAll(List<T> domains) {
        if (domains == null) {
            return;
        }
        sharedTransactionManager.runInTransaction(() -> domains.forEach(this::delete));
        domains.forEach(this::delete);
    }

    @Override
    public <T extends Domain> List<T> saveAll(List<T> domains) {
        return saveAll(domains, false);
    }

    @Override
    public AttributeStorage getAttributeStorage(Domain domain, DataStorage dataStorage) {
        AttributeStorage attributeStorage = domain.getStorage().get(dataStorage.getName());
        if (Objects.isNull(attributeStorage)) {
            if (domain.isLazy(dataStorage.getName())) {
                attributeStorage = entityManager.findAttributeStorage(domain.getEntity(), dataStorage);
            }
            if (Objects.isNull(attributeStorage)) {
                attributeStorage = entityManager.newEntity(AttributeStorage.class);
                attributeStorage.setStorageName(dataStorage.getName());
                attributeStorage.setDataFormat(dataStorage.getDataFormat());
                attributeStorage.setCluster(dataStorage.getCluster());
                attributeStorage.setShardType(dataStorage.getShardType());
            }
            domain.getStorage().put(dataStorage.getName(), attributeStorage);
        }
        if (Objects.isNull(attributeStorage.getDataWrapper())) {
            DataWrapper dataWrapper = dataWrapperFactory.createDataWrapper(attributeStorage.getDataFormat());
            try {
                dataWrapper.init(attributeStorage.getData());
            } catch (Exception err) {
                throw new ShardDataBaseException(err);
            }
            attributeStorage.setDataWrapper(dataWrapper);
        }
        return attributeStorage;
    }

    @Override
    public <T extends Domain> Map<String, DataStorage> getDataStorage(Class<T> clazz) {
        return getMapper(clazz).domainEntityMapper.getDataStorage();
    }

    @Override
    public <T extends Domain> boolean lock(T domain) {
        return entityManager.lock(domain.getEntity());
    }

    @Override
    public EntityTransaction getTransaction() {
        return sharedTransactionManager.getTransaction();
    }

    @Override
    public String getTransactionUUID() {
        return entityManager.getTransactionUUID();
    }

    @Override
    public void setAutonomousTransaction() {
        entityManager.setAutonomousTransaction();
    }

    @Override
    public void addParallel() {
        entityManager.addParallel();
    }

    @Override
    public <T extends Domain> List<AttributeHistory> getAttributeHistory(T domain, String attributeName) {
        if (domain == null || attributeName == null) {
            return Collections.EMPTY_LIST;
        }
        Mapper mapper = getMapper(domain.getClass());
        List<AttributeHistory> attributeHistoryList = mapper.domainEntityMapper.mapAttributeHistory(
                entityManager.findAll(
                        AttributeHistoryEntity.class,
                        "x0.C_ENTITY_ID=? and x0.C_ATTRIBUTE_NAME=?",
                        domain.getId(), attributeName));
        attributeHistoryList.addAll(domain.getAttributeHistory());
        return attributeHistoryList;
    }

    private Mapper getMapper(Class<?> clazz) {
        Mapper mapper = currentMapper.get();
        if (mapper != null && currentSourceClass.get() == clazz) {
            return mapper;
        }
        mapper = Optional
                .ofNullable(MAPPERS.get(clazz.getSuperclass()))
                .orElse(MAPPERS.get(clazz));
        if (mapper == null) {
            throw new ShardDataBaseException(
                    String.format(
                            "Can't find DomainEntityMapper for class %s or superclass %s",
                            clazz.getName(),
                            clazz.getSuperclass().getName()
                    )
            );
        }
        currentMapper.set(mapper);
        currentSourceClass.set(clazz);
        return mapper;
    }

    private <T extends Domain> List<T> saveAll(List<T> domains, boolean isUpdate) {
        if (domains == null) {
            return null;
        }
        Class clazz = domains.stream().map(Object::getClass).findAny().orElse(null);
        if (clazz == null) {
            return domains;
        }
        sharedTransactionManager.runInTransaction(() -> {
            if (isUpdate) {
                entityManager.updateAll(mapAllToEntities(clazz, domains));
            } else {
                entityManager.saveAll(mapAllToEntities(clazz, domains));
            }
        });
        return domains;
    }

    @AllArgsConstructor
    private static class Mapper {
        DomainEntityMapper domainEntityMapper;
        Class entityClass;
    }
}
