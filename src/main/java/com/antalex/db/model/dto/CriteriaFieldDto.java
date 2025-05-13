package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import javax.lang.model.element.Element;

@Data
@Builder
public class CriteriaFieldDto {
    private String fieldName;
    private Element element;
    private String setter;
    private int columnIndex;
    private DomainClassDto domainField;
    private String columnName;
}
