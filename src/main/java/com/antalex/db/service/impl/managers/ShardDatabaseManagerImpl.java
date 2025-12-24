package com.antalex.db.service.impl.managers;

import com.antalex.db.config.*;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.*;
import com.antalex.db.service.LockManager;
import com.antalex.db.service.SharedTransactionManager;
import com.antalex.db.service.api.*;
import com.antalex.db.service.impl.sequences.ApplicationSequenceGenerator;
import com.antalex.db.service.impl.sequences.SimpleSequenceGenerator;
import com.antalex.db.utils.ShardUtils;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.impl.transaction.SharedEntityTransaction;
import com.antalex.db.service.impl.transaction.TransactionalSQLTask;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
public class ShardDatabaseManagerImpl implements ShardDataBaseManager {
    private static final String INIT_CHANGE_LOG = "db/core/db.changelog-init.yaml";
    private static final String CLASSPATH = "classpath:";
    private static final String DEFAULT_CHANGE_LOG_PATH = "classpath:db/changelog";
    private static final String DEFAULT_CHANGE_LOG_NAME = "db.changelog-master.yaml";
    private static final String CLUSTERS_PATH = "clusters";
    private static final String SHARDS_PATH = "shards";
    private static final String MAIN_SEQUENCE = "SEQ_ID";
    private static final int DEFAULT_TIME_OUT_DB_PROCESSOR = 10;
    private static final long DEFAULT_TIME_OUT_LOCK_PROCESSOR = 60;
    private static final long DEFAULT_DELAY_LOCK_PROCESSOR = 10;
    private static final int SQL_IN_CLAUSE_LIMIT = 100;
    private static final int PERCENT_OF_ACTIVE_CONNECTION_FOR_PARALLEL_LIMIT = 50;
    private static final String SELECT_DB_INFO = "SELECT C_SHARD_ID,C_MAIN_SHARD,C_CLUSTER_ID,C_CLUSTER_NAME," +
            "C_DEFAULT_CLUSTER,C_SEGMENT_NAME,C_ACCESSIBLE FROM $$$.APP_DATABASE";
    private static final String INS_DB_INFO = "INSERT INTO $$$.APP_DATABASE " +
            "(C_SHARD_ID,C_MAIN_SHARD,C_CLUSTER_ID,C_CLUSTER_NAME,C_DEFAULT_CLUSTER,C_SEGMENT_NAME,C_ACCESSIBLE) "+
            " VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SAVE_TRANSACTION_QUERY = "INSERT INTO $$$.APP_TRANSACTION " +
            "(C_UUID,C_EXECUTE_TIME,C_CHUNKS,C_ELAPSED_TIME,C_ALL_ELAPSED_TIME,C_FAILED,C_ERROR_TEXT) " +
            "VALUES (?,?,?,?,?,?,?)";
    private static final String SAVE_DML_QUERY = "INSERT INTO $$$.APP_DML_QUERY " +
            "(C_TRN_UUID,C_QUERY_ORDER,C_SQL_TEXT,C_ROWS_PROCESSED,C_ELAPSED_TIME) VALUES (?,?,?,?,?)";

    private static final String SELECT_DYNAMIC_DB_INFO ="SELECT C_SEGMENT_NAME,C_ACCESSIBLE FROM $$$.APP_DATABASE";

    private final ResourceLoader resourceLoader;
    private final MultiDataBaseConfig multiDataBaseConfig;
    private final SharedTransactionManager sharedTransactionManager;
    private final TransactionalSQLTaskFactory taskFactory;
    private final TransactionalRemoteTaskFactory remoteTaskFactory;
    private final ExecutorService executorService;
    private final LockManager lockManager;

    private Cluster defaultCluster;
    private final Map<String, Cluster> clusters = new HashMap<>();
    private final Map<Short, Cluster> clusterIds = new HashMap<>();
    private final Map<String, SequenceGenerator> shardSequences = new HashMap<>();
    private final Map<String, Map<Integer, SequenceGenerator>> sequences = new HashMap<>();
    private final Map<Integer, DataBaseInstance> shards = new HashMap<>();
    private final List<ImmutablePair<Cluster, DataBaseInstance>> newShards = new ArrayList<>();
    private final Map<Integer, QueryQueue> queryQueueMap = new ConcurrentHashMap<>();

    private String changLogPath;
    private String changLogName;
    private Boolean liquibaseEnable;
    private String segment;
    private int sqlInClauseLimit;
    private int timeOutDbProcessor;

    @Autowired
    ShardDatabaseManagerImpl(
            ResourceLoader resourceLoader,
            MultiDataBaseConfig multiDataBaseConfig,
            SharedTransactionManager sharedTransactionManager,
            TransactionalSQLTaskFactory taskFactory,
            TransactionalRemoteTaskFactory remoteTaskFactory,
            LockManager lockManager)
    {
        this.resourceLoader = resourceLoader;
        this.multiDataBaseConfig = multiDataBaseConfig;
        this.sharedTransactionManager = sharedTransactionManager;
        this.taskFactory = taskFactory;
        this.remoteTaskFactory = remoteTaskFactory;
        this.executorService = Executors.newCachedThreadPool();
        this.taskFactory.setExecutorService(this.executorService);
        this.remoteTaskFactory.setExecutorService(this.executorService);
        this.lockManager = lockManager;
        getProperties();
        runInitLiquibase();
        processDataBaseInfo();
        runLiquibase();
        runScheduleDatabaseProcessor();
    }

    @Override
    public TransactionalTask getTransactionalTask(DataBaseInstance shard) {
        SharedEntityTransaction transaction = (SharedEntityTransaction) sharedTransactionManager.getTransaction();
        return Optional.ofNullable(
                transaction.getCurrentTask(
                        shard,
                        !shard.getRemote() &&
                                getActiveConnections(shard) >= shard.getActiveConnectionParallelLimit()
                )
        )
                .orElseGet(() -> createTransactionalTask(shard, transaction));
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(defaultCluster.getMainShard());
    }

    @Override
    public DataSource getDataSource() {
        return defaultCluster.getMainShard().getDataSource();
    }

    @Override
    public Cluster getCluster(Short id) {
        if (id == null) {
            throw new ShardDataBaseException("Не указан идентификатор кластера");
        }
        Cluster cluster = clusterIds.get(id);
        if (cluster == null) {
            throw new ShardDataBaseException("Отсутствует кластер с идентификатором " + id);
        }
        return cluster;
    }

    @Override
    public Cluster getCluster(String clusterName) {
        return Optional.ofNullable(clusterName).map(clusters::get).orElse(defaultCluster);
    }

