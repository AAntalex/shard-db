package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import javax.persistence.criteria.JoinType;

@Data
@Builder
public class CriteriaJoinDto {
    private EntityClassDto from;
    private String alias;
    private String on;
    private JoinType joinType;
    private Long columns;
}
