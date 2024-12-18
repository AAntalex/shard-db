package com.antalex.db.service.impl.repository;

import com.antalex.db.model.StorageContext;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.api.QueryQueue;
import com.antalex.db.service.api.TransactionalQuery;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.antalex.db.entity.AttributeHistoryEntity;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.api.ResultQuery;
import com.antalex.db.utils.Utils;

import java.util.*;


@Component
public class AttributeHistoryRepository implements ShardEntityRepository<AttributeHistoryEntity> {
    private static final ShardType SHARD_TYPE = ShardType.SHARDABLE;
    private static final String INS_QUERY = "INSERT INTO $$$.APP_ATTRIBUTE_HISTORY (SN,ST,SHARD_MAP,C_ENTITY_ID,C_ATTRIBUTE_NAME,C_TIME,C_VALUE,ID) VALUES (0,?,?,?,?,?,?,?)";
    private static final String SELECT_QUERY = "SELECT x0.ID,x0.SHARD_MAP,x0.C_ENTITY_ID,x0.C_ATTRIBUTE_NAME,x0.C_TIME,x0.C_VALUE FROM $$$.APP_ATTRIBUTE_HISTORY x0 WHERE x0.SHARD_MAP>=0";
    private static final String DELETE_QUERY = "DELETE FROM $$$.APP_ATTRIBUTE_HISTORY WHERE ID=?";
    private static final String LOCK_QUERY = "SELECT ID FROM $$$.APP_ATTRIBUTE_HISTORY WHERE ID=? FOR UPDATE NOWAIT";

    private static final Map<String, String> FIELD_MAP = ImmutableMap.<String, String>builder()
            .put("entityId", "C_ENTITY_ID")
            .put("attributeName", "C_ATTRIBUTE_NAME")
            .put("time", "C_TIME")
            .put("value", "C_VALUE")
            .build();

    private ShardEntityManager entityManager;
    private final ShardDataBaseManager dataBaseManager;

    @Autowired
    AttributeHistoryRepository(ShardDataBaseManager dataBaseManager) {
        this.dataBaseManager = dataBaseManager;
    }

    @Override
    public void setEntityManager(ShardEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Map<String, String> getFieldMap() {
        return FIELD_MAP;
    }

    @Override
    public AttributeHistoryEntity newEntity() {
        return new AttributeHistoryEntity();
    }

    @Override
    public AttributeHistoryEntity getEntity(Long id, StorageContext storageContext) {
        AttributeHistoryEntity entity = new AttributeHistoryEntity();
        entity.setId(id);
        entity.setStorageContext(storageContext);
        return entity;
    }

    @Override
    public ShardType getShardType() {
        return SHARD_TYPE;
    }

    @Override
    public ShardType getShardType(AttributeHistoryEntity entity) {
        return SHARD_TYPE;
    }

    @Override
    public Cluster getCluster() {
        return null;
    }

    @Override
    public Cluster getCluster(AttributeHistoryEntity entity) {
        return Optional.ofNullable(entity).map(ShardInstance::getCluster).orElse(null);
    }

    @Override
    public void setDependentStorage(AttributeHistoryEntity entity) {
    }

    @Override
    public void persist(AttributeHistoryEntity entity, boolean delete, boolean onlyChanged) {
        if (delete) {
            entityManager
                    .createQueries(entity, DELETE_QUERY, QueryType.DML)
                    .forEach(query -> query.bind(entity.getId()).addBatch());
        } else {
            String sql = entity.isStored() ? null : INS_QUERY;
            if (Objects.nonNull(sql)) {
                boolean checkChanges = onlyChanged && entity.isStored();
                entityManager
                        .createQueries(entity, sql, QueryType.DML)
                        .forEach(query ->
                                query
                                        .bind(entityManager.getTransactionUUID())
                                        .bindShardMap(entity)
                                        .bind(entity.getEntityId(), checkChanges && !entity.isChanged(1))
                                        .bind(entity.getAttributeName(), checkChanges && !entity.isChanged(2))
                                        .bind(entity.getTime(), checkChanges && !entity.isChanged(3))
                                        .bind(entity.getValue(), checkChanges && !entity.isChanged(4))
                                        .bind(entity.getId())
                                        .addBatch()
                        );
            }
        }
    }

    @Override
    public void generateDependentId(AttributeHistoryEntity entity) {
    }

    @Override
    public void lock(AttributeHistoryEntity entity) {
        entityManager
                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)
                .bind(entity.getId())
                .execute();
    }


