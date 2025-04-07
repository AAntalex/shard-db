package com.antalex.db.service.impl.managers;

import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.SharedTransactionManager;
import com.antalex.db.service.api.ResultQuery;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.service.impl.repository.AttributeStorageRepository;
import com.antalex.db.service.impl.transaction.SharedEntityTransaction;
import com.antalex.db.utils.ShardUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import java.util.*;

@Component
@Primary
public class ShardEntityManagerImpl implements ShardEntityManager {
    private static final Map<Class<?>, ShardEntityRepository> REPOSITORIES = new HashMap<>();

    private final ThreadLocal<ShardEntityRepository<?>> currentShardEntityRepository = new ThreadLocal<>();
    private final ThreadLocal<Class<?>> currentSourceClass = new ThreadLocal<>();

    private final ShardDataBaseManager dataBaseManager;
    private final SharedTransactionManager sharedTransactionManager;
    private final AttributeStorageRepository attributeStorageRepository;

    ShardEntityManagerImpl(
            ShardDataBaseManager dataBaseManager,
            SharedTransactionManager sharedTransactionManager,
            AttributeStorageRepository attributeStorageRepository)
    {
        this.dataBaseManager = dataBaseManager;
        this.sharedTransactionManager = sharedTransactionManager;
        this.attributeStorageRepository = attributeStorageRepository;
    }

    @Autowired
    public void setRepositories(List<ShardEntityRepository<?>> entityRepositories) {
        for (ShardEntityRepository<?> shardEntityRepository : entityRepositories) {
            Class<?>[] classes = GenericTypeResolver
                    .resolveTypeArguments(shardEntityRepository.getClass(), ShardEntityRepository.class);
            if (Objects.nonNull(classes) && classes.length > 0 && !REPOSITORIES.containsKey(classes[0])) {
                shardEntityRepository.setEntityManager(this);
                REPOSITORIES.put(classes[0], shardEntityRepository);
            }
        }
    }

    private <T extends ShardInstance> ShardEntityRepository<T> getEntityRepository(Class<?> clazz) {
        ShardEntityRepository repository = currentShardEntityRepository.get();
        if (repository != null && currentSourceClass.get() == clazz) {
            return repository;
        }
        repository = Optional
                .ofNullable(REPOSITORIES.get(clazz.getSuperclass()))
                .orElse(REPOSITORIES.get(clazz));
        if (repository == null) {
            throw new ShardDataBaseException(
                    String.format(
                            "Can't find ShardEntityRepository for class %s or superclass %s",
                            clazz.getName(),
                            clazz.getSuperclass().getName()
                    )
            );
        }
        currentShardEntityRepository.set(repository);
        currentSourceClass.set(clazz);
        return repository;
    }

    @Override
    public <T extends ShardInstance> ShardType getShardType(T entity) {
        if (entity == null) {
            return null;
        }
        return getEntityRepository(entity.getClass()).getShardType(entity);
    }

    @Override
    public <T extends ShardInstance> ShardType getShardType(Class<T> clazz) {
        return getEntityRepository(clazz).getShardType();
    }

    @Override
    public <T extends ShardInstance> Cluster getCluster(T entity) {
        if (entity == null) {
            return null;
        }
        return getEntityRepository(entity.getClass()).getCluster(entity);
    }

    @Override
    public <T extends ShardInstance> Cluster getCluster(Class<T> clazz) {
        return getEntityRepository(clazz).getCluster();
    }

    @Override
    public <T extends ShardInstance> T save(T entity) {
        return save(entity, false);
    }

    @Override
    public <T extends ShardInstance> Iterable<T> saveAll(Iterable<T> entities) {
        return saveAll(entities, false);
    }

    @Override
    public <T extends ShardInstance> T update(T entity) {
        return save(entity, true);
    }

    @Override
    public <T extends ShardInstance> Iterable<T> updateAll(Iterable<T> entities) {
        return saveAll(entities, true);
    }

    @Override
    public <T extends ShardInstance> void delete(T entity) {
        if (entity == null) {
            return;
        }
        sharedTransactionManager.runInTransaction(() ->
                persist(entity, false, false, true));
    }

    @Override
    public  <T extends ShardInstance> void deleteAll(Iterable<T> entities) {
        if (entities == null) {
            return;
        }
        sharedTransactionManager.runInTransaction(() ->
                entities.forEach(this::delete));
    }

