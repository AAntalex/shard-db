package ru.vtb.pmts.db.service;

import ru.vtb.pmts.db.entity.abstraction.ShardInstance;
import ru.vtb.pmts.db.model.Cluster;
import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.model.StorageContext;
import ru.vtb.pmts.db.service.api.TransactionalTask;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

public interface ShardDataBaseManager {
    Connection getConnection() throws SQLException;
    DataSource getDataSource();
    Cluster getCluster(Short id);
    Cluster getCluster(String clusterName);
    Cluster getDefaultCluster();
    DataBaseInstance getShard(Cluster cluster, Short id);
    Stream<DataBaseInstance> getEnabledShards(Cluster cluster);
    Stream<DataBaseInstance> getEntityShards(ShardInstance entity);
    Stream<DataBaseInstance> getNewShards(ShardInstance entity);
    void generateId(ShardInstance entity);
    Connection getConnection(Short clusterId, Short shardId) throws SQLException;
    StorageContext getStorageContext(Long id);
    long sequenceNextVal(String sequenceName, DataBaseInstance shard);
    long sequenceNextVal(String sequenceName, Cluster cluster);
    long sequenceNextVal(String sequenceName);
    long sequenceNextVal();
    long sequenceCurVal(String sequenceName, DataBaseInstance shard);
    long sequenceCurVal(String sequenceName, Cluster cluster);
    long sequenceCurVal(String sequenceName);
    long sequenceCurVal();
    TransactionalTask getTransactionalTask(DataBaseInstance shard);
    Boolean isEnabled(DataBaseInstance shard);
    void saveTransactionInfo();
}
