package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import javax.lang.model.element.Element;

@Data
@Builder
public class DomainFieldDto {
    private String fieldName;
    private Element element;
    private int fieldIndex;
    private String getter;
    private String setter;
    private EntityFieldDto entityField;
    private StorageDto storage;
}