    @Override
    public <T extends ShardInstance> void setDependentStorage(T entity) {
        if (entity == null) {
            return;
        }
        getEntityRepository(entity.getClass()).setDependentStorage(entity);
    }

    @Override
    public <T extends ShardInstance> boolean lock(T entity) {
        if (entity == null) {
            return false;
        }
        try {
            getEntityRepository(entity.getClass()).lock(entity);
            return true;
        } catch (ShardDataBaseException err) {
            return false;
        }
    }

    @Override
    public <T extends ShardInstance> void generateDependentId(T entity) {
        if (entity == null) {
            return;
        }
        getEntityRepository(entity.getClass()).generateDependentId(entity);
    }

    @Override
    public <T extends ShardInstance> void setStorage(T entity, ShardInstance parent, boolean force) {
        if (entity == null || entity.isLazy()) {
            return;
        }
        Cluster cluster = getCluster(entity);
        ShardType shardType = getShardType(entity);
        if (
                Optional.ofNullable(entity.getStorageContext())
                        .map(entityStorage ->
                                Optional.ofNullable(parent)
                                        .map(ShardInstance::getStorageContext)
                                        .filter(it ->
                                                it != entityStorage &&
                                                        shardType != ShardType.REPLICABLE &&
                                                        Objects.nonNull(entityStorage.getShard()) &&
                                                        cluster == it.getCluster() &&
                                                        dataBaseManager.isEnabled(it.getShard())
                                        )
                                        .map(storage ->
                                                Optional.ofNullable(storage.getShard())
                                                        .map(shard -> {
                                                            storage.setShardMap(
                                                                    ShardUtils.addShardMap(
                                                                            storage.getShardMap(),
                                                                            ShardUtils.getShardMap(
                                                                                    entityStorage.getShard().getId()
                                                                            )
                                                                    )
                                                            );
                                                            return false;
                                                        })
                                                        .orElseGet(() -> {
                                                            storage.setShard(entityStorage.getShard());
                                                            storage.setShardMap(
                                                                    ShardUtils.getShardMap(
                                                                            entityStorage.getShard().getId()
                                                                    )
                                                            );
                                                            return true;
                                                        })
                                        )
                                        .orElseGet(() -> {
                                            if (force) {
                                                setDependentStorage(entity);
                                            }
                                            return false;
                                        })
                        )
                        .orElseGet(() -> {
                            entity.setStorageContext(
                                    Optional.ofNullable(parent)
                                            .map(ShardInstance::getStorageContext)
                                            .filter(it ->
                                                    cluster == it.getCluster() &&
                                                            shardType != ShardType.REPLICABLE &&
                                                            dataBaseManager.isEnabled(it.getShard())
                                            )
                                            .map(storage ->
                                                    Optional.ofNullable(storage.getShard())
                                                            .map(shard ->
                                                                    StorageContext.builder()
                                                                            .cluster(cluster)
                                                                            .stored(false)
                                                                            .shard(shard)
                                                                            .shardMap(
                                                                                    ShardUtils.getShardMap(
                                                                                            shard.getId()
                                                                                    )
                                                                            )
                                                                            .build()
                                                            )
                                                            .orElse(storage)
                                            )
                                            .orElse(
                                                    StorageContext.builder()
                                                            .cluster(cluster)
                                                            .temporary(true)
                                                            .stored(false)
                                                            .build()
                                            )
                            );
                            setDependentStorage(entity);
                            return Optional.ofNullable(parent)
                                    .map(ShardInstance::getStorageContext)
                                    .map(StorageContext::isTemporary)
                                    .orElse(false) &&
                                    Objects.nonNull(entity.getStorageContext().getShard());
                        }))
        {
            parent.setStorageContext(
                    StorageContext.builder()
                            .cluster(parent.getStorageContext().getCluster())
                            .shard(parent.getStorageContext().getShard())
                            .shardMap(parent.getStorageContext().getShardMap())
                            .stored(false)
                            .build()
            );
        }
    }

    @Override
    public <T extends ShardInstance> void setStorage(T entity, ShardInstance parent) {
        setStorage(entity, parent, false);
    }

    @Override
    public <T extends ShardInstance> void setAllStorage(Iterable<T> entities, ShardInstance parent) {
        if (entities == null) {
            return;
        }
        entities.forEach(entity -> setStorage(entity, parent));
    }

