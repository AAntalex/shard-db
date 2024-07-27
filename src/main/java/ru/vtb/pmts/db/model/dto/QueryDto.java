package ru.vtb.pmts.db.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.vtb.pmts.db.model.enums.QueryType;

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
    private List<List<String>> binds;
    private List<String> currentBinds;
    private List<String> types;
}
