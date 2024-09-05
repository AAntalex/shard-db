package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DomainClassDto {
    private String classPackage;
    private String targetClassName;
    private EntityClassDto entityClass;
    private List<DomainFieldDto> fields;
    private StorageDto storage;
    private String cluster;
    private Element classElement;
    private Map<String, StorageDto> storageMap;
    private Boolean chainAccessors;
}
