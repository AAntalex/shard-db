package com.antalex.db.model.dto;

import com.antalex.db.model.enums.ShardType;
import lombok.Builder;
import lombok.Data;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class EntityClassDto {
    private String classPackage;
    private String targetClassName;
    private String tableName;
    private String cluster;
    private ShardType shardType;
    private Element classElement;
    private List<EntityFieldDto> fields;
    private Map<String, EntityFieldDto> fieldMap;
    private List<IndexDto> indexes;
    private List<EntityFieldDto> uniqueFields;
    private List<EntityFieldDto> columnFields;
    private Boolean chainAccessors;
}
