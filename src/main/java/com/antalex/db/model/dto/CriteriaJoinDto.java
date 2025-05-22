package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.criteria.JoinType;

@Data
@Builder
public class CriteriaJoinDto {
    private EntityClassDto from;
    private String alias;
    private String joinAlias;
    private String on;
    private JoinType joinType;
    private Long columns;
    private Boolean linkedShard;
    private Pair<String, String> joinColumns;
}
