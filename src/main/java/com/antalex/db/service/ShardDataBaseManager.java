package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.service.api.TransactionalTask;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;

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
    TransactionalTask getTransactionalTask(DataBaseInstance shard);
    Boolean isEnabled(DataBaseInstance shard);
    void saveTransactionInfo();
}
