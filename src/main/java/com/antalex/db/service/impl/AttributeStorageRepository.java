package com.antalex.db.service.impl;

import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.AttributeStorageInterceptor;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.api.ResultQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;


@Component
public class AttributeStorageRepository implements ShardEntityRepository<AttributeStorage> {
    private static final String UPD_QUERY_PREFIX = "UPDATE $$$.APP_ATTRIBUTE_STORAGE SET SN=SN+1,ST=?,SHARD_MAP=?";
    private static final String INS_QUERY = "INSERT INTO $$$.APP_ATTRIBUTE_STORAGE (SN,ST,SHARD_MAP,C_ENTITY_ID,C_STORAGE_NAME,C_DATA,C_DATA_FORMAT,ID) VALUES (0,?,?,?,?,?,?,?)";
    private static final String UPD_QUERY = "UPDATE $$$.APP_ATTRIBUTE_STORAGE SET SN=SN+1,ST=?,SHARD_MAP=?,C_ENTITY_ID=?,C_STORAGE_NAME=?,C_DATA=?,C_DATA_FORMAT=? WHERE ID=?";
    private static final String LOCK_QUERY = "SELECT ID FROM $$$.APP_ATTRIBUTE_STORAGE WHERE ID=? FOR UPDATE NOWAIT";
    private static final String SELECT_QUERY = "SELECT x0.ID,x0.SHARD_MAP,x0.C_ENTITY_ID,x0.C_STORAGE_NAME,x0.C_DATA,x0.C_DATA_FORMAT FROM $$$.APP_ATTRIBUTE_STORAGE x0 WHERE x0.SHARD_MAP>=0";
    private static final String DELETE_QUERY = "DELETE FROM $$$.APP_ATTRIBUTE_STORAGE WHERE ID=?";

    private static final List<String> COLUMNS = Arrays.asList(
            "C_ENTITY_ID",
            "C_STORAGE_NAME",
            "C_DATA",
            "C_DATA_FORMAT"
    );
    private final Map<Long, String> updateQueries = new HashMap<>();

    private ShardEntityManager entityManager;

    @Override
    public void setEntityManager(ShardEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public AttributeStorage newEntity() {
        return new AttributeStorageInterceptor();
    }

    @Override
    public AttributeStorage getEntity(Long id, StorageContext storageContext) {
        AttributeStorageInterceptor entity = new AttributeStorageInterceptor();
        entity.setId(id);
        entity.setStorageContext(storageContext);
        entity.setEntityManager(entityManager);
        entity.init();
        return entity;
    }

    @Override
    public ShardType getShardType(AttributeStorage entity) {
        return entity.getShardType();
    }

    @Override
    public ShardType getShardType() {
        return null;
    }

    @Override
    public Cluster getCluster(AttributeStorage entity) {
        return entity.getCluster();
    }

    @Override
    public Cluster getCluster() {
        return null;
    }

    @Override
    public void setDependentStorage(AttributeStorage entity) {
    }

    @Override
    public void persist(AttributeStorage entity, boolean delete, boolean onlyChanged) {
        if (delete) {
            entityManager
                    .createQueries(entity, DELETE_QUERY, QueryType.DML)
                    .forEach(query -> query.bind(entity.getId()).addBatch());
        } else {
            String sql = entity.isStored() ? (onlyChanged ? getUpdateSQL(entity.getChanges()) : UPD_QUERY) : INS_QUERY;
            if (Objects.nonNull(sql)) {
                boolean checkChanges = onlyChanged && entity.isStored();
                entityManager
                        .createQueries(entity, sql, QueryType.DML)
                        .forEach(query ->
                                query
                                        .bind(entityManager.getTransactionUUID())
                                        .bindShardMap(entity)
                                        .bind(entity.getEntityId(), checkChanges && !entity.isChanged(1))
                                        .bind(entity.getStorageName(), checkChanges && !entity.isChanged(2))
                                        .bind(entity.getData(), checkChanges && !entity.isChanged(3))
                                        .bind(entity.getDataFormat(), checkChanges && !entity.isChanged(4))
                                        .bind(entity.getId())
                                        .addBatch()
                        );
            }
            additionalPersist(entity);
        }
    }

    @Override
    public void generateDependentId(AttributeStorage entity) {
    }

    @Override
    public void lock(AttributeStorage entity) {
        entityManager
                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)
                .bind(entity.getId())
                .execute();
    }