    @Override
    public AttributeHistoryEntity extractValues(AttributeHistoryEntity entity, ResultQuery result, int index) {
        try {
            if (!Optional.ofNullable(result.getLong(++index)).map(it -> it == 0L).orElse(true)) {
                if (entity == null) {
                    entity = entityManager.getEntity(AttributeHistoryEntity.class, result.getLong(index));
                }
                entity.setShardMap(result.getLong(++index));
                entity.setEntityId(result.getLong(++index));
                entity.setAttributeName(result.getString(++index));
                entity.setTime(result.getOffsetDateTime(++index));
                entity.setValue(result.getString(++index));
                entity.getStorageContext().setLazy(false);
                return entity;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return null;
    }

    @Override
    public AttributeHistoryEntity find(AttributeHistoryEntity entity, Map<String, DataStorage> storageMap) {
        try {
            ResultQuery result = entityManager
                    .createQuery(
                            entity,
                            SELECT_QUERY + " and x0.ID=?",
                            QueryType.SELECT,
                            QueryStrategy.OWN_SHARD
                    )
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

    @Override
    public AttributeHistoryEntity find(Map<String, DataStorage> storageMap, String condition, Object... binds) {
        try {
            ResultQuery result = entityManager
                    .createQuery(
                            AttributeHistoryEntity.class,
                            SELECT_QUERY +
                                    Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                            .map(it -> " and " + it)
                                            .orElse(StringUtils.EMPTY),
                            QueryType.SELECT
                    )
                    .fetchLimit(1)
                    .bindAll(binds)
                    .getResult();
            if (result.next()) {
                AttributeHistoryEntity entity = entityManager.getEntity(AttributeHistoryEntity.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                return entity;
            } else {
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public List<AttributeHistoryEntity>  findAll(
            Map<String, DataStorage> storageMap,
            Integer limit,
            String condition,
            Object... binds)
    {
        return findAll(
                entityManager
                        .createQuery(
                                AttributeHistoryEntity.class, 
                                SELECT_QUERY +
                                        " and " +
                                        Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                                .orElse("x0.ID in (<IDS>)"),
                                QueryType.SELECT
                        )
                        .fetchLimit(limit)
                        .bindAll(binds)
                        .getResult()
        );
    }

    @Override
    public List<AttributeHistoryEntity> skipLocked(
            Integer limit,
            String condition,
            Object... binds) {
        return findAll(
                entityManager
                        .createQuery(
                                AttributeHistoryEntity.class,
                                SELECT_QUERY +
                                        Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                                .map(it -> " and " + it)
                                                .orElse(StringUtils.EMPTY) +
                                " FOR UPDATE SKIP LOCKED",
                                QueryType.LOCK
                        )
                        .fetchLimit(limit)
                        .bindAll(binds)
                        .getResult()
        );
    }

    @Override
    public List<AttributeHistoryEntity> findAll(
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
                                        Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                                .map(it -> " and " + it)
                                                .orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .bindAll(binds)
                        .getResult()
        );
    }

    @Override
    public List<AttributeHistoryEntity> findAll(
            Map<String, DataStorage> storageMap,
            List<Long> ids,
            String condition)
    {
        List<AttributeHistoryEntity> result = new ArrayList<>();
        QueryQueue queue = dataBaseManager
            .createQueryQueueByIds(
                    SELECT_QUERY +
                            " and " +
                            Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                    .orElse("x0.ID in (<IDS>)"),
                    ids
            );
        while (true) {
            if (
                    !result.addAll(
                    Optional
                            .ofNullable(queue.get())
                            .map(TransactionalQuery::getResult)
                            .map(this::findAll)
                            .orElse(Collections.emptyList())
                    )
            ) break;
        }
        return result;
    }

    private List<AttributeHistoryEntity> findAll(ResultQuery result) {
        List<AttributeHistoryEntity> entities = new ArrayList<>();
        try {
            while (result.next()) {
                AttributeHistoryEntity entity =
                        entityManager.getEntity(AttributeHistoryEntity.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                entities.add(entity);
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return entities;
    }
}
