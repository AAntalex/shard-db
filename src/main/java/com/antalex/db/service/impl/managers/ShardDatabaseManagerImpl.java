package com.antalex.db.service.impl.managers;

import com.antalex.db.config.*;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.*;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.SharedTransactionManager;
import com.antalex.db.service.api.*;
import com.antalex.db.service.impl.*;
import com.antalex.db.utils.ShardUtils;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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
    private static final int DEFAULT_TIME_OUT_REFRESH_DB_INFO = 10;

    private static final String SELECT_DB_INFO = "SELECT SHARD_ID,MAIN_SHARD,CLUSTER_ID,CLUSTER_NAME,DEFAULT_CLUSTER" +
            ",SEGMENT_NAME,ACCESSIBLE FROM $$$.APP_DATABASE";
    private static final String INS_DB_INFO = "INSERT INTO $$$.APP_DATABASE " +
            "(SHARD_ID,MAIN_SHARD,CLUSTER_ID,CLUSTER_NAME,DEFAULT_CLUSTER,SEGMENT_NAME,ACCESSIBLE) " +
            " VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_DYNAMIC_DB_INFO = "SELECT SEGMENT_NAME,ACCESSIBLE FROM $$$.APP_DATABASE";

    private final ResourceLoader resourceLoader;
    private final ShardDataBaseConfig shardDataBaseConfig;
    private final SharedTransactionManager sharedTransactionManager;
    private final TransactionalSQLTaskFactory taskFactory;
    private final TransactionalExternalTaskFactory externalTaskFactory;

    private Cluster defaultCluster;
    private final Map<String, Cluster> clusters = new HashMap<>();
    private final Map<Short, Cluster> clusterIds = new HashMap<>();
    private final Map<String, SequenceGenerator> shardSequences = new HashMap<>();
    private final Map<String, Map<Integer, SequenceGenerator>> sequences = new HashMap<>();
    private final List<ImmutablePair<Cluster, Shard>> newShards = new ArrayList<>();

    private String changLogPath;
    private String changLogName;
    private Boolean liquibaseEnable;
    private String segment;
    private int parallelLimit;
    private int timeOut;
    private final ExecutorService executorService;

    ShardDatabaseManagerImpl(
            ResourceLoader resourceLoader,
            ShardDataBaseConfig shardDataBaseConfig,
            SharedTransactionManager sharedTransactionManager,
            TransactionalSQLTaskFactory taskFactory,
            TransactionalExternalTaskFactory externalTaskFactory)
    {
        this.resourceLoader = resourceLoader;
        this.shardDataBaseConfig = shardDataBaseConfig;
        this.sharedTransactionManager = sharedTransactionManager;
        this.sharedTransactionManager.setParallelRun(true);
        this.taskFactory = taskFactory;
        this.externalTaskFactory = externalTaskFactory;
        this.executorService = Executors.newCachedThreadPool();
        this.taskFactory.setExecutorService(this.executorService);
        this.externalTaskFactory.setExecutorService(this.executorService);

        getProperties();
        runInitLiquibase();
        processDataBaseInfo();
        runLiquibase();
    }

    @Override
    public TransactionalTask getTransactionalTask(Shard shard) {
        SharedEntityTransaction transaction = (SharedEntityTransaction) sharedTransactionManager.getTransaction();
        return Optional.ofNullable(
                transaction.getCurrentTask(
                        shard,
                        !shard.getExternal() &&
                                ((HikariDataSource) shard.getDataSource())
                                        .getHikariPoolMXBean()
                                        .getActiveConnections() > parallelLimit
                )
        )
                .orElseGet(() -> {
                    try {
                        TransactionalTask task = shard.getExternal() ?
                                externalTaskFactory.createTask(shard) :
                                taskFactory.createTask(shard, getConnection(shard));
                        transaction.addTask(shard, task);
                        return task;
                    } catch (Exception err) {
                        throw new ShardDataBaseException(err);
                    }
                });
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
            throw new ShardDataBaseException("Отсутсвует кластер с идентификатором " + id);
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
    public Shard getShard(Cluster cluster, Short id) {
        if (cluster == null) {
            throw new ShardDataBaseException("Не указан кластер");
        }
        if (id == null) {
            throw new ShardDataBaseException("Не указан идентификатор шарды");
        }
        Shard shard = cluster.getShardMap().get(id);
        if (shard == null) {
            throw new ShardDataBaseException(
                    String.format("Отсутсвует шарда с идентификатором '%d' в кластере '%s'", id, cluster.getName())
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
        entity.setId((
                        sequenceNextVal(MAIN_SEQUENCE, storageContext.getCluster()) *
                                ShardUtils.MAX_CLUSTERS + storageContext.getCluster().getId() - 1
                ) * ShardUtils.MAX_SHARDS + storageContext.getShard().getId() - 1
        );
    }

    @Override
    public Stream<Shard> getEnabledShards(Cluster cluster) {
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
    public Stream<Shard> getEntityShards(ShardInstance entity) {
        return getShardsFromValue(
                entity,
                entity.isStored() ?
                        entity.getStorageContext().getOriginalShardMap() :
                        entity.getStorageContext().getShardMap(),
                false
        );
    }

    @Override
    public Stream<Shard> getNewShards(ShardInstance entity) {
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
    public long sequenceNextVal(String sequenceName, Shard shard) {
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
        return sequenceGenerator.nextValue();
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
    public Connection getConnection(Short clusterId, Short shardId) throws SQLException {
        return getConnection(
                Optional.ofNullable(clusterId)
                        .map(clusterIds::get)
                        .map(Cluster::getShards)
                        .map(it -> it.get(shardId))
                        .orElse(null)
        );
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
        Shard shard = getShard(cluster, ShardUtils.getShardIdFromEntityId(id));
        return StorageContext.builder()
                .stored(true)
                .isLazy(true)
                .cluster(cluster)
                .shard(shard)
                .build();
    }

    @Override
    public Boolean isEnabled(Shard shard) {
        return Optional.ofNullable(shard)
                .map(Shard::getDynamicDataBaseInfo)
                .map(it ->
                        this.isAvailable(it) &&
                                Optional.ofNullable(shard.getSegment())
                                        .orElse(Optional.ofNullable(it.getSegment()).orElse(StringUtils.EMPTY))
                                        .equals(Optional.ofNullable(this.segment).orElse(StringUtils.EMPTY))
                )
                .orElse(true);
    }

    private Stream<Shard> getShardsFromValue(ShardInstance entity, Long shardMap, boolean onlyNew) {
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

    private Shard getNextShard(Cluster cluster) {
        Shard shard = cluster.getShards().get((int) shardSequences.get(cluster.getName()).nextValue());
        Short shardId = shard.getId();
        while (!isEnabled(shard)) {
            shard = cluster.getShards().get((int) shardSequences.get(cluster.getName()).nextValue());
            Assert.isTrue(!shardId.equals(shard.getId()), "Отсутсвуют доступные шарды!");
        }
        return shard;
    }

    private void processDataBaseInfo() {
        getDataBaseInfo();
        checkDataBaseInfo();
        saveDataBaseInfo();
    }

    private void checkShardID(Cluster cluster, Shard shard, short shardId) {
        if (Objects.isNull(shard.getId())) {
            shard.setId(shardId);
            this.addShardToCluster(cluster, shard);
        } else {
            Assert.isTrue(
                    !Optional
                            .ofNullable(shardDataBaseConfig.getChecks())
                            .map(ChecksConfig::getCheckShardID)
                            .orElse(true) ||
                            shard.getId().equals(shardId),
                    String.format(
                            "Идентификатор шарды в настройках '%s.clusters.shards.id' = '%d' " +
                                    "кластера '%s' " +
                                    "не соответсвует идентификатору в БД = '%d'.",
                            ShardDataBaseConfig.CONFIG_NAME, shard.getId(), cluster.getName(), shardId
                    )
            );
        }
    }

    private void checkMainShard(Cluster cluster, Shard shard, boolean mainShard) {
        Assert.isTrue(
                !Optional
                        .ofNullable(shardDataBaseConfig.getChecks())
                        .map(ChecksConfig::getCheckMainShard)
                        .orElse(true) ||
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
                            .ofNullable(shardDataBaseConfig.getChecks())
                            .map(ChecksConfig::getCheckClusterID)
                            .orElse(true) ||
                            cluster.getId().equals(clusterId),
                    String.format(
                            "Идентификатор кластера '%s' в настройках '%s.clusters.id' = '%d' " +
                                    "не соответсвует идентификатору в БД (%s) = '%d'.",
                            ShardDataBaseConfig.CONFIG_NAME, cluster.getName(), cluster.getId(), shardName, clusterId
                    )
            );
        }
    }

    private void checkClusterName(Cluster cluster, String clusterName, String shardName) {
        Assert.isTrue(
                !Optional
                        .ofNullable(shardDataBaseConfig.getChecks())
                        .map(ChecksConfig::getCheckClusterName)
                        .orElse(true) ||
                        cluster.getName().equals(clusterName),
                String.format(
                        "Наименование кластера '%s' в настройках '%s.clusters.name' = '%s' " +
                                "не соответсвует наименованию в БД (%s) = '%s'.",
                        ShardDataBaseConfig.CONFIG_NAME, cluster.getName(), cluster.getName(), shardName, clusterName
                )
        );
    }

    private void checkClusterDefault(Cluster cluster, boolean clusterDefault, String shardName) {
        Assert.isTrue(
                !Optional
                        .ofNullable(shardDataBaseConfig.getChecks())
                        .map(ChecksConfig::getCheckClusterDefault)
                        .orElse(true) ||
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
                String.format("Отсутсвует свободный идентификатор для шарды в кластере %s", cluster.getName())
        );
    }

    private short getClusterId() {
        for (short i = 1; i <= ShardUtils.MAX_CLUSTERS; i++) {
            if (!clusterIds.containsKey(i)) {
                return i;
            }
        }
        throw new ShardDataBaseException("Отсутсвует свободный идентификатор для кластера");
    }

    private void checkDataBaseInfo(Cluster cluster, Shard shard) {
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
        for (Shard shard : cluster.getShards()) {
            checkDataBaseInfo(cluster, shard);
        }
    }

    private void checkDataBaseInfo() {
        clusters.values()
                .forEach(this::checkDataBaseInfo);
    }

    private void getDynamicDataBaseInfo(Shard shard) {
        if (shard.getDynamicDataBaseInfo().getLastTime() != null &&
                System.currentTimeMillis() - shard.getDynamicDataBaseInfo().getLastTime() > this.timeOut)
        {
            log.trace("Read dynamic DB info...'");
            DynamicDataBaseInfo dynamicDataBaseInfo = shard.getDynamicDataBaseInfo();
            dynamicDataBaseInfo.setLastTime(System.currentTimeMillis());
            try {
                Connection connection = shard.getDataSource().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        ShardUtils.transformSQL(SELECT_DYNAMIC_DB_INFO, shard)
                );
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    dynamicDataBaseInfo.setAvailable(true);
                    dynamicDataBaseInfo.setSegment(resultSet.getString(1));
                    dynamicDataBaseInfo.setAccessible(resultSet.getBoolean(2));
                }
                connection.close();
            } catch (SQLException err) {
                if (err instanceof SQLTransientConnectionException) {
                    dynamicDataBaseInfo.setAvailable(false);
                }
                throw new ShardDataBaseException(err);
            }
        }
    }

    private void getDataBaseInfo(Cluster cluster) {
        cluster
                .getShards()
                .stream()
                .filter(it -> !it.getExternal())
                .forEach(shard -> {
                    TransactionalSQLTask task = (TransactionalSQLTask) getTransactionalTask(shard);
                    task.setName("GET DataBase Info on shard " + shard.getName());
                    TransactionalSQLQuery query = (TransactionalSQLQuery) task.addQuery(
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
                            throw new ShardDataBaseException(err);
                        }
                    });
                });
    }

    private void getDataBaseInfo() {
        sharedTransactionManager.getTransaction().begin();
        clusters.values()
                .forEach(this::getDataBaseInfo);
        sharedTransactionManager.getTransaction().commit();
    }


    private void saveDataBaseInfo(Cluster cluster, Shard shard) {
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

        TransactionalSQLTask task = (TransactionalSQLTask) getTransactionalTask(shard);
        task.setName("SAVE DataBase Info on shard " + shard.getName());
        task.addQuery(INS_DB_INFO, QueryType.DML)
                .bind(shard.getDataBaseInfo().getShardId())
                .bind(shard.getDataBaseInfo().isMainShard())
                .bind(shard.getDataBaseInfo().getClusterId())
                .bind(shard.getDataBaseInfo().getClusterName())
                .bind(shard.getDataBaseInfo().isDefaultCluster())
                .bind(dynamicDBInfo.getSegment())
                .bind(dynamicDBInfo.getAccessible());
    }

    private void saveDataBaseInfo() {
        sharedTransactionManager.getTransaction().begin();
        newShards
                .stream()
                .filter(it -> !it.getRight().getExternal() && Objects.isNull(it.getRight().getDataBaseInfo()))
                .forEach(it -> saveDataBaseInfo(it.getLeft(), it.getRight()));
        sharedTransactionManager.getTransaction().commit();
    }

    private <T> Optional<T> getTransactionConfigValue(ShardDataBaseConfig shardDataBaseConfig,
                                          ClusterConfig clusterConfig,
                                          ShardConfig shardConfig,
                                          Function<SharedTransactionConfig, T> functionGet)
    {
        return Optional.ofNullable(
                Optional.ofNullable(shardConfig.getTransactionConfig())
                        .map(functionGet)
                        .orElse(
                                Optional.ofNullable(clusterConfig.getTransactionConfig())
                                        .map(functionGet)
                                        .orElse(
                                                Optional.ofNullable(shardDataBaseConfig.getTransactionConfig())
                                                        .map(functionGet)
                                                        .orElse(null)
                                        )
                        )
        );
    }

    private <T> void setHikariConfigValue(ShardDataBaseConfig shardDataBaseConfig,
                                          ClusterConfig clusterConfig,
                                          ShardConfig shardConfig,
                                          Function<HikariSettings, T> functionGet,
                                          Consumer<T> functionSet)
    {
        Optional.ofNullable(
                Optional.ofNullable(shardConfig.getHikari())
                        .map(functionGet)
                        .orElse(
                                Optional.ofNullable(clusterConfig.getHikari())
                                        .map(functionGet)
                                        .orElse(
                                                Optional.ofNullable(shardDataBaseConfig.getHikari())
                                                        .map(functionGet)
                                                        .orElse(null)
                                        )
                        )
        ).ifPresent(functionSet);
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
        setDataBaseConfigValue(dataSourceConfig, DataSourceConfig::getUser, config::setUsername);
        setDataBaseConfigValue(dataSourceConfig, DataSourceConfig::getPass, config::setPassword);
    }

    private void setOptionalHikariConfig(
            HikariConfig config,
            ShardDataBaseConfig shardDataBaseConfig,
            ClusterConfig clusterConfig,
            ShardConfig shardConfig)
    {
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getMinimumIdle, config::setMinimumIdle
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getMaximumPoolSize, config::setMaximumPoolSize
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getIdleTimeout, config::setIdleTimeout
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getConnectionTimeout, config::setConnectionTimeout
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getMaxLifetime, config::setMaxLifetime
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getPoolName, config::setPoolName
        );
    }

    private void setOptionalHikariConfig(ShardDataBaseConfig shardDataBaseConfig,
                                         ClusterConfig clusterConfig,
                                         ShardConfig shardConfig) {
        this.parallelLimit = getTransactionConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                SharedTransactionConfig::getActiveConnectionParallelLimit)
                .orElse(0);
        this.taskFactory.setParallelCommit(
                getTransactionConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                        SharedTransactionConfig::getParallelCommit)
                        .orElse(true)
        );
    }

    private HikariConfig getHikariConfig(
            ShardDataBaseConfig shardDataBaseConfig,
            ClusterConfig clusterConfig,
            ShardConfig shardConfig)
    {
        HikariConfig config = new HikariConfig();
        setOptionalDataSourceConfig(config, shardConfig.getDataSource());
        setOptionalHikariConfig(config, shardDataBaseConfig, clusterConfig, shardConfig);
        return config;
    }

    private void processLiquibaseConfig() {
        this.changLogPath = Optional
                .ofNullable(shardDataBaseConfig.getLiquibase())
                .map(LiquibaseConfig::getChangeLogSrc)
                .orElse(DEFAULT_CHANGE_LOG_PATH);
        this.changLogName = Optional
                .ofNullable(shardDataBaseConfig.getLiquibase())
                .map(LiquibaseConfig::getChangeLogName)
                .orElse(DEFAULT_CHANGE_LOG_NAME);
        this.liquibaseEnable = Optional
                .ofNullable(shardDataBaseConfig.getLiquibase())
                .map(LiquibaseConfig::getEnabled)
                .orElse(false);
    }

    private void processThreadPoolConfig() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) this.executorService;
        Optional.ofNullable(shardDataBaseConfig.getThreadPool())
                .map(ThreadPoolConfig::getCorePoolSize)
                .ifPresent(executor::setCorePoolSize);
        Optional.ofNullable(shardDataBaseConfig.getThreadPool())
                .map(ThreadPoolConfig::getMaximumPoolSize)
                .ifPresent(executor::setMaximumPoolSize);
        Optional.ofNullable(shardDataBaseConfig.getThreadPool())
                .map(ThreadPoolConfig::getKeepAliveTime)
                .ifPresent(keepAliveTime -> executor.setKeepAliveTime(keepAliveTime, TimeUnit.SECONDS));
    }

    private void getProperties() {
        this.segment = shardDataBaseConfig.getSegment();
        this.timeOut = Optional
                .ofNullable(shardDataBaseConfig.getTimeOut())
                .orElse(DEFAULT_TIME_OUT_REFRESH_DB_INFO) * 1000;
        this.processLiquibaseConfig();
        this.processThreadPoolConfig();
        Assert.notEmpty(
                shardDataBaseConfig.getClusters(),
                String.format("Property '%s.clusters' must not be empty", ShardDataBaseConfig.CONFIG_NAME)
        );
        Assert.isTrue(
                shardDataBaseConfig.getClusters().size() <= ShardUtils.MAX_CLUSTERS,
                "Number of clusters cannot be more than " + ShardUtils.MAX_CLUSTERS
        );
        shardDataBaseConfig.getClusters().forEach(clusterConfig->{
            Cluster cluster = new Cluster();
            cluster.setName(clusterConfig.getName());
            Assert.notNull(
                    cluster.getName(),
                    String.format("Property '%s.clusters.name' must not be empty", ShardDataBaseConfig.CONFIG_NAME)
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
                    String.format("Property '%s.clusters.shards' must not be empty", ShardDataBaseConfig.CONFIG_NAME)
            );
            Assert.isTrue(
                    clusterConfig.getShards().size() <= ShardUtils.MAX_SHARDS,
                    "Number of shards in cluster cannot be more than " + ShardUtils.MAX_SHARDS
            );

            clusterConfig.getShards().forEach(shardConfig-> {
                Shard shard = new Shard();
                setOptionalHikariConfig(shardDataBaseConfig, clusterConfig, shardConfig);
                if (Optional.ofNullable(shardConfig.getDataSource()).map(DataSourceConfig::getUrl).isPresent()) {
                    HikariDataSource dataSource = new HikariDataSource(
                            getHikariConfig(shardDataBaseConfig, clusterConfig, shardConfig)
                    );
                    shard.setDataSource(dataSource);
                    shard.setOwner(
                            Optional.ofNullable(shardConfig.getDataSource())
                                    .map(DataSourceConfig::getOwner)
                                    .orElse(dataSource.getUsername())
                    );
                    shard.setExternal(false);
                } else {
                    shard.setExternal(true);
                    shard.setUrl(shardConfig.getUrl());
                    Assert.isTrue(
                            Objects.nonNull(shard.getUrl()),
                            String.format(
                                    "Properties '%s.clusters.shards.datasource.url' or '%s.clusters.shards.url'" +
                                    " must not be empty",
                                    ShardDataBaseConfig.CONFIG_NAME,
                                    ShardDataBaseConfig.CONFIG_NAME
                            )
                    );
                }
                shard.setSequenceCacheSize(
                        Optional.ofNullable(shardConfig.getSequenceCacheSize())
                                .orElse(
                                        Optional.ofNullable(clusterConfig.getSequenceCacheSize())
                                                .orElse(
                                                        shardDataBaseConfig.getSequenceCacheSize()
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
                shard.setName(
                        String.format(
                                "%s: (%s)",
                                cluster.getName(),
                                Optional.ofNullable(shardConfig.getDataSource())
                                        .map(DataSourceConfig::getUrl)
                                        .orElse(shardConfig.getUrl())
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

    private synchronized void addShardToCluster(Cluster cluster, Shard shard) {
        if (Objects.isNull(shard.getHashCode())) {
            shard.setHashCode(shard.hashCode());
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

    private Connection getConnection(Shard shard) throws SQLException {
        if (Objects.nonNull(shard)) {
            getDynamicDataBaseInfo(shard);
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

    private void runLiquibase(Shard shard, String changeLog) {
        if (!shard.getExternal() && isEnabled(shard)) {
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
                    throw new ShardDataBaseException(err);
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
        sharedTransactionManager.getTransaction().begin();
        runLiquibase(INIT_CHANGE_LOG);
        sharedTransactionManager.getTransaction().commit();
    }

    private void runLiquibaseFromPath(String path, Shard shard) {
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

    private void runLiquibase() {
        if (!this.liquibaseEnable) {
            return;
        }
        sharedTransactionManager.getTransaction().begin();
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
                                                Optional.of(clusterPath + File.separatorChar + SHARDS_PATH)
                                                        .filter(src -> resourceLoader.getResource(src).exists())
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
                });
        sharedTransactionManager.getTransaction().commit();
    }
}