    @Override
    public <T extends ShardInstance> void generateId(T entity, boolean force) {
        if (entity == null || entity.isLazy()) {
            return;
        }
        if (Objects.isNull(entity.getId())) {
            dataBaseManager.generateId(entity);
            generateDependentId(entity);
        } else {
            if (force) {
                generateDependentId(entity);
            }
        }
    }

    @Override
    public <T extends ShardInstance> void generateId(T entity) {
        generateId(entity, false);
    }

    @Override
    public <T extends ShardInstance> void persist(T entity, boolean onlyChanged) {
        persist(entity, onlyChanged, false, false);
    }

    @Override
    public <T extends ShardInstance> void persistAll(Iterable<T> entities, boolean delete, boolean onlyChanged) {
        if (entities == null) {
            return;
        }
        entities.forEach(it -> persist(it, onlyChanged, false, delete));
    }

    @Override
    public <T extends ShardInstance> void generateAllId(Iterable<T> entities) {
        if (entities == null) {
            return;
        }
        entities.forEach(this::generateId);
    }

    @Override
    public EntityTransaction getTransaction() {
        return sharedTransactionManager.getTransaction();
    }

    @Override
    public String getTransactionUUID() {
        return sharedTransactionManager.getTransactionUUID().toString();
    }

    @Override
    public void setAutonomousTransaction() {
        sharedTransactionManager.setAutonomousTransaction();
    }

    @Override
    public void flush() {
        getTransaction().commit();
    }

    @Override
    public void addParallel() {
        ((SharedEntityTransaction) getTransaction()).addParallel();
    }

    @Override
    public <T extends ShardInstance> TransactionalQuery createQuery(
            T entity,
            String query,
            QueryType queryType,
            QueryStrategy queryStrategy)
    {
        return switch (queryStrategy) {
            case OWN_SHARD -> dataBaseManager.createQuery(entity.getStorageContext().getShard(), query, queryType);
            case MAIN_SHARD ->
                    dataBaseManager.createQuery(entity.getStorageContext().getCluster().getMainShard(), query, queryType);
            case ALL_SHARDS, NEW_SHARDS -> getMainQuery(
                    createQueries(entity, query, queryType, queryStrategy)
            );
        };
    }

    @Override
    public <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(
            T entity,
            String query,
            QueryType queryType,
            QueryStrategy queryStrategy)
    {
        return switch (queryStrategy) {
            case OWN_SHARD, MAIN_SHARD ->
                    Collections.singletonList(createQuery(entity, query, queryType, queryStrategy));
            case ALL_SHARDS -> dataBaseManager.getEntityShards(entity)
                    .map(shard -> dataBaseManager.createQuery(shard, query, queryType))
                    .toList();
            case NEW_SHARDS -> dataBaseManager.getNewShards(entity)
                    .map(shard -> dataBaseManager.createQuery(shard, query, queryType))
                    .toList();
        };
    }

    @Override
    public <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(
            Class<T> clazz,
            String query,
            QueryType queryType)
    {
        ShardEntityRepository<T> repository = getEntityRepository(clazz);
        if (
                Optional.ofNullable(repository.getShardType())
                        .map(it ->  it == ShardType.REPLICABLE)
                        .orElse(false)
        ) {
            return Collections.singletonList(
                    dataBaseManager.createQuery(
                            dataBaseManager.getNextShard(repository.getCluster()), query, queryType)
            );
        } else {
            return createQueries(repository.getCluster(), query, queryType);
        }
    }

    @Override
    public Iterable<TransactionalQuery> createQueries(
            Cluster cluster,
            String query,
            QueryType queryType)
    {
        return dataBaseManager.getEnabledShards(cluster)
                .map(shard -> dataBaseManager.createQuery(shard, query, queryType))
                .toList();
    }

    @Override
    public <T extends ShardInstance> TransactionalQuery createQuery(Class<T> clazz, String query, QueryType queryType) {
        return getMainQuery(createQueries(clazz, query, queryType));
    }

    @Override
    public TransactionalQuery createQuery(Cluster cluster, String query, QueryType queryType) {
        return getMainQuery(createQueries(cluster, query, queryType));
    }

    @Override
    public <T extends ShardInstance> T newEntity(Class<T> clazz) {
        ShardEntityRepository<T> repository = getEntityRepository(clazz);
        return repository.newEntity();
    }