    @Override
    public Cluster getDefaultCluster() {
        return defaultCluster;
    }

    @Override
    public DataBaseInstance getShard(Cluster cluster, Short id) {
        if (cluster == null) {
            throw new ShardDataBaseException("Не указан кластер");
        }
        if (id == null) {
            throw new ShardDataBaseException("Не указан идентификатор шарды");
        }
        DataBaseInstance shard = cluster.getShardMap().get(id);
        if (shard == null) {
            throw new ShardDataBaseException(
                    String.format("Отсутствует шарда с идентификатором '%d' в кластере '%s'", id, cluster.getName())
            );
        }
        return shard;
    }

    @Override
    public void generateId(ShardInstance entity) {
        if (entity == null) {
            return;
        }
        StorageContext storageContext = entity.getStorageContext();
        Assert.notNull(storageContext, "Не определены аттрибуты хранения");
        Assert.notNull(
                storageContext.getCluster(),
                "Не верно определены аттрибуты хранения. Не определен кластер"
        );
        if (Objects.isNull(storageContext.getShard())) {
            storageContext.setShard(
                    getNextShard(storageContext.getCluster())
            );
            storageContext.setShardMap(ShardUtils.getShardMap(storageContext.getShard().getId()));
        }

        if (storageContext.isTemporary()) {
            entity.setStorageContext(
                    StorageContext.builder()
                            .cluster(storageContext.getCluster())
                            .shard(storageContext.getShard())
                            .shardMap(storageContext.getShardMap())
                            .stored(false)
                            .build()
            );
        }
        entity.setId(
                (sequenceNextVal(MAIN_SEQUENCE, storageContext.getCluster()) * ShardUtils.MAX_REPLICATIONS
                         * ShardUtils.MAX_CLUSTERS + storageContext.getCluster().getId() - 1
                ) * ShardUtils.MAX_SHARDS + storageContext.getShard().getId() - 1
        );
    }

    @Override
    public Stream<DataBaseInstance> getEnabledShards(Cluster cluster) {
        return Optional.ofNullable(cluster)
                .map(Cluster::getShards)
                .map(List::stream)
                .orElse(
                        clusters.values()
                                .stream()
                                .map(Cluster::getShards)
                                .flatMap(List::stream)
                                .distinct()
                ).filter(this::isEnabled);
    }

    @Override
    public Stream<DataBaseInstance> getEntityShards(ShardInstance entity) {
        return getShardsFromValue(
                entity,
                entity.isStored() ?
                        entity.getStorageContext().getOriginalShardMap() :
                        entity.getStorageContext().getShardMap(),
                false
        );
    }

    @Override
    public Stream<DataBaseInstance> getNewShards(ShardInstance entity) {
        if (entity.isStored()) {
            return getShardsFromValue(
                    entity,
                    entity.getStorageContext().getOriginalShardMap() ^
                                    entity.getStorageContext().getShardMap(),
                    true
            );
        } else {
            return Stream.empty();
        }
    }

    @Override
    public long sequenceNextVal(String sequenceName, DataBaseInstance shard) {
        return getSequenceGenerator(sequenceName, shard).nextValue();
    }

    @Override
    public long sequenceNextVal(String sequenceName, Cluster cluster) {
        return sequenceNextVal(sequenceName, cluster.getMainShard());
    }

    @Override
    public long sequenceNextVal(String sequenceName) {
        return sequenceNextVal(sequenceName, getDefaultCluster());
    }

    @Override
    public long sequenceNextVal() {
        return sequenceNextVal(MAIN_SEQUENCE, getDefaultCluster());
    }

    @Override
    public long sequenceCurVal(String sequenceName, DataBaseInstance shard) {
        return getSequenceGenerator(sequenceName, shard).curValue();
    }

    @Override
    public long sequenceCurVal(String sequenceName, Cluster cluster) {
        return sequenceCurVal(sequenceName, cluster.getMainShard());
    }

    @Override
    public long sequenceCurVal(String sequenceName) {
        return sequenceCurVal(sequenceName, getDefaultCluster());
    }

    @Override
    public long sequenceCurVal() {
        return sequenceCurVal(MAIN_SEQUENCE, getDefaultCluster());
    }

    @Override
    public StorageContext getStorageContext(Long id) {
        if (id == null) {
            throw new ShardDataBaseException("Не указан идентификатор сущности");
        }
        if (id.equals(0L)) {
            throw new ShardDataBaseException("Идентификатор сущности не может быть равен 0");
        }
        Cluster cluster = getCluster(ShardUtils.getClusterIdFromEntityId(id));
        DataBaseInstance shard = getShard(cluster, ShardUtils.getShardIdFromEntityId(id));
        return StorageContext.builder()
                .stored(true)
                .lazy(true)
                .cluster(cluster)
                .shard(shard)
                .build();
    }

    @Override
    public Boolean isEnabled(DataBaseInstance shard) {
        return Optional.ofNullable(shard)
                .map(DataBaseInstance::getDynamicDataBaseInfo)
                .map(it ->
                        this.isAvailable(it) &&
                                Optional.ofNullable(shard.getSegment())
                                        .orElse(Optional.ofNullable(it.getSegment()).orElse(StringUtils.EMPTY))
                                        .equals(Optional.ofNullable(this.segment).orElse(StringUtils.EMPTY))
                )
                .orElse(true);
    }

    @Override
    public void saveTransactionInfo() {
        if (!sharedTransactionManager.getTransactionInfoList().isEmpty()) {
            log.trace("Save transaction info...");
            List<TransactionInfo> copyTransactionInfoList;
            synchronized (sharedTransactionManager.getTransactionInfoList()) {
                copyTransactionInfoList = sharedTransactionManager.getTransactionInfoList().stream().toList();
                sharedTransactionManager.getTransactionInfoList().clear();
            }
            SharedEntityTransaction transaction = (SharedEntityTransaction) sharedTransactionManager.getTransaction();
            transaction.begin();
            copyTransactionInfoList.forEach(transactionInfo -> {
                getTransactionalTask(transactionInfo.shard())
                        .getQuery(SAVE_TRANSACTION_QUERY, QueryType.DML)
                        .bind(transactionInfo.uuid())
                        .bind(transactionInfo.executeTime())
                        .bind(transactionInfo.chunks())
                        .bind(transactionInfo.elapsedTime())
                        .bind(transactionInfo.allElapsedTime())
                        .bind(transactionInfo.failed())
                        .bind(transactionInfo.error())
                        .addBatch();
                transactionInfo.queries().forEach(queryInfo -> getTransactionalTask(transactionInfo.shard())
                        .getQuery(SAVE_DML_QUERY, QueryType.DML)
                        .bind(transactionInfo.uuid())
                        .bind(queryInfo.order())
                        .bind(queryInfo.sql())
                        .bind(queryInfo.rows())
                        .bind(queryInfo.elapsedTime())
                        .addBatch());
            });
            transaction.commit(false);
        }
    }

