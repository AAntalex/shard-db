package com.antalex.db.model.dto;

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
    private List<CriteriaJoinDto> joins;
}
