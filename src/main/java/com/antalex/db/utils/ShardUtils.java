package com.antalex.db.utils;

import com.antalex.db.model.Shard;

public class ShardUtils {
    public static final int MAX_SHARDS = 63;
    public static final int MAX_CLUSTERS = 100;
    public static final String DEFAULT_CLUSTER_NAME = "DEFAULT";
    private static final String DEFAULT_OWNER_PREFIX = "$$$";

    public static String transformSQL(String sql, Shard shard) {
        return sql.replace(DEFAULT_OWNER_PREFIX, shard.getOwner());
    }

    public static Long getShardValue(Short id) {
        return 1L << (id - 1);
    }

    public static Long addShardValue(Long shardValue, Long addShardValue) {
        return shardValue | addShardValue;
    }
}
