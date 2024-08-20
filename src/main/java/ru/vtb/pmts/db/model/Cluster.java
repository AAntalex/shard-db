package ru.vtb.pmts.db.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Cluster {
    private Short id;
    private String name;
    private DataBaseInstance mainShard;
    private List<DataBaseInstance> shards = new ArrayList<>();
    private Map<Short, DataBaseInstance> shardMap = new HashMap<>();
}