    @Override
    public TransactionalQuery createQuery(DataBaseInstance shard, String query, QueryType queryType) {
        TransactionalQuery transactionalQuery = getTransactionalTask(shard).getQuery(query, queryType);
        if (queryType == QueryType.SELECT) {
            transactionalQuery.setParallelRun(
                    Optional.ofNullable(multiDataBaseConfig.getParallelRun()).orElse(true)
            );
        }
        return transactionalQuery;
    }

    @Override
    public DataBaseInstance getNextShard(Cluster cluster) {
        DataBaseInstance shard = cluster.getShards().get((int) shardSequences.get(cluster.getName()).nextValue());
        Short shardId = shard.getId();
        while (!isEnabled(shard)) {
            shard = cluster.getShards().get((int) shardSequences.get(cluster.getName()).nextValue());
            Assert.isTrue(
                    !shardId.equals(shard.getId()),
                    "Отсутствуют доступные шарды в кластере " + cluster.getName() + "!"
            );
        }
        return shard;
    }

    @Override
    public QueryStream createQueryStreamByIds(String query, List<Long> ids, Object... binds) {
        if (!query.contains("<IDS>")) {
            throw new ShardDataBaseException("В запросе отсутствует обязательный параметр <IDS>!");
        }
        String endQuery = query.substring(query.indexOf("<IDS>") + 5);
        if (endQuery.contains("?") || endQuery.contains("{:")) {
            throw new ShardDataBaseException(
                    "Условия использующие связанные переменные в запросе должны предшествовать условиям с <IDS>");
        }
        return new TransactionalQueryStream(query, ids, binds);
    }

    @Override
    public TransactionalQuery getMainQuery(Iterable<TransactionalQuery> queries) {
        TransactionalQuery mainQuery = null;
        for (TransactionalQuery query : queries) {
            if (Objects.isNull(mainQuery)) {
                mainQuery = query;
                mainQuery.setParallelRun(
                        Optional.ofNullable(multiDataBaseConfig.getParallelRun()).orElse(true)
                );
            } else {
                mainQuery.addRelatedQuery(query);
            }
        }
        return mainQuery;
    }

    @Override
    public Iterable<TransactionalQuery> createQueries(
            Cluster cluster,
            String query,
            QueryType queryType)
    {
        return getEnabledShards(cluster)
                .map(shard -> createQuery(shard, query, queryType))
                .toList();
    }

    @Override
    public TransactionalQuery createQuery(Cluster cluster, String query, QueryType queryType) {
        return getMainQuery(createQueries(cluster, query, queryType));
    }

    @Override
    public QueryQueue getQueryQueue(Cluster cluster, String query, Object... objects) {
        int key = Triple.of(cluster.getId(), query, objects).hashCode();

        QueryQueue queryQueue = queryQueueMap.get(key);
        if (queryQueue == null) {
//            queryQueue = queryQueueMap.computeIfAbsent(key, );
        }



        return null;
    }

    private class TransactionalQueryStream implements QueryStream {
        private Map<Integer, List<List<Long>>> chunkIds;
        private final String query;
        private final Object[] binds;

