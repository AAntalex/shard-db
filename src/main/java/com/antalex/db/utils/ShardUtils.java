package com.antalex.db.utils;

import com.antalex.db.model.DataBaseInstance;

import java.util.Optional;

public class ShardUtils {
    public static final int MAX_REPLICATIONS = 10;
    public static final int MAX_SHARDS = 63;
    public static final int MAX_CLUSTERS = 100;
    private static final String DEFAULT_OWNER_PREFIX = "$$$";

    public static String transformSQL(String sql, DataBaseInstance shard) {
        return Optional.ofNullable(shard.getOwner())
                .map(owner -> sql.replace(DEFAULT_OWNER_PREFIX, owner))
                .orElse(sql);
    }

    public static Long getShardMap(Short id) {
        return 1L << (id - 1);
    }

    public static Long addShardMap(Long shardMap, Long addShardMap) {
        return shardMap | addShardMap;
    }

    public static Short getShardIdFromEntityId(Long id) {
        return (short) (id % ShardUtils.MAX_SHARDS + 1);
    }

    public static Short getClusterIdFromEntityId(Long id) {
        return (short) (id / ShardUtils.MAX_SHARDS % ShardUtils.MAX_CLUSTERS + 1);
    }
}
