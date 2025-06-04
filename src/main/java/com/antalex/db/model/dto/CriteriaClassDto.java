package com.antalex.db.model.dto;

import com.antalex.db.model.PredicateGroup;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CriteriaClassDto {
    private String classPackage;
    private String targetClassName;
    private EntityClassDto from;
    private List<CriteriaFieldDto> fields;
    private String alias;
    private String where;
    private Long columns;
    private List<CriteriaJoinDto> joins;
    private List<PredicateGroup> predicateGroups;
}
