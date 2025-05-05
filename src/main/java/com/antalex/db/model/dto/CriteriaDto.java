package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CriteriaDto {
    private String classPackage;
    private String targetClassName;
    private EntityClassDto from;
    private String alias;
    private String where;
    private List<CriteriaJoinDto> joins;
}