    @Override
    public AttributeStorage extractValues(AttributeStorage entity, ResultQuery result, int index) {
        try {
            if (!Optional.ofNullable(result.getLong(++index)).map(it -> it == 0L).orElse(true)) {
                AttributeStorageInterceptor entityInterceptor =
                        (AttributeStorageInterceptor) Optional.ofNullable(entity)
                                .orElse(entityManager.getEntity(AttributeStorage.class, result.getLong(index)));
                entityInterceptor.setShardMap(result.getLong(++index));
                entityInterceptor.setEntityId(result.getLong(++index), false);
                entityInterceptor.setStorageName(result.getString(++index), false);
                entityInterceptor.setData(result.getString(++index), false);
                entityInterceptor.setDataFormat(result.getObject(++index, DataFormat.class), false);
                entityInterceptor.getStorageContext().setLazy(false);
                entityInterceptor.init();
                return entityInterceptor;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return null;
    }

    @Override
    public AttributeStorage find(AttributeStorage entity, Map<String, DataStorage> storageMap) {
        try {
            ResultQuery result = entityManager
                    .createQuery(entity, SELECT_QUERY + " and x0.ID=?", QueryType.SELECT, QueryStrategy.OWN_SHARD)
                    .bind(entity.getId())
                    .getResult();
            if (result.next()) {
                int index = 0;
                extractValues(entity, result, index);
            } else {
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return entity;
    }

    public AttributeStorage find(ShardInstance parent, DataStorage storage) {
        try {
            Cluster cluster =
                    Optional.ofNullable(storage.getCluster())
                            .orElse(parent.getStorageContext().getCluster());
            ResultQuery result = entityManager
                    .createQuery(
                            cluster,
                            SELECT_QUERY + " and x0.C_ENTITY_ID=? and x0.C_STORAGE_NAME=?",
                            QueryType.SELECT
                    )
                    .bind(parent.getId())
                    .bind(storage.getName())
                    .getResult();
            if (result.next()) {
                AttributeStorage entity = entityManager.getEntity(AttributeStorage.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                entity.setCluster(cluster);
                entity.setShardType(storage.getShardType());
                return entity;
            } else {
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public List<AttributeStorage> findAll(
            Map<String, DataStorage> storageMap,
            Integer limit,
            String condition,
            Object... binds) {
        return findAll(
                entityManager
                        .createQuery(
                                AttributeStorage.class, 
                                SELECT_QUERY +
                                        Optional.ofNullable(condition)
                                                .map(it -> " and " + it)
                                                .orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .bindAll(binds)
                        .getResult()
        );
    }

    @Override
    public List<AttributeStorage> findAll(
            ShardInstance parent,
            Map<String, DataStorage> storageMap,
            String condition,
            Object... binds)
    {
        return findAll(
                entityManager
                        .createQuery(
                                parent,
                                SELECT_QUERY +
                                        Optional.ofNullable(condition)
                                                .map(it -> " and " + it)
                                                .orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .bindAll(binds)
                        .getResult()
        );
    }

    @Override
    public List<AttributeStorage> skipLocked(
            Integer limit,
            String condition,
            Object... binds) {

        return findAll(
                entityManager
                        .createQuery(
                                AttributeStorage.class,
                                SELECT_QUERY +
                                        Optional.ofNullable(condition)
                                                .map(it -> " and " + it)
                                                .orElse(StringUtils.EMPTY) +
                                        " FOR UPDATE OF x0 SKIP LOCKED",
                                QueryType.LOCK
                        )
                        .fetchLimit(limit)
                        .bindAll(binds)
                        .getResult()
        );
    }

    private void additionalPersist(AttributeStorage entity) {
        if (entity.hasNewShards()) {
            entityManager
                    .createQueries(entity, INS_QUERY, QueryType.DML, QueryStrategy.NEW_SHARDS)
                    .forEach(query ->
                            query
                                    .bind(entityManager.getTransactionUUID())
                                    .bindShardMap(entity)
                                    .bind(entity.getEntityId())
                                    .bind(entity.getStorageName())
                                    .bind(entity.getData())
                                    .bind(entity.getDataFormat())
                                    .bind(entity.getId())
                                    .addBatch()
                    );
        }
    }

    private List<AttributeStorage> findAll(ResultQuery result) {
        List<AttributeStorage> entities = new ArrayList<>();
        try {
            while (result.next()) {
                AttributeStorage entity = entityManager.getEntity(AttributeStorage.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                entities.add(entity);
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return entities;
    }

    private String getUpdateSQL(Long changes) {
        if (
                Optional.ofNullable(changes)
                .map(it -> it.equals(0L) && COLUMNS.size() <= Long.SIZE)
                .orElse(true)) 
        {
            return null;
        }
        String sql = updateQueries.get(changes);
        if (Objects.isNull(sql)) {
            sql = IntStream.range(0, COLUMNS.size())
                    .filter(idx -> idx > Long.SIZE || (changes & (1L << idx)) > 0L)
                    .mapToObj(idx -> "," + COLUMNS.get(idx) + "=?")
                    .reduce(UPD_QUERY_PREFIX, String::concat) + " WHERE ID=?";
            updateQueries.put(changes, sql);
        }
        return sql;
    }
}
