package ru.vtb.pmts.db.service;

import ru.vtb.pmts.db.entity.abstraction.ShardInstance;
import ru.vtb.pmts.db.model.Cluster;
import ru.vtb.pmts.db.model.Shard;
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
    Shard getShard(Cluster cluster, Short id);
    Stream<Shard> getEnabledShards(Cluster cluster);
    Stream<Shard> getEntityShards(ShardInstance entity);
    Stream<Shard> getNewShards(ShardInstance entity);
    void generateId(ShardInstance entity);
    Connection getConnection(Short clusterId, Short shardId) throws SQLException;
    StorageContext getStorageContext(Long id);
    long sequenceNextVal(String sequenceName, Shard shard);
    long sequenceNextVal(String sequenceName, Cluster cluster);
    long sequenceNextVal(String sequenceName);
    long sequenceNextVal();
    TransactionalTask getTransactionalTask(Shard shard);
    Boolean isEnabled(Shard shard);
    void saveTransactionInfo();
}
