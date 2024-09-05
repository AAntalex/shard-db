package com.antalex.db.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import com.antalex.db.model.enums.QueryType;

import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class QueryDto {
    private String clusterName;
    private Short shardId;
    private UUID taskUuid;
    private UUID clientUuid;
    private Boolean postponedCommit;
    private String query;
    private QueryType queryType;
    private List<List<String>> batchBinds;
    private List<String> binds;
    private List<String> types;
}
