package com.antalex.db.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Cluster {
    private Short id;
    private String name;
    private Shard mainShard;
    private List<Shard> shards = new ArrayList<>();
    private Map<Short, Shard> shardMap = new HashMap<>();
}