        TransactionalQueryStream(String query, List<Long> ids, Object... binds) {
            this.query = query;
            this.binds = binds;
            this.chunkIds = groupIds(ids)
                    .entrySet()
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    Map.Entry::getKey,
                                    it -> Lists.partition(it.getValue(), sqlInClauseLimit))
                    );
        }

        @Override
        public TransactionalQuery get() {
            Set<UUID> currentTaskQueries = new HashSet<>();
            SharedEntityTransaction transaction = (SharedEntityTransaction) sharedTransactionManager.getTransaction();
            Map<Integer, List<List<Long>>> newChunkIds = new HashMap<>();
            List<TransactionalQuery> queries = new ArrayList<>();
            for (Map.Entry<Integer, List<List<Long>>> groupIds: chunkIds.entrySet()) {
                TransactionalTask currentTask = null;
                DataBaseInstance shard = shards.get(groupIds.getKey());
                for (int i = 0; i < groupIds.getValue().size(); i++) {
                    List<Long> idLists = groupIds.getValue().get(i);
                    if (currentTask != null) {
                        transaction.addParallel(shard);
                    }
                    TransactionalTask task = getTransactionalTask(shard);
                    currentTask = (currentTask == null) ? task : currentTask;
                    if (currentTaskQueries.contains(task.getTaskUuid())) {
                        newChunkIds.put(groupIds.getKey(), groupIds.getValue().subList(i, groupIds.getValue().size()));
                        break;
                    } else {
                        TransactionalQuery currentQuery =
                                task.
                                        getQuery(
                                                query.replace(
                                                        "<IDS>",
                                                        idLists
                                                                .stream()
                                                                .map(it -> "?")
                                                                .collect(Collectors.joining(","))
                                                ),
                                                QueryType.SELECT,
                                                null
                                        )
                                        .bindAll(binds)
                                        .bindAll(idLists.toArray());
                        currentTaskQueries.add(task.getTaskUuid());
                        queries.add(currentQuery);
                    }
                }
                if (currentTask != null) {
                    transaction.setCurrentTask(shard, currentTask);
                }
            }
            this.chunkIds = newChunkIds;
            return getMainQuery(queries);
        }
    }

    private TransactionalTask createTask(DataBaseInstance shard) {
        try {
            return shard.getRemote() ?
                    remoteTaskFactory.createTask(shard) :
                    taskFactory.createTask(shard, getConnection(shard));
        } catch (Exception err) {
            throw new ShardDataBaseException(err, shard);
        }
    }

    private TransactionalTask createTransactionalTask(DataBaseInstance shard, SharedEntityTransaction transaction) {
        TransactionalTask task = createTask(shard);
        transaction.addTask(shard, task);
        return task;
    }

    private SequenceGenerator getSequenceGenerator(String sequenceName, DataBaseInstance shard) {
        return Optional.ofNullable(sequences.get(sequenceName))
                .map(shardSequences -> shardSequences.get(shard.getHashCode()))
                .orElse(getOrCreateSequenceGenerator(sequenceName, shard));
    }

    private synchronized SequenceGenerator getOrCreateSequenceGenerator(String sequenceName, DataBaseInstance shard) {
        Map<Integer, SequenceGenerator> shardSequences = sequences.get(sequenceName);
        if (Objects.isNull(shardSequences)) {
            shardSequences = new HashMap<>();
            sequences.put(sequenceName, shardSequences);
        }
        SequenceGenerator sequenceGenerator = shardSequences.get(shard.getHashCode());
        if (Objects.isNull(sequenceGenerator)) {
            sequenceGenerator = new ApplicationSequenceGenerator(sequenceName, shard);
            shardSequences.put(shard.getHashCode(), sequenceGenerator);
        }
        return sequenceGenerator;
    }

    private int getActiveConnections(DataBaseInstance shard) {
        return ((HikariDataSource) shard.getDataSource())
                .getHikariPoolMXBean()
                .getActiveConnections();
    }

    private Map<Integer, List<Long>> groupIds(List<Long> ids) {
        Set<Long> uniqueIds = new HashSet<>();
        Map<Integer, List<Long>> partsIds = new HashMap<>();
        ids.forEach(id -> {
            if (!uniqueIds.contains(id)) {
                uniqueIds.add(id);
                DataBaseInstance shard = getShard(
                        getCluster(ShardUtils.getClusterIdFromEntityId(id)),
                        ShardUtils.getShardIdFromEntityId(id)
                );
                List<Long> partIds = partsIds.get(shard.getHashCode());
                if (Objects.isNull(partIds)) {
                    partIds = new ArrayList<>();
                    partsIds.put(shard.getHashCode(), partIds);
                }
                partIds.add(id);
            }
        });
        return partsIds;
    }

    private Stream<DataBaseInstance> getShardsFromValue(ShardInstance entity, Long shardMap, boolean onlyNew) {
        return entity
                .getStorageContext()
                .getCluster()
                .getShards()
                .stream()
                .filter(shard ->
                        !onlyNew && shardMap.equals(0L) ||
                                (ShardUtils.getShardMap(shard.getId()) & shardMap) > 0L
                );
    }

    private void processDataBaseInfo() {
        getDataBaseInfo();
        checkDataBaseInfo();
        saveDataBaseInfo();
    }

    private void checkShardID(Cluster cluster, DataBaseInstance shard, short shardId) {
        if (Objects.isNull(shard.getId())) {
            shard.setId(shardId);
            this.addShardToCluster(cluster, shard);
        } else {
            Assert.isTrue(
                    !Optional
                            .ofNullable(multiDataBaseConfig.getChecks())
                            .map(ChecksConfig::getCheckShardID)
                            .orElse(true) ||
                            shard.getId().equals(shardId),
                    String.format(
                            "Идентификатор шарды в настройках '%s.clusters.shards.id' = '%d' " +
                                    "кластера '%s' " +
                                    "не соответствует идентификатору в БД = '%d'.",
                            MultiDataBaseConfig.CONFIG_NAME, shard.getId(), cluster.getName(), shardId
                    )
            );
        }
    }

    private void checkMainShard(Cluster cluster, DataBaseInstance shard, boolean mainShard) {
        Assert.isTrue(
                !Optional
                        .ofNullable(multiDataBaseConfig.getChecks())
                        .map(ChecksConfig::getCheckMainShard)
                        .orElse(false) ||
                        shard.getId().equals(cluster.getMainShard().getId()) == mainShard,
                String.format(
                        "Шарда с ID = '%d'%s должна быть основной в Кластере '%s'" ,
                        shard.getId(),
                        mainShard ? "" : " не",
                        cluster.getName()
                )
        );
    }

    private void checkClusterID(Cluster cluster, short clusterId, String shardName) {
        if (Objects.isNull(cluster.getId())) {
            cluster.setId(clusterId);
            this.addCluster(cluster.getId(), cluster);
        } else {
            Assert.isTrue(
                    !Optional
                            .ofNullable(multiDataBaseConfig.getChecks())
                            .map(ChecksConfig::getCheckClusterID)
                            .orElse(true) ||
                            cluster.getId().equals(clusterId),
                    String.format(
                            "Идентификатор кластера '%s' в настройках '%s.clusters.id' = '%d' " +
                                    "не соответствует идентификатору в БД (%s) = '%d'.",
                            MultiDataBaseConfig.CONFIG_NAME, cluster.getName(), cluster.getId(), shardName, clusterId
                    )
            );
        }
    }

    private void checkClusterName(Cluster cluster, String clusterName, String shardName) {
        Assert.isTrue(
                !Optional
                        .ofNullable(multiDataBaseConfig.getChecks())
                        .map(ChecksConfig::getCheckClusterName)
                        .orElse(true) ||
                        cluster.getName().equals(clusterName),
                String.format(
                        "Наименование кластера '%s' в настройках '%s.clusters.name' = '%s' " +
                                "не соответствует наименованию в БД (%s) = '%s'.",
                        MultiDataBaseConfig.CONFIG_NAME, cluster.getName(), cluster.getName(), shardName, clusterName
                )
        );
    }

    private void checkClusterDefault(Cluster cluster, boolean clusterDefault, String shardName) {
        Assert.isTrue(
                !Optional
                        .ofNullable(multiDataBaseConfig.getChecks())
                        .map(ChecksConfig::getCheckClusterDefault)
                        .orElse(false) ||
                        cluster.getName().equals(getDefaultCluster().getName()) == clusterDefault,
                String.format(
                        "Кластер '%s'%s должен быть основным для БД %s" ,
                        cluster.getName(),
                        clusterDefault ? "" : " не",
                        shardName
                )
        );
    }

    private short getShardId(Cluster cluster) {
        for (short i = 1; i <= ShardUtils.MAX_SHARDS; i++) {
            if (!cluster.getShardMap().containsKey(i)) {
                return i;
            }
        }
        throw new ShardDataBaseException(
                String.format("Отсутствует свободный идентификатор для шарды в кластере %s", cluster.getName())
        );
    }

    private short getClusterId() {
        for (short i = 1; i <= ShardUtils.MAX_CLUSTERS; i++) {
            if (!clusterIds.containsKey(i)) {
                return i;
            }
        }
        throw new ShardDataBaseException("Отсутствует свободный идентификатор для кластера");
    }

    private void checkDataBaseInfo(Cluster cluster, DataBaseInstance shard) {
        if (Objects.nonNull(shard.getDataBaseInfo())) {
            checkShardID(cluster, shard, shard.getDataBaseInfo().getShardId());
            checkMainShard(cluster, shard, shard.getDataBaseInfo().isMainShard());
            checkClusterID(cluster, shard.getDataBaseInfo().getClusterId(), shard.getName());
            checkClusterName(cluster, shard.getDataBaseInfo().getClusterName(), shard.getName());
            checkClusterDefault(cluster, shard.getDataBaseInfo().isDefaultCluster(), shard.getName());
        } else {
            newShards.add(ImmutablePair.of(cluster, shard));
        }
    }

    private void checkDataBaseInfo(Cluster cluster) {
        for (DataBaseInstance shard : cluster.getShards()) {
            checkDataBaseInfo(cluster, shard);
        }
    }

    private void checkDataBaseInfo() {
        clusters.values()
                .forEach(this::checkDataBaseInfo);
    }

    private void getDynamicDataBaseInfo(DataBaseInstance shard) {
        log.trace("Read dynamic DB info on '{}'...", shard.getName());
        TransactionalTask task = getTransactionalTask(shard);
        DynamicDataBaseInfo dynamicDataBaseInfo = shard.getDynamicDataBaseInfo();
        dynamicDataBaseInfo.setLastTime(System.currentTimeMillis());
        try {
            ResultQuery resultSet = task.getQuery(
                    SELECT_DYNAMIC_DB_INFO,
                    QueryType.SELECT
            ).getResult();
            if (resultSet.next()) {
                dynamicDataBaseInfo.setAvailable(true);
                dynamicDataBaseInfo.setSegment(resultSet.getString(1));
                dynamicDataBaseInfo.setAccessible(resultSet.getBoolean(2));
            }
            task.finish();
            ((SharedEntityTransaction) sharedTransactionManager.getTransaction()).close();
        } catch (Exception err) {
            if (err instanceof SQLTransientConnectionException) {
                dynamicDataBaseInfo.setAvailable(false);
                log.trace("The shard '{}' is not available", shard.getName());
            } else {
                throw new ShardDataBaseException(err, shard);
            }
        }
    }

    private void getDataBaseInfo(Cluster cluster) {
        cluster
                .getShards()
                .stream()
                .filter(it -> !it.getRemote())
                .forEach(shard -> {
                    TransactionalTask task = getTransactionalTask(shard);
                    task.setName("GET DataBase Info on shard " + shard.getName());
                    TransactionalQuery query = task.getQuery(
                            SELECT_DB_INFO,
                            QueryType.SELECT
                    );
                    task.addStep(() -> {
                        try {
                            ResultQuery resultSet = query.getResult();
                            if (resultSet.next()) {
                                shard.setDataBaseInfo(
                                        DataBaseInfo
                                                .builder()
                                                .shardId(resultSet.getShort(1))
                                                .mainShard(resultSet.getBoolean(2))
                                                .clusterId(resultSet.getShort(3))
                                                .clusterName(resultSet.getString(4))
                                                .defaultCluster(resultSet.getBoolean(5))
                                                .build()
                                );
                                DynamicDataBaseInfo dynamicDBInfo = shard.getDynamicDataBaseInfo();
                                dynamicDBInfo.setLastTime(System.currentTimeMillis());
                                dynamicDBInfo.setAvailable(true);
                                dynamicDBInfo.setSegment(resultSet.getString(6));
                                dynamicDBInfo.setAccessible(resultSet.getBoolean(7));
                            }
                        } catch (Exception err) {
                            throw new ShardDataBaseException(err, shard);
                        }
                    });
                });
    }

    private void getDataBaseInfo() {
        sharedTransactionManager.runInTransaction(() ->
                clusters.values()
                        .forEach(this::getDataBaseInfo)
        );
    }


    private void saveDataBaseInfo(Cluster cluster, DataBaseInstance shard) {
        if (Objects.isNull(shard.getId())) {
            shard.setId(getShardId(cluster));
            this.addShardToCluster(cluster, shard);
        }
        if (Objects.isNull(cluster.getId())) {
            cluster.setId(getClusterId());
            this.addCluster(cluster.getId(), cluster);
        }
        shard.setDataBaseInfo(
                DataBaseInfo
                        .builder()
                        .shardId(shard.getId())
                        .mainShard(shard.getId().equals(cluster.getMainShard().getId()))
                        .clusterId(cluster.getId())
                        .clusterName(cluster.getName())
                        .defaultCluster(cluster.getName().equals(getDefaultCluster().getName()))
                        .build()
        );

        DynamicDataBaseInfo dynamicDBInfo = shard.getDynamicDataBaseInfo();
        dynamicDBInfo.setLastTime(System.currentTimeMillis());
        dynamicDBInfo.setAvailable(true);
        dynamicDBInfo.setAccessible(Optional.ofNullable(dynamicDBInfo.getAccessible()).orElse(true));

        TransactionalTask task = getTransactionalTask(shard);
        task.setName("SAVE DataBase Info on shard " + shard.getName());
        task.getQuery(INS_DB_INFO, QueryType.DML)
                .bind(shard.getDataBaseInfo().getShardId())
                .bind(shard.getDataBaseInfo().isMainShard())
                .bind(shard.getDataBaseInfo().getClusterId())
                .bind(shard.getDataBaseInfo().getClusterName())
                .bind(shard.getDataBaseInfo().isDefaultCluster())
                .bind(dynamicDBInfo.getSegment())
                .bind(dynamicDBInfo.getAccessible());
    }

    private void saveDataBaseInfo() {
        SharedEntityTransaction transaction = (SharedEntityTransaction) sharedTransactionManager.getTransaction();
        transaction.begin();
        newShards
                .stream()
                .filter(it -> !it.getRight().getRemote() && Objects.isNull(it.getRight().getDataBaseInfo()))
                .forEach(it -> saveDataBaseInfo(it.getLeft(), it.getRight()));
        transaction.commit(false);
    }

    private <T> Optional<T> getHikariConfigValue(MultiDataBaseConfig multiDataBaseConfig,
                                                 ClusterConfig clusterConfig,
                                                 ShardConfig shardConfig,
                                                 Function<HikariSettings, T> functionGet)
    {
        return Optional.ofNullable(
                Optional.ofNullable(shardConfig.getHikari())
                        .map(functionGet)
                        .orElse(
                                Optional.ofNullable(clusterConfig.getHikari())
                                        .map(functionGet)
                                        .orElse(
                                                Optional.ofNullable(multiDataBaseConfig.getHikari())
                                                        .map(functionGet)
                                                        .orElse(null)
                                        )
                        )
        );
    }

    private <T> void setHikariConfigValue(MultiDataBaseConfig multiDataBaseConfig,
                                          ClusterConfig clusterConfig,
                                          ShardConfig shardConfig,
                                          Function<HikariSettings, T> functionGet,
                                          Consumer<T> functionSet)
    {
        getHikariConfigValue(multiDataBaseConfig, clusterConfig, shardConfig, functionGet).ifPresent(functionSet);
    }

    private static <T> void setDataBaseConfigValue(DataSourceConfig dataSourceConfig,
                                                   Function<DataSourceConfig, T> functionGet,
                                                   Consumer<T> functionSet)
    {
        Optional.ofNullable(dataSourceConfig).map(functionGet).ifPresent(functionSet);
    }

    private void setOptionalDataSourceConfig(HikariConfig config, DataSourceConfig dataSourceConfig) {
        setDataBaseConfigValue(dataSourceConfig, DataSourceConfig::getUrl, config::setJdbcUrl);
        setDataBaseConfigValue(dataSourceConfig, DataSourceConfig::getDriver, config::setDriverClassName);
        setDataBaseConfigValue(dataSourceConfig, DataSourceConfig::getClassName, config::setDataSourceClassName);
        setDataBaseConfigValue(dataSourceConfig, DataSourceConfig::getUsername, config::setUsername);
        setDataBaseConfigValue(dataSourceConfig, DataSourceConfig::getPassword, config::setPassword);
    }

    private void  setOptionalHikariConfig(
            HikariConfig config,
            MultiDataBaseConfig multiDataBaseConfig,
            ClusterConfig clusterConfig,
            ShardConfig shardConfig)
    {
        setHikariConfigValue(multiDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getMinimumIdle, config::setMinimumIdle
        );
        setHikariConfigValue(multiDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getMaximumPoolSize, config::setMaximumPoolSize
        );
        getHikariConfigValue(multiDataBaseConfig, clusterConfig, shardConfig, HikariSettings::getIdleTimeout)
                .map(SECONDS::toMillis)
                .ifPresent(config::setIdleTimeout);
        getHikariConfigValue(multiDataBaseConfig, clusterConfig, shardConfig, HikariSettings::getConnectionTimeout)
                .map(SECONDS::toMillis)
                .ifPresent(config::setConnectionTimeout);
        getHikariConfigValue(multiDataBaseConfig, clusterConfig, shardConfig, HikariSettings::getMaxLifetime)
                .map(SECONDS::toMillis)
                .ifPresent(config::setMaxLifetime);
        getHikariConfigValue(multiDataBaseConfig, clusterConfig, shardConfig, HikariSettings::getKeepAliveTime)
                .map(SECONDS::toMillis)
                .ifPresent(config::setKeepaliveTime);
        setHikariConfigValue(multiDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getPoolName, config::setPoolName
        );
    }

    private HikariConfig getHikariConfig(
            MultiDataBaseConfig multiDataBaseConfig,
            ClusterConfig clusterConfig,
            ShardConfig shardConfig)
    {
        HikariConfig config = new HikariConfig();
        setOptionalDataSourceConfig(config, shardConfig.getDatasource());
        setOptionalHikariConfig(config, multiDataBaseConfig, clusterConfig, shardConfig);
        return config;
    }

    private void processLiquibaseConfig() {
        this.changLogPath = Optional
                .ofNullable(multiDataBaseConfig.getLiquibase())
                .map(LiquibaseConfig::getChangeLogSrc)
                .orElse(DEFAULT_CHANGE_LOG_PATH);
        this.changLogName = Optional
                .ofNullable(multiDataBaseConfig.getLiquibase())
                .map(LiquibaseConfig::getChangeLogName)
                .orElse(DEFAULT_CHANGE_LOG_NAME);
        this.liquibaseEnable = Optional
                .ofNullable(multiDataBaseConfig.getLiquibase())
                .map(LiquibaseConfig::getEnabled)
                .orElse(false);
    }

    private void processThreadPoolConfig() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) this.executorService;
        Optional.ofNullable(multiDataBaseConfig.getThreadPool())
                .map(ThreadPoolConfig::getCorePoolSize)
                .ifPresent(executor::setCorePoolSize);
        Optional.ofNullable(multiDataBaseConfig.getThreadPool())
                .map(ThreadPoolConfig::getMaximumPoolSize)
                .ifPresent(executor::setMaximumPoolSize);
        Optional.ofNullable(multiDataBaseConfig.getThreadPool())
                .map(ThreadPoolConfig::getKeepAliveTime)
                .ifPresent(keepAliveTime -> executor.setKeepAliveTime(keepAliveTime, TimeUnit.SECONDS));
        Optional.ofNullable(multiDataBaseConfig.getThreadPool())
                .map(ThreadPoolConfig::getNameFormat)
                .ifPresent(nameFormat ->
                        executor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat(nameFormat).build()));
    }

    private void processLockConfig() {
        this.lockManager.setDelay(
                Optional.ofNullable(multiDataBaseConfig.getLockProcessor())
                        .map(LockProcessorConfig::getDelay)
                        .orElse(DEFAULT_DELAY_LOCK_PROCESSOR)
        );
        this.lockManager.setTimeOut(
                Optional.ofNullable(multiDataBaseConfig.getLockProcessor())
                        .map(LockProcessorConfig::getTimeOut)
                        .orElse(DEFAULT_TIME_OUT_LOCK_PROCESSOR)
        );
    }

    private void getProperties() {
        this.segment = multiDataBaseConfig.getSegment();
        this.sqlInClauseLimit = Optional
                .ofNullable(multiDataBaseConfig.getSqlInClauseLimit())
                .orElse(SQL_IN_CLAUSE_LIMIT);
        this.timeOutDbProcessor = Optional
                .ofNullable(multiDataBaseConfig.getProcessorTimeOut())
                .orElse(DEFAULT_TIME_OUT_DB_PROCESSOR);
        this.processLiquibaseConfig();
        this.processThreadPoolConfig();
        this.processLockConfig();
        this.sharedTransactionManager.setParallelRun(
                Optional.ofNullable(multiDataBaseConfig.getParallelRun()).orElse(true));
        Assert.notEmpty(
                multiDataBaseConfig.getClusters(),
                String.format("Property '%s.clusters' must not be empty", MultiDataBaseConfig.CONFIG_NAME)
        );
        Assert.isTrue(
                multiDataBaseConfig.getClusters().size() <= ShardUtils.MAX_CLUSTERS,
                "Number of clusters cannot be more than " + ShardUtils.MAX_CLUSTERS
        );
        multiDataBaseConfig.getClusters().forEach(clusterConfig->{
            Cluster cluster = new Cluster();
            cluster.setName(clusterConfig.getName());
            Assert.notNull(
                    cluster.getName(),
                    String.format("Property '%s.clusters.name' must not be empty", MultiDataBaseConfig.CONFIG_NAME)
            );
            if (Objects.isNull(getDefaultCluster()) ||
                    Optional.ofNullable(clusterConfig.getDefaultCluster()).orElse(false))
            {
                defaultCluster = cluster;
            }
            cluster.setId(clusterConfig.getId());
            this.addCluster(cluster.getId(), cluster);

            Assert.notEmpty(
                    clusterConfig.getShards(),
                    String.format("Property '%s.clusters.shards' must not be empty", MultiDataBaseConfig.CONFIG_NAME)
            );
            Assert.isTrue(
                    clusterConfig.getShards().size() <= ShardUtils.MAX_SHARDS,
                    "Number of shards in cluster cannot be more than " + ShardUtils.MAX_SHARDS
            );

            clusterConfig.getShards().forEach(shardConfig-> {
                DataBaseInstance shard = new DataBaseInstance();
                if (Optional.ofNullable(shardConfig.getDatasource()).map(DataSourceConfig::getUrl).isPresent()) {
                    HikariConfig hikariConfig = getHikariConfig(multiDataBaseConfig, clusterConfig, shardConfig);
                    HikariDataSource dataSource = new HikariDataSource(hikariConfig);
                    shard.setDataSource(dataSource);
                    shard.setOwner(
                            Optional.ofNullable(shardConfig.getDatasource())
                                    .map(DataSourceConfig::getOwner)
                                    .orElse(dataSource.getUsername())
                    );
                    Integer percent = Optional.ofNullable(shardConfig.getPercentActiveConnectionParallelLimit())
                            .orElse(
                                    Optional.ofNullable(clusterConfig.getPercentActiveConnectionParallelLimit())
                                            .orElse(
                                                    Optional.ofNullable(
                                                            multiDataBaseConfig
                                                                    .getPercentActiveConnectionParallelLimit()
                                                            )
                                                            .orElse(PERCENT_OF_ACTIVE_CONNECTION_FOR_PARALLEL_LIMIT)
                                            )
                            );
                    shard.setActiveConnectionParallelLimit(
                            Optional.ofNullable(shardConfig.getActiveConnectionParallelLimit())
                                    .orElse(
                                            Optional.ofNullable(clusterConfig.getActiveConnectionParallelLimit())
                                                    .orElse(
                                                            Optional.ofNullable(
                                                                    multiDataBaseConfig
                                                                            .getActiveConnectionParallelLimit()
                                                                    )
                                                                    .orElse(
                                                                            hikariConfig.getMaximumPoolSize() *
                                                                                    percent / 100
                                                                    )
                                                    )
                                    )
                    );
                    if (shard.getActiveConnectionParallelLimit() > hikariConfig.getMaximumPoolSize()) {
                        throw new ShardDataBaseException(
                                String.format(
                                        "Value of activeConnectionParallelLimit (%s) " +
                                                "cannot be more than maximumPoolSize (%s)",
                                        shard.getActiveConnectionParallelLimit(),
                                        hikariConfig.getMaximumPoolSize()
                                )
                        );
                    }
                    if (
                            shard.getActiveConnectionParallelLimit() >
                                    hikariConfig.getMaximumPoolSize() *
                                            PERCENT_OF_ACTIVE_CONNECTION_FOR_PARALLEL_LIMIT / 100) {
                        log.warn("The recommended value for the activeConnectionParallelLimit parameter " +
                                "is no more than {}", hikariConfig.getMaximumPoolSize() *
                                PERCENT_OF_ACTIVE_CONNECTION_FOR_PARALLEL_LIMIT / 100);
                    }
                    shard.setRemote(false);
                } else {
                    shard.setRemote(true);
                    shard.setUrl(
                            Optional.ofNullable(shardConfig.getRemote())
                            .map(RemoteConfig::getUrl)
                            .orElse(null)
                    );
                    Assert.isTrue(
                            Objects.nonNull(shard.getUrl()),
                            String.format(
                                    "Properties '%s.clusters.shards.datasource.url' or '%s.clusters.shards.remote.url'" +
                                    " must not be empty",
                                    MultiDataBaseConfig.CONFIG_NAME,
                                    MultiDataBaseConfig.CONFIG_NAME
                            )
                    );
                    shard.setOwner(
                            Optional.ofNullable(shardConfig.getRemote())
                                    .map(RemoteConfig::getOwner)
                                    .orElse(null)
                    );
                    shard.setWebClient(WebClient.builder().baseUrl(shard.getUrl()).build());
                }
                shard.setSequenceCacheSize(
                        Optional.ofNullable(shardConfig.getSequenceCacheSize())
                                .orElse(
                                        Optional.ofNullable(clusterConfig.getSequenceCacheSize())
                                                .orElse(
                                                        multiDataBaseConfig.getSequenceCacheSize()
                                                )
                                )
                );
                shard.setSegment(shardConfig.getSegment());
                shard.setDynamicDataBaseInfo(
                        DynamicDataBaseInfo
                                .builder()
                                .segment(shardConfig.getSegment())
                                .accessible(
                                        Optional.ofNullable(shardConfig.getAccessible())
                                                .orElse(true)
                                )
                                .available(true)
                                .build()
                );
                shard.setClusterName(cluster.getName());
                shard.setName(
                        String.format(
                                "%s: (%s)",
                                Optional.ofNullable(shardConfig.getId())
                                        .map(it -> cluster.getName() + "-" + it)
                                        .orElse(cluster.getName()),
                                Optional.ofNullable(shardConfig.getDatasource())
                                        .map(DataSourceConfig::getUrl)
                                        .orElse(shard.getUrl())
                        )
                );
                shard.setId(shardConfig.getId());
                this.addShardToCluster(cluster, shard);

                if (Objects.isNull(cluster.getMainShard())
                        || Optional.ofNullable(shardConfig.getMain()).orElse(false))
                {
                    cluster.setMainShard(shard);
                }
                cluster.getShards().add(shard);
            });
            shardSequences.put(cluster.getName(), new SimpleSequenceGenerator(0L, (long) cluster.getShards().size()-1));
            this.addCluster(cluster.getName(), cluster);
        });
    }

    private void addCluster(String name, Cluster cluster) {
        if (clusters.containsKey(name)) {
            throw new ShardDataBaseException(
                    String.format("The cluster with name %s already exists", name)
            );
        } else {
            clusters.put(name, cluster);
        }
    }

    private synchronized void addCluster(Short id, Cluster cluster) {
        if (Objects.isNull(id)) {
            return;
        }
        if (cluster.getId() > ShardUtils.MAX_CLUSTERS) {
            throw new ShardDataBaseException("ID of cluster cannot be more than " + ShardUtils.MAX_CLUSTERS);
        }
        if (clusterIds.containsKey(id)) {
            throw new ShardDataBaseException(
                    String.format("The cluster with ID %d already exists", id)
            );
        } else {
            clusterIds.put(id, cluster);
        }
    }

    private synchronized void addShardToCluster(Cluster cluster, DataBaseInstance shard) {
        if (Objects.isNull(shard.getHashCode())) {
            shard.setHashCode(shard.hashCode());
            if (shards.containsKey(shard.getHashCode())) {
                throw new ShardDataBaseException(
                        String.format("HashCode %s for shard %s is not unique", shard.getHashCode(), shard.getName())
                );
            }
            shards.put(shard.getHashCode(), shard);
        }
        if (Objects.isNull(shard.getId())) {
            return;
        }
        if (shard.getId() < 1) {
            throw new ShardDataBaseException("ID of Shard cannot be less than 1");
        }
        if (shard.getId() > ShardUtils.MAX_SHARDS) {
            throw new ShardDataBaseException("ID of Shard cannot be more than " + ShardUtils.MAX_SHARDS);
        }
        if (cluster.getShardMap().containsKey(shard.getId())) {
            throw new ShardDataBaseException(
                    String.format("The shard with ID %d already exists in cluster %s", shard.getId(), cluster.getName())
            );
        } else {
            cluster.getShardMap().put(shard.getId(), shard);
        }
    }

    private Connection getConnection(DataBaseInstance shard) throws SQLException {
        if (Objects.nonNull(shard)) {
            if (!this.isAvailable(shard.getDynamicDataBaseInfo())) {
                throw new ShardDataBaseException(String.format("The shard \"%s\" is unavailable", shard.getName()));
            }
            return shard.getDataSource().getConnection();
        }
        return null;
    }

    private Boolean isAvailable(DynamicDataBaseInfo dynamicDataBaseInfo) {
        return Optional.ofNullable(dynamicDataBaseInfo)
                .map(it -> it.getAccessible() && it.getAvailable())
                .orElse(true);
    }

    private void runLiquibase(DataBaseInstance shard, String changeLog) {
        if (!shard.getRemote() && isEnabled(shard)) {
            log.debug(String.format("Run changelog \"%s\" on shard %s", changeLog, shard.getName()));
            TransactionalSQLTask task = (TransactionalSQLTask) getTransactionalTask(shard);
            task.setName("Changelog on shard " + shard.getName());
            task.addStep(() -> {
                try {
                    Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                            new JdbcConnection(task.getConnection())
                    );
                    database.setDefaultCatalogName(shard.getOwner());
                    database.setDefaultSchemaName(shard.getOwner());
                    new CommandScope(UpdateCommandStep.COMMAND_NAME)
                            .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                            .addArgumentValue(
                                    UpdateCommandStep.CHANGELOG_FILE_ARG,
                                    changeLog.startsWith(CLASSPATH) ?
                                            changeLog.substring(CLASSPATH.length()) :
                                            changeLog
                            )
                            .execute();
                } catch (LiquibaseException err) {
                    throw new ShardDataBaseException(err, shard);
                }
            }, changeLog);
        }
    }

    private void runLiquibase(Cluster cluster, String changeLog) {
        cluster
                .getShards()
                .forEach(shard -> runLiquibase(shard, changeLog));
    }

    private void runLiquibase(String changeLog) {
        clusters.values()
                .forEach(cluster -> runLiquibase(cluster, changeLog));
    }

    private void runInitLiquibase() {
        if (!this.liquibaseEnable) {
            return;
        }
        sharedTransactionManager.runInTransaction(() -> runLiquibase(INIT_CHANGE_LOG));
    }

    private void runLiquibaseFromPath(String path, DataBaseInstance shard) {
        Optional.of(path + File.separatorChar + this.changLogName)
                .filter(src -> resourceLoader.getResource(src).exists())
                .ifPresent(changeLog -> runLiquibase(shard, changeLog));
    }

    private void runLiquibaseFromPath(String path, Cluster cluster) {
        Optional.of(path + File.separatorChar + this.changLogName)
                .filter(src -> resourceLoader.getResource(src).exists())
                .ifPresent(changeLog -> runLiquibase(cluster, changeLog));
    }

    private void runLiquibaseFromPath(String path) {
        Optional.of(path + File.separatorChar + this.changLogName)
                .filter(src -> resourceLoader.getResource(src).exists())
                .ifPresent(this::runLiquibase);
    }

    private void runScheduleDatabaseProcessor() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            log.trace("schedule Database Processor...");
            clusters
                    .values()
                    .forEach(
                            cluster ->
                                    cluster.getShards()
                                            .forEach(shard ->
                                                    this.executorService.submit(() ->
                                                            this.getDynamicDataBaseInfo(shard)))
                    );
            saveTransactionInfo();
        }, this.timeOutDbProcessor, this.timeOutDbProcessor, TimeUnit.SECONDS);
    }

    private void runLiquibase() {
        if (!this.liquibaseEnable) {
            return;
        }
        sharedTransactionManager.runInTransaction(() ->
                Optional.ofNullable(this.changLogPath)
                        .filter(src -> resourceLoader.getResource(src).exists())
                        .ifPresent(path -> {
                            Optional.of(path + File.separatorChar + CLUSTERS_PATH)
                                    .filter(src -> resourceLoader.getResource(src).exists())
                                    .ifPresent(clustersPath -> {
                                        runLiquibaseFromPath(clustersPath);
                                        clusters.forEach((clusterName, cluster) ->
                                                Optional.of(clustersPath + File.separatorChar + clusterName)
                                                        .filter(src -> resourceLoader.getResource(src).exists())
                                                        .ifPresent(clusterPath -> {
                                                            runLiquibaseFromPath(clusterPath, cluster);
                                                            Optional.of(
                                                                    clusterPath +
                                                                            File.separatorChar +
                                                                            SHARDS_PATH
                                                                    )
                                                                    .filter(src ->
                                                                            resourceLoader.getResource(src).exists())
                                                                    .ifPresent(shardsPath -> {
                                                                        runLiquibaseFromPath(shardsPath, cluster);
                                                                        cluster
                                                                                .getShards()
                                                                                .forEach(shard ->
                                                                                        runLiquibaseFromPath(
                                                                                                shardsPath +
                                                                                                        File.separatorChar +
                                                                                                        shard.getId()
                                                                                                , shard
                                                                                        )
                                                                                );
                                                                    });
                                                        })
                                        );
                                    });
                            runLiquibaseFromPath(path, getDefaultCluster().getMainShard());
                        })
        );
    }
}