    @Override
    public <T extends ShardInstance> T getEntity(Class<T> clazz, Long id) {
        if (Optional.ofNullable(id).map(it -> it.equals(0L)).orElse(true)) {
            return null;
        }
        SharedEntityTransaction transaction = (SharedEntityTransaction) getTransaction();
        T entity = transaction.getPersistentObject(clazz, id);
        if (Objects.isNull(entity)) {
            ShardEntityRepository<T> repository = getEntityRepository(clazz);
            entity = repository.getEntity(id, dataBaseManager.getStorageContext(id));
            addEntity(transaction, clazz, entity, id);
        }
        return entity;
    }

    @Override
    public <T extends ShardInstance> T find(Class<T> clazz, Long id, Map<String, DataStorage> storageMap) {
        return find(getEntity(clazz, id), storageMap);
    }

    @Override
    public <T extends ShardInstance> T find(
            Class<T> clazz,
            Map<String, DataStorage> storageMap,
            String condition,
            Object... binds)
    {
        return sharedTransactionManager.runInTransaction(() -> {
            ShardEntityRepository<T> repository = getEntityRepository(clazz);
            return repository.find(storageMap, condition, binds);
        });
    }

    @Override
    public <T extends ShardInstance> List<T> findAllLimit(
            Class<T> clazz,
            Map<String, DataStorage> storageMap,
            Integer limit,
            String condition,
            Object... binds)
    {
        return sharedTransactionManager.runInTransaction(() -> {
            ShardEntityRepository<T> repository = getEntityRepository(clazz);
            return repository.findAll(storageMap, limit, condition, binds);
        });
    }

    @Override
    public <T extends ShardInstance> List<T> findAll(
            Class<T> clazz,
            ShardInstance parent,
            Map<String, DataStorage> storageMap,
            String condition,
            Object... binds)
    {
        return sharedTransactionManager.runInTransaction(() -> {
            ShardEntityRepository<T> repository = getEntityRepository(clazz);
            return repository.findAll(parent, storageMap, condition, binds);
        });
    }

    @Override
    public <T extends ShardInstance> List<T> findAllByIds(
            Class<T> clazz,
            Map<String, DataStorage> storageMap,
            String condition,
            List<Long> ids)
    {
        return sharedTransactionManager.runInTransaction(() -> {
            ShardEntityRepository<T> repository = getEntityRepository(clazz);
            return repository.findAll(storageMap, ids, condition);
        });
    }

    @Override
    public <T extends ShardInstance> List<T> skipLocked(
            Class<T> clazz,
            Integer limit,
            String condition,
            Object... binds)
    {
        ShardEntityRepository<T> repository = getEntityRepository(clazz);
        return repository.skipLocked(limit, condition, binds);
    }

    @Override
    public <T extends ShardInstance> T extractValues(T entity, ResultQuery result, int index) {
        if (entity == null) {
            return null;
        }
        ShardEntityRepository<T> repository = getEntityRepository(entity.getClass());
        return repository.extractValues(entity, result, index);
    }

    @Override
    public <T extends ShardInstance> T extractValues(Class<T> clazz, ResultQuery result, int index) {
        ShardEntityRepository<T> repository = getEntityRepository(clazz);
        return repository.extractValues(null, result, index);
    }

    @Override
    public <T extends ShardInstance> Map<String, String> getFieldMap(Class<T> clazz) {
        return getEntityRepository(clazz).getFieldMap();
    }

    @Override
    public List<AttributeStorage> extractAttributeStorage(
            Map<String, DataStorage> storageMap,
            ResultQuery result,
            Cluster cluster,
            int index)
    {
        List<AttributeStorage> attributeStorageList = new ArrayList<>();
        if (Objects.nonNull(storageMap)) {
            for (DataStorage dataStorage : storageMap.values()) {
                if (
                        dataStorage.getFetchType() == FetchType.EAGER &&
                                Optional.ofNullable(dataStorage.getCluster())
                                        .map(it -> it == cluster)
                                        .orElse(true)
                ) {
                    AttributeStorage attributeStorage =
                            attributeStorageRepository.extractValues(null, result, index);
                    if (Objects.nonNull(attributeStorage)) {
                        attributeStorage.setCluster(cluster);
                        attributeStorage.setShardType(dataStorage.getShardType());
                        attributeStorageList.add(attributeStorage);
                    }
                    index = index + 6;
                }
            }
        }
        return attributeStorageList;
    }

