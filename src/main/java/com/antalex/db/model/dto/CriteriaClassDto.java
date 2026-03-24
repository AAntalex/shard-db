package com.antalex.db.model.dto;

import com.antalex.db.model.PredicateGroup;
import com.antalex.db.model.criteria.CriteriaPredicate;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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
    private List<CriteriaPredicate> predicates;
}
