package com.antalex.db.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.model.dto.response.ResponseClusterInfoDto;
import com.antalex.db.model.dto.response.ResponseInstanceInfoDto;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.api.DatabaseInfoService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DatabaseInfoServiceImpl implements DatabaseInfoService {
    private final ShardDataBaseManager dataBaseManager;

    @Override
    public ResponseInstanceInfoDto getDatabaseInfo(String cluster, Short shardId) {
        return map(dataBaseManager.getShard(dataBaseManager.getCluster(cluster), shardId));
    }

    @Override
    public ResponseClusterInfoDto getDatabaseInfo(String cluster) {
        return map(dataBaseManager.getCluster(cluster));
    }

    @Override
    public Map<String, ResponseClusterInfoDto> getDatabaseInfo() {
        return dataBaseManager
                .getClusters()
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                it -> map(it.getValue()))
                );
    }

    private ResponseInstanceInfoDto map(DataBaseInstance shard) {
        return new ResponseInstanceInfoDto()
                .shardId(shard.getDataBaseInfo().getShardId())
                .mainShard(shard.getDataBaseInfo().isMainShard())
                .accessible(shard.getDynamicDataBaseInfo().getAccessible())
                .available(shard.getDynamicDataBaseInfo().getAvailable())
                .segment(shard.getDynamicDataBaseInfo().getSegment())
                .lastTime(shard.getDynamicDataBaseInfo().getLastTime())
                .unavailableReason(shard.getDynamicDataBaseInfo().getUnavailableReason())
                .activeConnections(
                        ((HikariDataSource) shard.getDataSource())
                                .getHikariPoolMXBean()
                                .getActiveConnections()
                )
                .idleConnections(
                        ((HikariDataSource) shard.getDataSource())
                                .getHikariPoolMXBean()
                                .getIdleConnections()
                );
    }

    private ResponseClusterInfoDto map(Cluster cluster) {
        return new ResponseClusterInfoDto()
                .clusterId(cluster.getId())
                .defaultCluster(cluster == dataBaseManager.getDefaultCluster())
                .instances(
                        cluster.getShards()
                                .stream()
                                .map(this::map)
                                .toList()
                );
    }
}