    @Override
    public <T extends ShardInstance> T find(T entity, Map<String, DataStorage> storageMap) {
        if (entity == null) {
            return null;
        }
        return sharedTransactionManager.runInTransaction(() -> {
            ShardEntityRepository<T> repository = getEntityRepository(entity.getClass());
            return repository.find(entity, storageMap);
        });
    }

    @Override
    public AttributeStorage findAttributeStorage(ShardInstance parent, DataStorage storage) {
        return attributeStorageRepository.find(parent, storage);
    }

    private TransactionalQuery getMainQuery(Iterable<TransactionalQuery> queries) {
        TransactionalQuery mainQuery = null;
        for (TransactionalQuery query : queries) {
            if (Objects.isNull(mainQuery)) {
                mainQuery = query;
            } else {
                mainQuery.addRelatedQuery(query);
            }
        }
        return mainQuery;
    }

    private <T extends ShardInstance> T save(T entity, boolean onlyChanged) {
        setStorage(entity, null, true);
        generateId(entity, true);
        sharedTransactionManager.runInTransaction(() ->
                persist(entity, onlyChanged, true, false));
        return entity;
    }

    private  <T extends ShardInstance> Iterable<T> saveAll(Iterable<T> entities, boolean onlyChanged) {
        if (entities == null) {
            return null;
        }
        sharedTransactionManager.runInTransaction(() ->
                entities.forEach(it -> save(it, onlyChanged)));
        return entities;
    }

    private <T extends ShardInstance> void persist(T entity, boolean onlyChanged, boolean force, boolean delete) {
        EntityTransaction transaction = getTransaction();
        if (
                Optional.ofNullable(entity)
                        .map(ShardInstance::getStorageContext)
                        .map(it -> delete || !it.isLazy())
                        .orElse(false) &&
                        entity.setTransactionalContext(transaction))
        {
            ShardEntityRepository<T> repository = getEntityRepository(entity.getClass());
            if (!delete) {
                checkShardMap(entity, repository.getShardType(entity));
                entity.getAttributeStorage()
                        .forEach(attributeStorage -> {
                            if (Objects.isNull(attributeStorage.getCluster())) {
                                attributeStorage.setCluster(entity.getStorageContext().getCluster());
                            }
                            setStorage(attributeStorage, entity);
                            generateId(attributeStorage);
                            attributeStorage.setEntityId(entity.getId());
                        });
                entity.getAttributeHistory()
                        .forEach(attributeHistory -> {
                            if (Objects.isNull(attributeHistory.getCluster())) {
                                attributeHistory.setCluster(entity.getStorageContext().getCluster());
                            }
                            setStorage(attributeHistory, entity);
                            generateId(attributeHistory);
                            attributeHistory.setEntityId(entity.getId());
                        });
            }
            if (
                    delete && entity.isStored() ||
                            !delete && (force || !entity.isStored() || entity.isChanged() || entity.hasNewShards())
            ) {
                repository.persist(entity, delete, onlyChanged);
                entity.getStorageContext().persist(delete);
                entity.getAttributeStorage().forEach(it -> persist(it, onlyChanged, force, delete));
                if (!delete) {
                    addEntity(
                            (SharedEntityTransaction) transaction,
                            entity.getClass().getSuperclass(),
                            entity,
                            entity.getId()
                    );
                }
            }
            entity.getAttributeStorage().clear();
            entity.getAttributeHistory().clear();
        }
    }

    private <T extends ShardInstance> void addEntity(
            SharedEntityTransaction transaction,
            Class<?> clazz,
            T entity,
            Long id)
    {
        if (entity == null) {
            return;
        }
        transaction.addPersistentObject(clazz, id, entity);
    }

    private void checkShardMap(ShardInstance entity, ShardType shardType) {
        Long shardMap = entity.getStorageContext().getShardMap();
        if (shardType == ShardType.REPLICABLE && !shardMap.equals(0L))
        {
            entity.getStorageContext().setShardMap(0L);
        }
        if (shardType == ShardType.SHARDABLE
                && !shardMap.equals(ShardUtils.getShardMap(entity.getStorageContext().getShard().getId())))
        {
            throw new ShardDataBaseException("У шардируемой сущности не может быть определенно более 1 шарды.");
        }
    }
}