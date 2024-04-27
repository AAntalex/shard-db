package com.antalex.db.service.impl.managers;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.DataStorage;
import com.antalex.db.service.*;
import com.antalex.db.service.api.DataWrapper;
import com.antalex.db.service.api.DataWrapperFactory;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

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

    @Autowired
    private ShardEntityManager entityManager;

    @Autowired
    private DataWrapperFactory dataWrapperFactory;

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

    @Override
    public <T extends Domain> T find(Class<T> clazz, Long id) {
        return map(clazz, entityManager.find(getMapper(clazz).entityClass, id));
    }

    @Override
    public <T extends Domain> List<T> findAll(Class<T> clazz, String condition, Object... binds) {
        return mapAllToDomains(clazz, entityManager.findAll(getMapper(clazz).entityClass, condition, binds));
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
        Mapper mapper = getMapper(domain.getClass());
        entityManager.save(mapper.domainEntityMapper.map(domain));
        return domain;
    }

    @Override
    public <T extends Domain> T update(T domain) {
        if (domain == null) {
            return null;
        }
        Mapper mapper = getMapper(domain.getClass());
        entityManager.update(mapper.domainEntityMapper.map(domain));
        return domain;
    }

    @Override
    public <T extends Domain> List<T> updateAll(List<T> domains) {
        return saveAll(domains, true);
    }

    @Override
    public <T extends Domain> List<T> saveAll(List<T> domains) {
        return saveAll(domains, false);
    }

    @Override
    public AttributeStorage getAttributeStorage(Domain domain, DataStorage dataStorage) {
        AttributeStorage attributeStorage = domain.getStorage().get(dataStorage.getName());
        if (attributeStorage == null) {
            attributeStorage = entityManager.newEntity(AttributeStorage.class);
            attributeStorage.setStorageName(dataStorage.getName());
            attributeStorage.setDataFormat(dataStorage.getDataFormat());
            DataWrapper dataWrapper = dataWrapperFactory.createDataWrapper(attributeStorage.getDataFormat());
            try {
                dataWrapper.init(null);
            } catch (Exception err) {
                throw new ShardDataBaseException(err);
            }
            attributeStorage.setDataWrapper(dataWrapper);
            attributeStorage.setCluster(dataStorage.getCluster());
            attributeStorage.setShardType(dataStorage.getShardType());
            domain.getStorage().put(dataStorage.getName(), attributeStorage);
        }
        return attributeStorage;
    }

    @Override
    public <T extends Domain> boolean lock(T domain) {
        return entityManager.lock(domain.getEntity());
    }

    @Override
    public EntityTransaction getTransaction() {
        return entityManager.getTransaction();
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

    private <T extends Domain> List<T> saveAll(List<T> domains, boolean isUpdate) {
        if (domains == null) {
            return null;
        }
        Class clazz = domains.stream().map(Object::getClass).findAny().orElse(null);
        if (clazz == null) {
            return domains;
        }
        if (isUpdate) {
            entityManager.updateAll(mapAllToEntities(clazz, domains));
        } else {
            entityManager.saveAll(mapAllToEntities(clazz, domains));
        }
        return domains;
    }

    @AllArgsConstructor
    private static class Mapper {
        DomainEntityMapper domainEntityMapper;
        Class entityClass;
    }
}
